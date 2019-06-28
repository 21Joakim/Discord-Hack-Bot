package com.jsb.bot.logger;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
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
import com.jsb.bot.utility.MiscUtility;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookMessage;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import okhttp3.OkHttpClient;

class LoggerClient {
	
	private static final int MAX_RETRIES = 3;
	
	public static class Request {
		
		public final JDA jda;
		public final long guildId;
		
		public final LoggerType type;
		public final List<WebhookEmbed> embeds;
		
		public final Instant created;
		
		public Request(JDA jda, long guildId, LoggerType type, List<WebhookEmbed> embeds, Instant created) {
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
	
	public boolean isLoggerCapable(Document logger, LoggerType type) {
		return this.isLoggerCapable(logger, type, null);
	}
	
	public boolean isLoggerCapable(Document logger, LoggerType type, Predicate<Document> predicate) {
		if(!logger.getBoolean("enabled", true)) {
			return false;
		}
		
		List<Document> events = logger.getList("events", Document.class, Collections.emptyList());
		boolean mode = logger.getBoolean("mode");
		
		Document foundEvent = null;
		if(mode) {
			if(events.size() == 0) {
				return false;
			}else{
				for(Document event : events) {
					if(event.getString("type").equalsIgnoreCase(type.toString())) {
						foundEvent = event;
					}
				}
			}
		}else{
			foundEvent = new Document("type", type.toString());
			
			for(Document event : events) {
				if(event.getString("type").equalsIgnoreCase(type.toString())) {
					return false;
				}
			}
		}
		
		if(foundEvent != null && (predicate == null || predicate.test(foundEvent))) {
			return true;
		}
		
		return false;
	}
	
	public Document getLogger(Guild guild, LoggerType type) {
		return this.getLogger(guild, type, null);
	}

	public Document getLogger(Guild guild, LoggerType type, Predicate<Document> predicate) {
		Document document = Database.get().getGuildById(guild.getIdLong(), null, Projections.include("logger.loggers"));
		List<Document> loggers = document.getEmbedded(List.of("logger", "loggers"), Collections.emptyList());
		for(Document logger : loggers) {
			if(this.isLoggerCapable(logger, type, predicate)) {
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
				
				/* Remove logger if the channel no longer exists */
				if(request.getGuild().getTextChannelById(logger.getLong("channelId")) == null) {
					deque.addFirst(request);
					
					Database.get().updateGuildById(request.guildId, Updates.pull("logger.loggers", new Document("channelId", logger.getLong("channelId"))), (update, exception) -> {
						if(exception != null) {
							exception.printStackTrace();
						}
						
						this.handleQueue(deque, retries + 1);
					});
					
					return;
				}
				
				List<WebhookEmbed> embeds = new ArrayList<>(request.embeds);
				List<Request> uncapable = new ArrayList<>();
				
				int totalLength = MiscUtility.getWebhookEmbedLength(embeds);
				
				while(totalLength < MessageEmbed.EMBED_MAX_LENGTH_BOT && embeds.size() < 10) {
					Request anotherRequest = deque.poll();
					if(anotherRequest == null) {
						break;
					}
					
					if(embeds.size() + anotherRequest.embeds.size() > 10) {
						uncapable.add(request);
						
						break;
					}
					
					int length = MiscUtility.getWebhookEmbedLength(anotherRequest.embeds);
					
					if(totalLength + length <= MessageEmbed.EMBED_MAX_LENGTH_BOT) {
						if(this.isLoggerCapable(logger, anotherRequest.type)) {
							embeds.addAll(anotherRequest.embeds);
							totalLength += length;
						}else{
							uncapable.add(request);
						}
					}else{
						uncapable.add(request);
						
						break;
					}
				}
				
				if(uncapable.size() > 0) {
					for(Request wrongTypeRequest : uncapable) {
						deque.addFirst(wrongTypeRequest);
					}
				}
				
				WebhookClient client = this.webhooks.computeIfAbsent(logger.getLong("webhookId"), id -> {
					return new WebhookClientBuilder(id, logger.getString("webhookToken"))
						.setHttpClient(this.webhookClient)
						.setExecutorService(this.webhookScheduler)
						.build();
				});
				
				WebhookMessage message = new WebhookMessageBuilder()
					.addEmbeds(embeds)
					.setAvatarUrl("https://cdn.discordapp.com/attachments/423589273120276484/594225220483547147/Logger.png")
					.build();
				
				client.send(message)
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

	public void queue(Guild guild, LoggerType type, WebhookEmbed... embeds) {
		this.queue(new Request(guild.getJDA(), guild.getIdLong(), type, Arrays.asList(embeds), Clock.systemUTC().instant()));
	}
}