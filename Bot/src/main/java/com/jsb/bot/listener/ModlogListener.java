package com.jsb.bot.listener;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.bson.Document;

import com.jsb.bot.database.Database;
import com.jsb.bot.modlog.Action;
import com.mongodb.client.model.Projections;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ModlogListener extends ListenerAdapter {

	public static void createModlog(Guild guild, User moderator, User user, String reason, boolean automatic, Action action) {
		Database.get().getGuildById(guild.getIdLong(), null, Projections.include("modlog.enabled", "modlog.channel", "modlog.disabledActions"), (data, readException) -> {
			if (readException != null) {
				readException.printStackTrace();
			} else {
				Document modlogData = data.getEmbedded(List.of("modlog"), new Document());
				boolean enabled = modlogData.getBoolean("enabled", false);
				if (enabled) {
					List<String> disabledActions = modlogData.getList("disabledActions", String.class);
					if (!disabledActions.contains(action.toString())) {
						long caseId = Database.get().getModlogCasesAmountFromGuild(guild.getIdLong()) + 1;
						TextChannel channel = guild.getTextChannelById(modlogData.getLong("channel"));
						if (channel != null) {
							EmbedBuilder embed = new EmbedBuilder();
							embed.setTitle("Case " + caseId + " | " + action.getName());
							embed.addField("Moderator", moderator == null ? "Unknown" : moderator.getAsTag() + " (" + moderator.getId() + ")", false);
							embed.addField("User", user.getAsTag() + " (" + user.getId() + ")", false);
							embed.addField("Reason", reason == null ? "None Given" : reason, false);
							embed.setTimestamp(Instant.now());
							
							if (guild.getSelfMember().hasPermission(channel, Permission.MESSAGE_EMBED_LINKS) && guild.getSelfMember().hasPermission(channel, Permission.MESSAGE_WRITE)) {
								channel.sendMessage(embed.build()).queue(message -> {
									Document newCase = new Document()
											.append("guildId", guild.getIdLong())
											.append("messageId", message.getIdLong())
											.append("channelId", channel.getIdLong())
											.append("moderatorId", moderator.getIdLong())
											.append("userId", user.getIdLong())
											.append("createdAt", Clock.systemUTC().instant().getEpochSecond())
											.append("id", caseId)
											.append("reason", reason)
											.append("action", action.toString())
											.append("automatic", automatic);
									
									Database.get().insertModlogCase(newCase);
								});
								
								return;
							}
						}
						
						Document newCase = new Document()
								.append("guildId", guild.getIdLong())
								.append("messageId", null)
								.append("channelId", null)
								.append("moderatorId", moderator.getIdLong())
								.append("userId", user.getIdLong())
								.append("createdAt", Clock.systemUTC().instant().getEpochSecond())
								.append("id", caseId)
								.append("reason", reason)
								.append("action", action.toString())
								.append("automatic", automatic);
						
						Database.get().insertModlogCase(newCase);
					}
				}
			}
		});
	}
	
	public void onGuildBan(GuildBanEvent event) {
		if (event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
			event.getGuild().retrieveAuditLogs().type(ActionType.BAN).queueAfter(500, TimeUnit.MILLISECONDS, auditLogs -> {
				User moderator = null;
				String reason = null;
				for (AuditLogEntry auditLog : auditLogs) {
					if (auditLog.getTargetIdLong() == event.getUser().getIdLong()) {
						moderator = auditLog.getUser();
						reason = auditLog.getReason();
						break;
					}
				} 
				
				if (moderator != null && !moderator.equals(event.getJDA().getSelfUser())) {
					ModlogListener.createModlog(event.getGuild(), moderator, event.getUser(), reason, false, Action.BAN);
				}
			});
		}
	}
	
	public void onGuildUnban(GuildUnbanEvent event) {
		if (event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
			event.getGuild().retrieveAuditLogs().type(ActionType.UNBAN).queueAfter(500, TimeUnit.MILLISECONDS, auditLogs -> {
				User moderator = null;
				String reason = null;
				for (AuditLogEntry auditLog : auditLogs) {
					if (auditLog.getTargetIdLong() == event.getUser().getIdLong()) {
						moderator = auditLog.getUser();
						reason = auditLog.getReason();
						break;
					}
				} 
				
				System.out.println(moderator.getName() + " - " + reason);
				
				if (moderator != null && !moderator.equals(event.getJDA().getSelfUser())) {
					ModlogListener.createModlog(event.getGuild(), moderator, event.getUser(), reason, false, Action.UNBAN);
				}
			});
		}
	}
	
	public void onGuildMemberLeave(GuildMemberLeaveEvent event) {
		if (event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
			event.getGuild().retrieveAuditLogs().type(ActionType.KICK).queueAfter(500, TimeUnit.MILLISECONDS, auditLogs -> {
				User moderator = null;
				String reason = null;
				for (AuditLogEntry auditLog : auditLogs) {
					if (auditLog.getTargetIdLong() == event.getUser().getIdLong() && Duration.between(auditLog.getTimeCreated(), ZonedDateTime.now(ZoneId.of("UTC"))).getSeconds() <= 5) {
						moderator = auditLog.getUser();
						reason = auditLog.getReason();
						break;
					}
				} 
				
				if (moderator != null && !moderator.equals(event.getJDA().getSelfUser())) {
					ModlogListener.createModlog(event.getGuild(), moderator, event.getUser(), reason, false, Action.KICK);
				}
			});
		}
	}
	
}
