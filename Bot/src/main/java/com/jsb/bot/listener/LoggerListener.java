package com.jsb.bot.listener;

import java.awt.Color;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.bson.Document;

import com.jsb.bot.database.Database;
import com.mongodb.client.model.Projections;

import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbed.EmbedAuthor;
import club.minnced.discord.webhook.send.WebhookEmbed.EmbedFooter;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.events.channel.text.TextChannelCreateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class LoggerListener extends ListenerAdapter {
	
	private static final int MAX_ATTEMPTS = 3;
	
	private static final int LOG_DELAY = 500;
	
	public static class Request {
		
		public final JDA jda;
		public final long guildId;
		public final Document logger;
		public final List<WebhookEmbed> embeds;
		
		public Request(JDA jda, long guildId, Document logger, List<WebhookEmbed> embeds) {
			this.jda = jda;
			this.guildId = guildId;
			this.logger = logger;
			this.embeds = embeds;
		}
		
		public Guild getGuild() {
			return this.jda.getGuildById(guildId);
		}
	}
	
	private Document getLogger(Guild guild, String type) {
		return this.getLogger(guild, type, null);
	}
	
	private Document getLogger(Guild guild, String type, Predicate<Document> predicate) {
		Document document = Database.get().getGuildById(guild.getIdLong(), null, Projections.include("logger.enabled", "logger.loggers"));
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
	
	/* 
	 * Used to delay all logs so we can get the correct action as well as the correct audit logs,
	 * the reason all logs are delayed and not just the ones that use audit logs is because 
	 * to keep the order of the logs, this can be done in other ways as well but this is the 
	 * easiest. 
	 */
	private ScheduledExecutorService delayer = Executors.newSingleThreadScheduledExecutor();
	
	private void queue(Request request) {
		
	}
	
	private void queue(JDA jda, Guild guild, Document logger, WebhookEmbed... embeds) {
		this.queue(new Request(jda, guild.getIdLong(), logger, Arrays.asList(embeds)));
	}
	
	public void onChannelCreate(Guild guild, GuildChannel channel, String type) {
		Document logger = this.getLogger(guild, type);
		if(logger != null) {
			this.delayer.schedule(() -> {
				WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
				embed.setDescription(String.format("A channel by the name of %s has been created", channel.getName()));
				embed.setColor(Color.GREEN.getRGB());
				embed.setTimestamp(ZonedDateTime.now());
				embed.setAuthor(new EmbedAuthor(guild.getName(), null, guild.getIconUrl()));
				embed.setFooter(new EmbedFooter(String.format("Channel ID: %s", channel.getIdLong()), null));
				
				if(guild.getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
					guild.retrieveAuditLogs().type(ActionType.CHANNEL_CREATE).queue(logs -> {
						AuditLogEntry entry = logs.stream()
							.filter(e -> e.getTargetIdLong() == channel.getIdLong())
							.findFirst().orElse(null);
						
						if(entry != null) {
							embed.setDescription(String.format("A channel by the name of %s has been created by **%s**", channel.getName(), entry.getUser().getAsTag()));
						}
						
						this.queue(guild.getJDA(), guild, logger, embed.build());
					});
				}else{
					this.queue(guild.getJDA(), guild, logger, embed.build());
				}
			}, LoggerListener.LOG_DELAY, TimeUnit.MILLISECONDS);
		}
	}
	
	public void onTextChannelCreate(TextChannelCreateEvent event) {
		onChannelCreate(event.getGuild(), event.getChannel(), "TEXT_CHANNEL_CREATE");
	}
}