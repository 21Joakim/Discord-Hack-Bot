package com.jsb.bot.modlog;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.bson.Document;

import com.jsb.bot.database.Database;
import com.jsb.bot.utility.CheckUtility;
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
	
	private static Document createCase(long guildId, Long messageId, Long channelId, Long moderatorId, long userId, long caseId, String reason, Action action, boolean automatic) {
		return new Document()
			.append("guildId", guildId)
			.append("messageId", messageId)
			.append("channelId", channelId)
			.append("moderatorId", moderatorId)
			.append("userId", userId)
			.append("createdAt", Clock.systemUTC().instant().getEpochSecond())
			.append("id", caseId)
			.append("reason", reason)
			.append("action", action.toString())
			.append("automatic", automatic);
	}

	public static void createModlog(Guild guild, User moderator, User user, String reason, boolean automatic, Action action) {
		Database.get().getGuildById(guild.getIdLong(), null, Projections.include("modlog.enabled", "modlog.channel", "modlog.disabledActions"), (data, readException) -> {
			if(CheckUtility.isExceptional(readException)) {
				return;
			}
			
			Document modlogData = data.getEmbedded(List.of("modlog"), new Document());
			
			boolean enabled = modlogData.getBoolean("enabled", false);
			if(!enabled) {
				return;
			}
			
			List<String> disabledActions = modlogData.getList("disabledActions", String.class, Collections.emptyList());
			if(!disabledActions.contains(action.toString())) {
				/* TODO: There is probably a safer and better way to do this */
				long caseId = Database.get().getModlogCasesAmountFromGuild(guild.getIdLong()) + 1;
				
				TextChannel channel = guild.getTextChannelById(modlogData.getLong("channel"));
				if(channel != null) {
					EmbedBuilder embed = new EmbedBuilder();
					embed.setTitle("Case " + caseId + " | " + action.getName());
					embed.addField("Moderator", moderator == null ? "Unknown" : moderator.getAsTag() + " (" + moderator.getId() + ")", false);
					embed.addField("User", user.getAsTag() + " (" + user.getId() + ")", false);
					embed.addField("Reason", reason == null ? "None Given" : reason, false);
					embed.setTimestamp(Instant.now());
					
					if(guild.getSelfMember().hasPermission(channel, Permission.MESSAGE_EMBED_LINKS) && guild.getSelfMember().hasPermission(channel, Permission.MESSAGE_WRITE)) {
						channel.sendMessage(embed.build()).queue(message -> {
							Document newCase = ModlogListener.createCase(guild.getIdLong(), message.getIdLong(), channel.getIdLong(), 
								moderator.getIdLong(), user.getIdLong(), caseId, reason, action, automatic);
							
							Database.get().insertModlogCase(newCase, ($, exception) -> {
								if(exception != null) {
									exception.printStackTrace();
								}
							});
						});
						
						return;
					}
				}
				
				Document newCase = ModlogListener.createCase(guild.getIdLong(), null, null, 
					moderator.getIdLong(), user.getIdLong(), caseId, reason, action, automatic);
				
				Database.get().insertModlogCase(newCase, ($, exception) -> {
					if(CheckUtility.isExceptional(exception)) {
						return;
					}
				});
			}
		});
	}
	
	public void onGuildBan(GuildBanEvent event) {
		if(!event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
			return;
		}
		
		event.getGuild().retrieveAuditLogs().type(ActionType.BAN).queueAfter(500, TimeUnit.MILLISECONDS, auditLogs -> {
			AuditLogEntry log = null;
			for(AuditLogEntry auditLog : auditLogs) {
				if(auditLog.getTargetIdLong() == event.getUser().getIdLong()) {
					log = auditLog;
					
					break;
				}
			}
			
			if(log != null) {
				User moderator = log.getUser();
				if(moderator != null && log.getUser().equals(event.getJDA().getSelfUser())) {
					return;
				}
				
				ModlogListener.createModlog(event.getGuild(), moderator, event.getUser(), log.getReason(), false, Action.BAN);
			}
		});
	}
	
	public void onGuildUnban(GuildUnbanEvent event) {
		if(!event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
			return;
		}
		
		event.getGuild().retrieveAuditLogs().type(ActionType.UNBAN).queueAfter(500, TimeUnit.MILLISECONDS, auditLogs -> {
			AuditLogEntry log = null;
			for(AuditLogEntry auditLog : auditLogs) {
				if(auditLog.getTargetIdLong() == event.getUser().getIdLong()) {
					log = auditLog;
					
					break;
				}
			}
			
			if(log != null) {
				User moderator = log.getUser();
				if(moderator != null && log.getUser().equals(event.getJDA().getSelfUser())) {
					return;
				}
				
				ModlogListener.createModlog(event.getGuild(), log.getUser(), event.getUser(), log.getReason(), false, Action.UNBAN);
			}
		});
	}
	
	public void onGuildMemberLeave(GuildMemberLeaveEvent event) {
		if(!event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
			return;
		}
		
		event.getGuild().retrieveAuditLogs().type(ActionType.KICK).queueAfter(500, TimeUnit.MILLISECONDS, auditLogs -> {
			AuditLogEntry log = null;
			for(AuditLogEntry auditLog : auditLogs) {
				if(auditLog.getTargetIdLong() == event.getUser().getIdLong() && Duration.between(auditLog.getTimeCreated(), ZonedDateTime.now(ZoneId.of("UTC"))).getSeconds() <= 5) {
					log = auditLog;
					
					break;
				}
			} 
			
			if(log != null) {
				User moderator = log.getUser();
				if(moderator != null && log.getUser().equals(event.getJDA().getSelfUser())) {
					return;
				}
				
				ModlogListener.createModlog(event.getGuild(), moderator, event.getUser(), log.getReason(), false, Action.KICK);
			}
		});
	}
}