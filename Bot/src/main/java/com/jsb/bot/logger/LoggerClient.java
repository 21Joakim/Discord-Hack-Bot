package com.jsb.bot.logger;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Predicate;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.jsb.bot.database.Database;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookEmbed;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import okhttp3.OkHttpClient;

class LoggerClient {
	
	private static final int MAX_RETRIES = 3;

	private static final int TIME_BEFORE_REFETCH = 1000;
	
	public static class Request {
		
		public final JDA jda;
		public final long guildId;
		
		public final String type;
		public final List<WebhookEmbed> embeds;
		
		public final Instant created;
		
		public Request(JDA jda, long guildId, String type, List<WebhookEmbed> embeds, Instant created) {
			this.jda = jda;
			this.guildId = guildId;
			this.type = type;
			this.embeds = embeds;
			this.created = created;
		}
		
		public Guild getGuild() {
			return this.jda.getGuildById(guildId);
		}
	}
	
	public static LoggerClient INSTANCE = new LoggerClient();
	
	public static LoggerClient get() {
		return LoggerClient.INSTANCE;
	}
	
	private LoggerClient() {}
	
	private ExecutorService executor = Executors.newCachedThreadPool();

	private Map<Long, WebhookClient> webhooks = new HashMap<>();
	private Map<Long, BlockingDeque<Request>> queue = new HashMap<>();

	private Set<BlockingDeque<Request>> handlingQueue = new HashSet<>();

	private OkHttpClient webhookClient = new OkHttpClient();
	private ScheduledExecutorService webhookScheduler = Executors.newSingleThreadScheduledExecutor();
	
	public Document getLogger(Guild guild, String type) {
		return this.getLogger(guild, type, null);
	}

	public Document getLogger(Guild guild, String type, Predicate<Document> predicate) {
		Document document = Database.get().getGuildById(guild.getIdLong(), null, Projections.include("logger.loggers"));
		List<Document> loggers = document.getEmbedded(List.of("logger", "loggers"), Collections.emptyList());
		for(Document logger : loggers) {
			if(!logger.getBoolean("enabled", true)) {
				continue;
			}
			
			List<Document> enabledEvents = logger.getList("enabledEvents", Document.class, Collections.emptyList());
			List<Document> disabledEvents = logger.getList("disabledEvents", Document.class, Collections.emptyList());
			
			boolean enabled = enabledEvents.size() == 0;
			Document enabledEvent = Database.EMPTY_DOCUMENT;
			
			for(Document event : enabledEvents) {
				if(event.getString("type").equals(type)) {
					enabled = false;
					enabledEvent = event;
				}
			}
			
			for(Document event : disabledEvents) {
				if(event.getString("type").equals(type)) {
					enabled = false;
				}
			}
			
			if(enabled && (predicate == null || predicate.test(enabledEvent))) {
				return logger;
			}
		}
		
		return null;
	}

	private void handleQueue(BlockingDeque<Request> deque, int retries) {
		this.executor.submit(() -> {
			Request request = deque.poll();
			if(request != null) {
				if(retries == LoggerClient.MAX_RETRIES) {
					this.handleQueue(deque, 0);
					
					return;
				}
				
				Document logger = this.getLogger(request.getGuild(), request.type);
				if(logger == null) {
					this.handleQueue(deque, 0);
					
					return;
				}
				
				WebhookClient client = this.webhooks.computeIfAbsent(logger.getLong("webhookId"), id -> {
					return new WebhookClientBuilder(id, logger.getString("webhookToken"))
						.setHttpClient(this.webhookClient)
						.setExecutorService(this.webhookScheduler)
						.build();
				});
				
				client.send(request.embeds)
					.thenRun(() -> {
						this.handleQueue(deque, 0);
					})
					.exceptionally(exception -> {
						if(exception instanceof CompletionException) {
							exception = exception.getCause();
						}
						
						deque.addFirst(request);
						
						/* Not very pretty but it's the best I have */
						if(exception.getMessage().startsWith("Request returned failure 404")) {
							TextChannel channel = request.getGuild().getTextChannelById(logger.getLong("channelId"));
							
							if(request.getGuild().getSelfMember().hasPermission(Permission.MANAGE_WEBHOOKS)) {
								channel.createWebhook("Logger").queue(webhook -> {
									Bson update = Updates.combine(
										Updates.set("logger.loggers.$[logger].webhookId", webhook.getIdLong()),
										Updates.set("logger.loggers.$[logger].webhookToken", webhook.getToken())
									);
									
									UpdateOptions options = new UpdateOptions().arrayFilters(List.of(Filters.eq("logger.channelId", logger.getLong("channelId"))));
									
									Database.get().updateGuildById(request.guildId, update, options, (result, exception2) -> {
										if(exception2 != null) {
											exception2.printStackTrace();
										}
										
										this.handleQueue(deque, retries + 1);
									});
								});
							}
						}else{
							this.handleQueue(deque, retries + 1);
						}
						
						return null;
					});
			}else{
				this.handlingQueue.remove(deque);
			}
		});
	}

	public void queue(Request request) {
		if(!this.queue.containsKey(request.guildId)) {
			this.queue.put(request.guildId, new LinkedBlockingDeque<>());
		}
		
		BlockingDeque<Request> deque = this.queue.get(request.guildId);
		deque.add(request);
		
		if(this.handlingQueue.add(deque)) {
			this.handleQueue(deque, 0);
		}
	}

	public void queue(Guild guild, String type, WebhookEmbed... embeds) {
		this.queue(new Request(guild.getJDA(), guild.getIdLong(), type, Arrays.asList(embeds), Clock.systemUTC().instant()));
	}
}