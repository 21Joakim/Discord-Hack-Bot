package com.jsb.bot.logger;

import java.awt.Color;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.bson.Document;

import club.minnced.discord.webhook.send.WebhookEmbed.EmbedAuthor;
import club.minnced.discord.webhook.send.WebhookEmbed.EmbedField;
import club.minnced.discord.webhook.send.WebhookEmbed.EmbedFooter;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.events.channel.text.TextChannelCreateEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class LoggerListener extends ListenerAdapter {
	
	private static final int LOG_DELAY = 500;
	
	/* 
	 * Used to delay all logs so we can get the correct action as well as the correct audit logs,
	 * the reason all logs are delayed and not just the ones that use audit logs is
	 * to keep the order of the logs, this can be done in other ways as well but this is the 
	 * easiest. 
	 */
	private ScheduledExecutorService delayer = Executors.newSingleThreadScheduledExecutor();
	
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		Document logger = LoggerClient.get().getLogger(event.getGuild(), "GUILD_MESSAGE_SENT");
		if(logger != null) {
			this.delayer.schedule(() -> {
				WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
				embed.setDescription(String.format("A message was sent by **%s**", event.getAuthor().getAsTag()));
				embed.setColor(Color.GREEN.getRGB());
				embed.setTimestamp(ZonedDateTime.now());
				embed.setAuthor(new EmbedAuthor(event.getMember().getEffectiveName(), event.getAuthor().getEffectiveAvatarUrl(), null));
				embed.setFooter(new EmbedFooter(String.format("User ID: %s", event.getAuthor().getId()), null));
				embed.addField(new EmbedField(true, "Content", event.getMessage().getContentRaw()));
				
				LoggerClient.get().queue(event.getGuild(), "GUILD_MESSAGE_SENT", embed.build());
			}, LoggerListener.LOG_DELAY, TimeUnit.MILLISECONDS);
		}
	}	
	
	public void onChannelCreate(Guild guild, GuildChannel channel, String type) {
		Document logger = LoggerClient.get().getLogger(guild, type);
		if(logger != null) {
			this.delayer.schedule(() -> {
				WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
				embed.setDescription(String.format("A channel by the name of %s has been created", channel.getName()));
				embed.setColor(Color.GREEN.getRGB());
				embed.setTimestamp(ZonedDateTime.now());
				embed.setAuthor(new EmbedAuthor(guild.getName(), guild.getIconUrl(), null));
				embed.setFooter(new EmbedFooter(String.format("Channel ID: %s", channel.getId()), null));
				
				if(guild.getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
					guild.retrieveAuditLogs().type(ActionType.CHANNEL_CREATE).queue(logs -> {
						AuditLogEntry entry = logs.stream()
							.filter(e -> e.getTargetIdLong() == channel.getIdLong())
							.findFirst().orElse(null);
						
						if(entry != null) {
							embed.setDescription(String.format("A channel by the name of %s has been created by **%s**", channel.getName(), entry.getUser().getAsTag()));
						}
						
						LoggerClient.get().queue(guild, type, embed.build());
					});
				}else{
					LoggerClient.get().queue(guild, type, embed.build());
				}
			}, LoggerListener.LOG_DELAY, TimeUnit.MILLISECONDS);
		}
	}
	
	public void onTextChannelCreate(TextChannelCreateEvent event) {
		onChannelCreate(event.getGuild(), event.getChannel(), "TEXT_CHANNEL_CREATE");
	}
}