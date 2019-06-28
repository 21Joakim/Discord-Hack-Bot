package com.jsb.bot.logger;

import java.awt.Color;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.bson.Document;

import com.jsb.bot.embed.WebhookEmbedBuilder;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.audit.AuditLogKey;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Guild.MFALevel;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.events.channel.category.CategoryCreateEvent;
import net.dv8tion.jda.api.events.channel.category.CategoryDeleteEvent;
import net.dv8tion.jda.api.events.channel.category.update.CategoryUpdateNameEvent;
import net.dv8tion.jda.api.events.channel.store.StoreChannelCreateEvent;
import net.dv8tion.jda.api.events.channel.store.StoreChannelDeleteEvent;
import net.dv8tion.jda.api.events.channel.store.update.StoreChannelUpdateNameEvent;
import net.dv8tion.jda.api.events.channel.text.TextChannelCreateEvent;
import net.dv8tion.jda.api.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.api.events.channel.text.update.TextChannelUpdateNSFWEvent;
import net.dv8tion.jda.api.events.channel.text.update.TextChannelUpdateNameEvent;
import net.dv8tion.jda.api.events.channel.text.update.TextChannelUpdateParentEvent;
import net.dv8tion.jda.api.events.channel.text.update.TextChannelUpdateSlowmodeEvent;
import net.dv8tion.jda.api.events.channel.text.update.TextChannelUpdateTopicEvent;
import net.dv8tion.jda.api.events.channel.voice.VoiceChannelCreateEvent;
import net.dv8tion.jda.api.events.channel.voice.VoiceChannelDeleteEvent;
import net.dv8tion.jda.api.events.channel.voice.update.VoiceChannelUpdateBitrateEvent;
import net.dv8tion.jda.api.events.channel.voice.update.VoiceChannelUpdateNameEvent;
import net.dv8tion.jda.api.events.channel.voice.update.VoiceChannelUpdateParentEvent;
import net.dv8tion.jda.api.events.channel.voice.update.VoiceChannelUpdateUserLimitEvent;
import net.dv8tion.jda.api.events.emote.EmoteAddedEvent;
import net.dv8tion.jda.api.events.emote.EmoteRemovedEvent;
import net.dv8tion.jda.api.events.emote.update.EmoteUpdateNameEvent;
import net.dv8tion.jda.api.events.emote.update.EmoteUpdateRolesEvent;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateAfkChannelEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateAfkTimeoutEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateExplicitContentLevelEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateIconEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateMFALevelEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateNameEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateNotificationLevelEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateOwnerEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateRegionEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateSplashEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateSystemChannelEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateVerificationLevelEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceGuildDeafenEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceGuildMuteEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.role.RoleCreateEvent;
import net.dv8tion.jda.api.events.role.RoleDeleteEvent;
import net.dv8tion.jda.api.events.role.update.RoleUpdateColorEvent;
import net.dv8tion.jda.api.events.role.update.RoleUpdateHoistedEvent;
import net.dv8tion.jda.api.events.role.update.RoleUpdateMentionableEvent;
import net.dv8tion.jda.api.events.role.update.RoleUpdateNameEvent;
import net.dv8tion.jda.api.events.role.update.RoleUpdatePermissionsEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class LoggerListener extends ListenerAdapter {
	
	private static final int LOG_DELAY = 500;
	
	private static final Color COLOR_GREEN = Color.decode("#5fe468");
	private static final Color COLOR_RED = Color.decode("#f84b50");
	private static final Color COLOR_ORANGE = Color.decode("#e6842b");
	
	/* 
	 * Used to delay all logs so we can get the correct action as well as the correct audit logs,
	 * the reason all logs are delayed and not just the ones that use audit logs is
	 * to keep the order of the logs, this can be done in other ways as well but this is the 
	 * easiest. 
	 */
	private ScheduledExecutorService delayer = Executors.newSingleThreadScheduledExecutor();
	
	private void delay(Runnable runnable) {
		this.delayer.schedule(runnable, LoggerListener.LOG_DELAY, TimeUnit.MILLISECONDS);
	}
	
	private String getPermissionDifference(long before, long after) {
		StringBuilder builder = new StringBuilder();
		
		long difference = before ^ after;
		
		EnumSet<Permission> added = Permission.getPermissions(after & difference);
		EnumSet<Permission> removed = Permission.getPermissions(before & difference);
		
		if(added.size() + removed.size() > 0) {
			builder.append("\n```diff");
			
			added.forEach(permission -> builder.append("\n+ " + permission.getName()));
			removed.forEach(permission -> builder.append("\n- " + permission.getName()));
			
			builder.append("```");
		}
		
		return builder.toString();
	}
	
	private String getType(GuildChannel channel) {
		if(channel.getType().equals(ChannelType.CATEGORY)) {
			return "category";
		}else if(channel.getType().equals(ChannelType.UNKNOWN)) {
			return "unknown channel";
		}
		
		return channel.getType().toString().toLowerCase() + " channel";
	}
	
	public void onGuildMemberJoin(GuildMemberJoinEvent event) {
		Document logger = LoggerClient.get().getLogger(event.getGuild(), LoggerType.MEMBER_JOIN);
		if(logger == null) {
			return;
		}
		
		this.delay(() -> {
			WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
			embed.setDescription(String.format("`%s` has joined the server", event.getMember().getEffectiveName()));
			embed.setAuthor(event.getUser().getAsTag(), event.getUser().getEffectiveAvatarUrl());
			embed.setTimestamp(ZonedDateTime.now());
			embed.setFooter(String.format("User ID: %s", event.getUser().getId()));
			embed.setColor(LoggerListener.COLOR_GREEN);
			
			LoggerClient.get().queue(event.getGuild(), LoggerType.MEMBER_JOIN, embed.build());
		});
	}
	
	/* TODO: Check for kick */
	public void onGuildMemberLeave(GuildMemberLeaveEvent event) {
		Document logger = LoggerClient.get().getLogger(event.getGuild(), LoggerType.MEMBER_LEAVE);
		if(logger == null) {
			return;
		}
		
		this.delay(() -> {
			WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
			embed.setDescription(String.format("**%s** has left the server", event.getUser().getAsTag()));
			embed.setAuthor(event.getUser().getAsTag(), event.getUser().getEffectiveAvatarUrl());
			embed.setTimestamp(ZonedDateTime.now());
			embed.setFooter(String.format("User ID: %s", event.getUser().getId()));
			embed.setColor(LoggerListener.COLOR_RED);
			
			LoggerClient.get().queue(event.getGuild(), LoggerType.MEMBER_LEAVE, embed.build());
		});
	}
	
	/* TODO: Add support for multiple roles */
	@SuppressWarnings("unchecked")
	public void onGuildMemberRoleAdd(GuildMemberRoleAddEvent event) {
		Document logger = LoggerClient.get().getLogger(event.getGuild(), LoggerType.MEMBER_ROLE_ADD);
		if(logger == null) {
			return;
		}
		
		this.delay(() -> {
			WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
			embed.setDescription(String.format("The role %s has been added to `%s`", event.getRoles().get(0).getAsMention(), event.getMember().getEffectiveName()));
			embed.setAuthor(event.getUser().getAsTag(), event.getUser().getEffectiveAvatarUrl());
			embed.setTimestamp(ZonedDateTime.now());
			embed.setFooter(String.format("Role ID: %s", event.getRoles().get(0).getId()));
			embed.setColor(LoggerListener.COLOR_GREEN);
			
			if(event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
				event.getGuild().retrieveAuditLogs().type(ActionType.MEMBER_ROLE_UPDATE).queue(logs -> {
					AuditLogEntry entry = logs.stream()
						.filter(e -> e.getTargetIdLong() == event.getUser().getIdLong())
						.filter(e -> e.getChangeByKey(AuditLogKey.MEMBER_ROLES_ADD) != null)
						.filter(e -> {
							List<String> roleIds = ((List<Map<String, String>>) e.getChangeByKey(AuditLogKey.MEMBER_ROLES_ADD).getNewValue())
								.stream()
								.map(role -> role.get("id"))
								.collect(Collectors.toList());
							
							return event.getRoles().stream()
								.map(role -> role.getId())
								.allMatch(id -> roleIds.contains(id));
						})
						.findFirst()
						.orElse(null);
					
					if(entry != null) {
						embed.appendDescription(String.format(" by **%s**", entry.getUser().getAsTag()));
					}
					
					LoggerClient.get().queue(event.getGuild(), LoggerType.MEMBER_ROLE_ADD, embed.build());
				});
			}else{
				LoggerClient.get().queue(event.getGuild(), LoggerType.MEMBER_ROLE_ADD, embed.build());
			}
		});
	}
	
	/* TODO: Add support for multiple roles */
	@SuppressWarnings("unchecked")
	public void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event) {
		Document logger = LoggerClient.get().getLogger(event.getGuild(), LoggerType.MEMBER_ROLE_REMOVE);
		if(logger == null) {
			return;
		}
		
		this.delay(() -> {
			WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
			embed.setDescription(String.format("The role %s has been removed from `%s`", event.getRoles().get(0).getAsMention(), event.getMember().getEffectiveName()));
			embed.setAuthor(event.getUser().getAsTag(), event.getUser().getEffectiveAvatarUrl());
			embed.setTimestamp(ZonedDateTime.now());
			embed.setFooter(String.format("Role ID: %s", event.getRoles().get(0).getId()));
			embed.setColor(LoggerListener.COLOR_RED);
			
			if(event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
				event.getGuild().retrieveAuditLogs().type(ActionType.MEMBER_ROLE_UPDATE).queue(logs -> {
					AuditLogEntry entry = logs.stream()
						.filter(e -> e.getTargetIdLong() == event.getUser().getIdLong())
						.filter(e -> e.getChangeByKey(AuditLogKey.MEMBER_ROLES_REMOVE) != null)
						.filter(e -> {
							List<String> roleIds = ((List<Map<String, String>>) e.getChangeByKey(AuditLogKey.MEMBER_ROLES_REMOVE).getNewValue())
								.stream()
								.map(role -> role.get("id"))
								.collect(Collectors.toList());
							
							return event.getRoles().stream()
								.map(role -> role.getId())
								.allMatch(id -> roleIds.contains(id));
						})
						.findFirst()
						.orElse(null);
					
					if(entry != null) {
						embed.appendDescription(String.format(" by **%s**", entry.getUser().getAsTag()));
					}
					
					LoggerClient.get().queue(event.getGuild(), LoggerType.MEMBER_ROLE_REMOVE, embed.build());
				});
			}else{
				LoggerClient.get().queue(event.getGuild(), LoggerType.MEMBER_ROLE_REMOVE, embed.build());
			}
		});
	}
	
	public void onGuildMemberUpdateNickname(GuildMemberUpdateNicknameEvent event) {
		Document logger = LoggerClient.get().getLogger(event.getGuild(), LoggerType.MEMBER_NICKNAME_CHANGE);
		if(logger == null) {
			return;
		}
		
		this.delay(() -> {
			WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
			
			if(event.getOldNickname() != null && event.getNewNickname() != null) {
				embed.setDescription(String.format("The nickname of `%s` has been changed from `%s` to `%s`", event.getUser().getName(), event.getOldNickname(), event.getNewNickname()));
			}else if(event.getOldNickname() != null) {
				embed.setDescription(String.format("The nickname of `%s` [`%s`] has been removed", event.getUser().getName(), event.getOldNickname()));
			}else{
				embed.setDescription(String.format("The nickname of `%s` has been set to `%s`", event.getUser().getName(), event.getNewNickname()));
			}
			
			embed.setAuthor(event.getUser().getAsTag(), event.getUser().getEffectiveAvatarUrl());
			embed.setTimestamp(ZonedDateTime.now());
			embed.setFooter(String.format("User ID: %s", event.getUser().getId()));
			embed.setColor(LoggerListener.COLOR_ORANGE);
			
			if(event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
				event.getGuild().retrieveAuditLogs().type(ActionType.MEMBER_UPDATE).queue(logs -> {
						AuditLogEntry entry = logs.stream()
							.filter(e -> e.getTargetIdLong() == event.getUser().getIdLong())
							.findFirst()
							.orElse(null);
						
						if(entry != null) {
							embed.appendDescription(String.format(" by **%s**", entry.getUser().getAsTag()));
						}
						
						LoggerClient.get().queue(event.getGuild(), LoggerType.MEMBER_NICKNAME_CHANGE, embed.build());
					});
			}else{
				LoggerClient.get().queue(event.getGuild(), LoggerType.MEMBER_NICKNAME_CHANGE, embed.build());
			}
		});
	}
	
	public void onGuildBan(GuildBanEvent event) {
		Document logger = LoggerClient.get().getLogger(event.getGuild(), LoggerType.MEMBER_BAN);
		if(logger == null) {
			return;
		}
		
		this.delay(() -> {
			WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
			embed.setDescription(String.format("`%s` has been banned", event.getUser().getName()));
			embed.setAuthor(event.getUser().getAsTag(), event.getUser().getEffectiveAvatarUrl());
			embed.setTimestamp(ZonedDateTime.now());
			embed.setFooter(String.format("User ID: %s", event.getUser().getId()));
			embed.setColor(LoggerListener.COLOR_RED);
			
			if(event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
				event.getGuild().retrieveAuditLogs().type(ActionType.BAN).queue(logs -> {
					AuditLogEntry entry = logs.stream()
						.filter(e -> e.getTargetIdLong() == event.getUser().getIdLong())
						.findFirst()
						.orElse(null);
					
					if(entry != null) {
						embed.appendDescription(String.format(" by **%s**", entry.getUser().getAsTag()));
					}
					
					LoggerClient.get().queue(event.getGuild(), LoggerType.MEMBER_BAN, embed.build());
				});
			}else{
				LoggerClient.get().queue(event.getGuild(), LoggerType.MEMBER_BAN, embed.build());
			}
		});
	}
	
	public void onGuildUnban(GuildUnbanEvent event) {
		Document logger = LoggerClient.get().getLogger(event.getGuild(), LoggerType.MEMBER_UNBAN);
		if(logger == null) {
			return;
		}
		
		this.delay(() -> {
			WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
			embed.setDescription(String.format("`%s` has been unbanned", event.getUser().getName()));
			embed.setAuthor(event.getUser().getAsTag(), event.getUser().getEffectiveAvatarUrl());
			embed.setTimestamp(ZonedDateTime.now());
			embed.setFooter(String.format("User ID: %s", event.getUser().getId()));
			embed.setColor(LoggerListener.COLOR_GREEN);
			
			if(event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
				event.getGuild().retrieveAuditLogs().type(ActionType.UNBAN).queue(logs -> {
					AuditLogEntry entry = logs.stream()
						.filter(e -> e.getTargetIdLong() == event.getUser().getIdLong())
						.findFirst()
						.orElse(null);
					
					if(entry != null) {
						embed.appendDescription(String.format(" by **%s**", entry.getUser().getAsTag()));
					}
					
					LoggerClient.get().queue(event.getGuild(), LoggerType.MEMBER_UNBAN, embed.build());
				});
			}else{
				LoggerClient.get().queue(event.getGuild(), LoggerType.MEMBER_UNBAN, embed.build());
			}
		});
	}
	
	public void onRoleCreate(RoleCreateEvent event) {
		Document logger = LoggerClient.get().getLogger(event.getGuild(), LoggerType.ROLE_CREATE);
		if(logger == null) {
			return;
		}
		
		this.delay(() -> {
			WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
			embed.setDescription(String.format("The role %s has been created", event.getRole().getAsMention()));
			embed.setAuthor(event.getGuild().getName(), event.getGuild().getIconUrl());
			embed.setTimestamp(ZonedDateTime.now());
			embed.setFooter(String.format("Role ID: %s", event.getRole().getId()));
			embed.setColor(LoggerListener.COLOR_GREEN);
			
			if(event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
				event.getGuild().retrieveAuditLogs().type(ActionType.ROLE_CREATE).queue(logs -> {
					AuditLogEntry entry = logs.stream()
						.filter(e -> e.getTargetIdLong() == event.getRole().getIdLong())
						.findFirst()
						.orElse(null);
					
					if(entry != null) {
						embed.appendDescription(String.format(" by **%s**", entry.getUser().getAsTag()));
					}
					
					LoggerClient.get().queue(event.getGuild(), LoggerType.ROLE_CREATE, embed.build());
				});
			}else{
				LoggerClient.get().queue(event.getGuild(), LoggerType.ROLE_CREATE, embed.build());
			}
		});
	}
	
	public void onRoleDelete(RoleDeleteEvent event) {
		Document logger = LoggerClient.get().getLogger(event.getGuild(), LoggerType.ROLE_DELETE);
		if(logger == null) {
			return;
		}
		
		this.delay(() -> {
			WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
			embed.setDescription(String.format("The role `%s` has been deleted", event.getRole().getName()));
			embed.setAuthor(event.getGuild().getName(), event.getGuild().getIconUrl());
			embed.setTimestamp(ZonedDateTime.now());
			embed.setFooter(String.format("Role ID: %s", event.getRole().getId()));
			embed.setColor(LoggerListener.COLOR_RED);
			
			if(event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
				event.getGuild().retrieveAuditLogs().type(ActionType.ROLE_DELETE).queue(logs -> {
					AuditLogEntry entry = logs.stream()
						.filter(e -> e.getTargetIdLong() == event.getRole().getIdLong())
						.findFirst()
						.orElse(null);
					
					if(entry != null) {
						embed.appendDescription(String.format(" by **%s**", entry.getUser().getAsTag()));
					}
					
					LoggerClient.get().queue(event.getGuild(), LoggerType.ROLE_DELETE, embed.build());
				});
			}else{
				LoggerClient.get().queue(event.getGuild(), LoggerType.ROLE_DELETE, embed.build());
			}
		});
	}
	
	public void onRoleUpdateColor(RoleUpdateColorEvent event) {
		Document logger = LoggerClient.get().getLogger(event.getGuild(), LoggerType.ROLE_UPDATE_COLOR);
		if(logger == null) {
			return;
		}
		
		this.delay(() -> {
			WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
			embed.setDescription(String.format("The role colour %s has been changed from `%s` to `%s`", event.getRole().getAsMention(), Integer.toHexString(event.getOldColorRaw()), Integer.toHexString(event.getNewColorRaw())));
			embed.setAuthor(event.getGuild().getName(), event.getGuild().getIconUrl());
			embed.setTimestamp(ZonedDateTime.now());
			embed.setFooter(String.format("Role ID: %s", event.getRole().getId()));
			embed.setColor(LoggerListener.COLOR_ORANGE);
			
			if(event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
				event.getGuild().retrieveAuditLogs().type(ActionType.ROLE_UPDATE).queue(logs -> {
					AuditLogEntry entry = logs.stream()
						.filter(e -> e.getChangeByKey(AuditLogKey.ROLE_COLOR) != null)
						.filter(e -> e.getTargetIdLong() == event.getRole().getIdLong())
						.findFirst()
						.orElse(null);
					
					if(entry != null) {
						embed.appendDescription(String.format(" by **%s**", entry.getUser().getAsTag()));
					}
					
					LoggerClient.get().queue(event.getGuild(), LoggerType.ROLE_UPDATE_COLOR, embed.build());
				});
			}else{
				LoggerClient.get().queue(event.getGuild(), LoggerType.ROLE_UPDATE_COLOR, embed.build());
			}
		});
	}
	
	public void onRoleUpdateHoisted(RoleUpdateHoistedEvent event) {
		Document logger = LoggerClient.get().getLogger(event.getGuild(), LoggerType.ROLE_UPDATE_HOISTED);
		if(logger == null) {
			return;
		}
		
		this.delay(() -> {
			WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
			embed.setDescription(String.format("The role %s has been changed to " + (event.getNewValue() ? "be" : "no longer be") + " hoisted", event.getRole().getAsMention()));
			embed.setAuthor(event.getGuild().getName(), event.getGuild().getIconUrl());
			embed.setTimestamp(ZonedDateTime.now());
			embed.setFooter(String.format("Role ID: %s", event.getRole().getId()));
			embed.setColor(LoggerListener.COLOR_ORANGE);
			
			if(event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
				event.getGuild().retrieveAuditLogs().type(ActionType.ROLE_UPDATE).queue(logs -> {
					AuditLogEntry entry = logs.stream()
						.filter(e -> e.getChangeByKey(AuditLogKey.ROLE_HOISTED) != null)
						.filter(e -> e.getTargetIdLong() == event.getRole().getIdLong())
						.findFirst()
						.orElse(null);
					
					if(entry != null) {
						embed.appendDescription(String.format(" by **%s**", entry.getUser().getAsTag()));
					}
					
					LoggerClient.get().queue(event.getGuild(), LoggerType.ROLE_UPDATE_HOISTED, embed.build());
				});
			}else{
				LoggerClient.get().queue(event.getGuild(), LoggerType.ROLE_UPDATE_HOISTED, embed.build());
			}
		});
	}
	
	public void onRoleUpdateMentionable(RoleUpdateMentionableEvent event) {
		Document logger = LoggerClient.get().getLogger(event.getGuild(), LoggerType.ROLE_UPDATE_MENTIONABLE);
		if(logger == null) {
			return;
		}
		
		this.delay(() -> {
			WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
			embed.setDescription(String.format("The role %s has been changed to " + (event.getNewValue() ? "be" : "no longer be") + " mentionable", event.getRole().getAsMention()));
			embed.setAuthor(event.getGuild().getName(), event.getGuild().getIconUrl());
			embed.setTimestamp(ZonedDateTime.now());
			embed.setFooter(String.format("Role ID: %s", event.getRole().getId()));
			embed.setColor(LoggerListener.COLOR_ORANGE);
			
			if(event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
				event.getGuild().retrieveAuditLogs().type(ActionType.ROLE_UPDATE).queue(logs -> {
					AuditLogEntry entry = logs.stream()
						.filter(e -> e.getChangeByKey(AuditLogKey.ROLE_MENTIONABLE) != null)
						.filter(e -> e.getTargetIdLong() == event.getRole().getIdLong())
						.findFirst()
						.orElse(null);
					
					if(entry != null) {
						embed.appendDescription(String.format(" by **%s**", entry.getUser().getAsTag()));
					}
					
					LoggerClient.get().queue(event.getGuild(), LoggerType.ROLE_UPDATE_MENTIONABLE, embed.build());
				});
			}else{
				LoggerClient.get().queue(event.getGuild(), LoggerType.ROLE_UPDATE_MENTIONABLE, embed.build());
			}
		});
	}
	
	public void onRoleUpdateName(RoleUpdateNameEvent event) {
		Document logger = LoggerClient.get().getLogger(event.getGuild(), LoggerType.ROLE_UPDATE_NAME);
		if(logger == null) {
			return;
		}
		
		this.delay(() -> {
			WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
			embed.setDescription(String.format("The role %s has been renamed from `%s` to `%s`", event.getRole().getAsMention(), event.getOldName(), event.getNewName()));
			embed.setAuthor(event.getGuild().getName(), event.getGuild().getIconUrl());
			embed.setTimestamp(ZonedDateTime.now());
			embed.setFooter(String.format("Role ID: %s", event.getRole().getId()));
			embed.setColor(LoggerListener.COLOR_ORANGE);
			
			if(event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
				event.getGuild().retrieveAuditLogs().type(ActionType.ROLE_UPDATE).queue(logs -> {
					AuditLogEntry entry = logs.stream()
						.filter(e -> e.getChangeByKey(AuditLogKey.ROLE_NAME) != null)
						.filter(e -> e.getTargetIdLong() == event.getRole().getIdLong())
						.findFirst()
						.orElse(null);
					
					if(entry != null) {
						embed.appendDescription(String.format(" by **%s**", entry.getUser().getAsTag()));
					}
					
					LoggerClient.get().queue(event.getGuild(), LoggerType.ROLE_UPDATE_NAME, embed.build());
				});
			}else{
				LoggerClient.get().queue(event.getGuild(), LoggerType.ROLE_UPDATE_NAME, embed.build());
			}
		});
	}
	
	public void onRoleUpdatePermissions(RoleUpdatePermissionsEvent event) {
		Document logger = LoggerClient.get().getLogger(event.getGuild(), LoggerType.ROLE_UPDATE_PERMISSIONS);
		if(logger == null) {
			return;
		}
		
		this.delay(() -> {
			WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
			embed.setDescription(String.format("The role %s has had permission changes", event.getRole().getAsMention()));
			embed.setAuthor(event.getGuild().getName(), event.getGuild().getIconUrl());
			embed.setTimestamp(ZonedDateTime.now());
			embed.setFooter(String.format("Role ID: %s", event.getRole().getId()));
			embed.setColor(LoggerListener.COLOR_ORANGE);
			
			if(event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
				event.getGuild().retrieveAuditLogs().type(ActionType.ROLE_UPDATE).queue(logs -> {
					AuditLogEntry entry = logs.stream()
						.filter(e -> e.getChangeByKey(AuditLogKey.ROLE_PERMISSIONS) != null)
						.filter(e -> e.getTargetIdLong() == event.getRole().getIdLong())
						.findFirst()
						.orElse(null);
					
					if(entry != null) {
						embed.appendDescription(String.format(" made by **%s**", entry.getUser().getAsTag()));
						embed.appendDescription(this.getPermissionDifference(event.getOldPermissionsRaw(), event.getNewPermissionsRaw()));
					}
					
					LoggerClient.get().queue(event.getGuild(), LoggerType.ROLE_UPDATE_PERMISSIONS, embed.build());
				});
			}else{
				embed.appendDescription(this.getPermissionDifference(event.getOldPermissionsRaw(), event.getNewPermissionsRaw()));
				
				LoggerClient.get().queue(event.getGuild(), LoggerType.ROLE_UPDATE_PERMISSIONS, embed.build());
			}
		});
	}
	
	public void onEmoteAdded(EmoteAddedEvent event) {
		Document logger = LoggerClient.get().getLogger(event.getGuild(), LoggerType.EMOTE_CREATE);
		if(logger == null) {
			return;
		}
		
		this.delay(() -> {
			WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
			embed.setDescription(String.format("The emote %s has been created", event.getEmote().getAsMention()));
			embed.setAuthor(event.getGuild().getName(), event.getGuild().getIconUrl());
			embed.setTimestamp(ZonedDateTime.now());
			embed.setFooter(String.format("Emote ID: %s", event.getEmote().getId()));
			embed.setColor(LoggerListener.COLOR_GREEN);
			
			if(event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
				event.getGuild().retrieveAuditLogs().type(ActionType.EMOTE_CREATE).queue(logs -> {
					AuditLogEntry entry = logs.stream()
						.filter(e -> e.getTargetIdLong() == event.getEmote().getIdLong())
						.findFirst()
						.orElse(null);
					
					if(entry != null) {
						embed.appendDescription(String.format(" by **%s**", entry.getUser().getAsTag()));
					}
					
					LoggerClient.get().queue(event.getGuild(), LoggerType.EMOTE_CREATE, embed.build());
				});
			}else{
				LoggerClient.get().queue(event.getGuild(), LoggerType.EMOTE_CREATE, embed.build());
			}
		});
	}
	
	public void onEmoteRemoved(EmoteRemovedEvent event) {
		Document logger = LoggerClient.get().getLogger(event.getGuild(), LoggerType.EMOTE_DELETE);
		if(logger == null) {
			return;
		}
		
		this.delay(() -> {
			WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
			embed.setDescription(String.format("The emote `%s` has been deleted", event.getEmote().getAsMention()));
			embed.setAuthor(event.getGuild().getName(), event.getGuild().getIconUrl());
			embed.setTimestamp(ZonedDateTime.now());
			embed.setFooter(String.format("Emote ID: %s", event.getEmote().getId()));
			embed.setColor(LoggerListener.COLOR_RED);
			
			if(event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
				event.getGuild().retrieveAuditLogs().type(ActionType.EMOTE_DELETE).queue(logs -> {
					AuditLogEntry entry = logs.stream()
						.filter(e -> e.getTargetIdLong() == event.getEmote().getIdLong())
						.findFirst()
						.orElse(null);
					
					if(entry != null) {
						embed.appendDescription(String.format(" by **%s**", entry.getUser().getAsTag()));
					}
					
					LoggerClient.get().queue(event.getGuild(), LoggerType.EMOTE_DELETE, embed.build());
				});
			}else{
				LoggerClient.get().queue(event.getGuild(), LoggerType.EMOTE_DELETE, embed.build());
			}
		});
	}
	
	public void onEmoteUpdateName(EmoteUpdateNameEvent event) {
		Document logger = LoggerClient.get().getLogger(event.getGuild(), LoggerType.EMOTE_UPDATE_NAME);
		if(logger == null) {
			return;
		}
		
		this.delay(() -> {
			WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
			embed.setDescription(String.format("The emote %s has been renamed from `%s` to `%s`", event.getEmote().getAsMention(), event.getOldName(), event.getNewName()));
			embed.setAuthor(event.getGuild().getName(), event.getGuild().getIconUrl());
			embed.setTimestamp(ZonedDateTime.now());
			embed.setFooter(String.format("Emote ID: %s", event.getEmote().getId()));
			embed.setColor(LoggerListener.COLOR_ORANGE);
			
			if(event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
				event.getGuild().retrieveAuditLogs().type(ActionType.EMOTE_UPDATE).queue(logs -> {
					AuditLogEntry entry = logs.stream()
						.filter(e -> e.getChangeByKey(AuditLogKey.EMOTE_NAME) != null)
						.filter(e -> e.getTargetIdLong() == event.getEmote().getIdLong())
						.findFirst()
						.orElse(null);
					
					if(entry != null) {
						embed.appendDescription(String.format(" by **%s**", entry.getUser().getAsTag()));
					}
					
					LoggerClient.get().queue(event.getGuild(), LoggerType.EMOTE_UPDATE_NAME, embed.build());
				});
			}else{
				LoggerClient.get().queue(event.getGuild(), LoggerType.EMOTE_UPDATE_NAME, embed.build());
			}
		});
	}
	
	/* TODO: Implement */
	public void onEmoteUpdateRoles(EmoteUpdateRolesEvent event) {}
	
	public void onGuildUpdateAfkChannel(GuildUpdateAfkChannelEvent event) {
		Document logger = LoggerClient.get().getLogger(event.getGuild(), LoggerType.GUILD_UPDATE_AFK_CHANNEL);
		if(logger == null) {
			return;
		}
		
		this.delay(() -> {
			WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
			
			if(event.getOldAfkChannel() == null) {
				embed.setDescription(String.format("The AFK channel has been set to `%s`", event.getNewAfkChannel().getName()));
			}else if(event.getNewAfkChannel() == null) {
				embed.setDescription(String.format("The AFK channel [`%s`] has been removed", event.getOldAfkChannel().getName()));
			}else{
				embed.setDescription(String.format("The AFK channel has been changed from `%s` to `%s`", event.getOldAfkChannel().getName(), event.getNewAfkChannel().getName()));
			}
			
			embed.setColor(LoggerListener.COLOR_ORANGE);
			embed.setTimestamp(ZonedDateTime.now());
			embed.setAuthor(event.getGuild().getName(), event.getGuild().getIconUrl());
			
			if(event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
				event.getGuild().retrieveAuditLogs().type(ActionType.GUILD_UPDATE).queue(logs -> {
					AuditLogEntry entry = logs.stream()
						.filter(e -> e.getChangeByKey(AuditLogKey.GUILD_AFK_CHANNEL) != null)
						.findFirst()
						.orElse(null);
					
					if(entry != null) {
						embed.appendDescription(String.format(" by **%s**", entry.getUser().getAsTag()));
					}
					
					LoggerClient.get().queue(event.getGuild(), LoggerType.GUILD_UPDATE_AFK_CHANNEL, embed.build());
				});
			}else{
				LoggerClient.get().queue(event.getGuild(), LoggerType.GUILD_UPDATE_AFK_CHANNEL, embed.build());
			}
		});
	}
	
	public void onGuildUpdateAfkTimeout(GuildUpdateAfkTimeoutEvent event) {
		Document logger = LoggerClient.get().getLogger(event.getGuild(), LoggerType.GUILD_UPDATE_AFK_TIMEOUT);
		if(logger == null) {
			return;
		}
		
		this.delay(() -> {
			WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
			embed.setDescription(String.format("The AFK timeout has been changed from `%s` seconds to `%s`", event.getOldAfkTimeout().getSeconds(), event.getNewAfkTimeout().getSeconds()));
			embed.setColor(LoggerListener.COLOR_ORANGE);
			embed.setTimestamp(ZonedDateTime.now());
			embed.setAuthor(event.getGuild().getName(), event.getGuild().getIconUrl());
			
			if(event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
				event.getGuild().retrieveAuditLogs().type(ActionType.GUILD_UPDATE).queue(logs -> {
					AuditLogEntry entry = logs.stream()
						.filter(e -> e.getChangeByKey(AuditLogKey.GUILD_AFK_TIMEOUT) != null)
						.findFirst()
						.orElse(null);
					
					if(entry != null) {
						embed.appendDescription(String.format(" by **%s**", entry.getUser().getAsTag()));
					}
					
					LoggerClient.get().queue(event.getGuild(), LoggerType.GUILD_UPDATE_AFK_TIMEOUT, embed.build());
				});
			}else{
				LoggerClient.get().queue(event.getGuild(), LoggerType.GUILD_UPDATE_AFK_TIMEOUT, embed.build());
			}
		});
	}
	
	public void onGuildUpdateExplicitContentLevel(GuildUpdateExplicitContentLevelEvent event) {
		Document logger = LoggerClient.get().getLogger(event.getGuild(), LoggerType.GUILD_UPDATE_EXPLICIT_CONTENT_LEVEL);
		if(logger == null) {
			return;
		}
		
		this.delay(() -> {
			WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
			embed.setDescription(String.format("The explicit content level has been changed from `%s` to `%s`", event.getOldLevel().getDescription(), event.getNewLevel().getDescription()));
			embed.setColor(LoggerListener.COLOR_ORANGE);
			embed.setTimestamp(ZonedDateTime.now());
			embed.setAuthor(event.getGuild().getName(), event.getGuild().getIconUrl());
			
			if(event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
				event.getGuild().retrieveAuditLogs().type(ActionType.GUILD_UPDATE).queue(logs -> {
					AuditLogEntry entry = logs.stream()
						.filter(e -> e.getChangeByKey(AuditLogKey.GUILD_EXPLICIT_CONTENT_FILTER) != null)
						.findFirst()
						.orElse(null);
					
					if(entry != null) {
						embed.appendDescription(String.format(" by **%s**", entry.getUser().getAsTag()));
					}
					
					LoggerClient.get().queue(event.getGuild(), LoggerType.GUILD_UPDATE_EXPLICIT_CONTENT_LEVEL, embed.build());
				});
			}else{
				LoggerClient.get().queue(event.getGuild(), LoggerType.GUILD_UPDATE_EXPLICIT_CONTENT_LEVEL, embed.build());
			}
		});
	}
	
	public void onGuildUpdateIcon(GuildUpdateIconEvent event) {
		Document logger = LoggerClient.get().getLogger(event.getGuild(), LoggerType.GUILD_UPDATE_ICON);
		if(logger == null) {
			return;
		}
		
		this.delay(() -> {
			WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
			embed.setDescription(String.format("The server icon has been changed"));
			embed.setColor(LoggerListener.COLOR_ORANGE);
			embed.setTimestamp(ZonedDateTime.now());
			embed.setAuthor(event.getGuild().getName(), event.getGuild().getIconUrl());
			
			if(event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
				event.getGuild().retrieveAuditLogs().type(ActionType.GUILD_UPDATE).queue(logs -> {
					AuditLogEntry entry = logs.stream()
						.filter(e -> e.getChangeByKey(AuditLogKey.GUILD_ICON) != null)
						.findFirst()
						.orElse(null);
					
					if(entry != null) {
						embed.appendDescription(String.format(" by **%s**", entry.getUser().getAsTag()));
					}
					
					LoggerClient.get().queue(event.getGuild(), LoggerType.GUILD_UPDATE_ICON, embed.build());
				});
			}else{
				LoggerClient.get().queue(event.getGuild(), LoggerType.GUILD_UPDATE_ICON, embed.build());
			}
		});
	}
	
	public void onGuildUpdateMFALevel(GuildUpdateMFALevelEvent event) {
		Document logger = LoggerClient.get().getLogger(event.getGuild(), LoggerType.GUILD_UPDATE_MFA_LEVEL);
		if(logger == null) {
			return;
		}
		
		this.delay(() -> {
			WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
			embed.setDescription(String.format("The server MFA (multi-factor authentication) level has been " + (event.getNewMFALevel().equals(MFALevel.TWO_FACTOR_AUTH) ? " set to `2FA (two factor authentication)`" : " removed")));
			embed.setColor(LoggerListener.COLOR_ORANGE);
			embed.setTimestamp(ZonedDateTime.now());
			embed.setAuthor(event.getGuild().getName(), event.getGuild().getIconUrl());
			
			if(event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
				event.getGuild().retrieveAuditLogs().type(ActionType.GUILD_UPDATE).queue(logs -> {
					AuditLogEntry entry = logs.stream()
						.filter(e -> e.getChangeByKey(AuditLogKey.GUILD_MFA_LEVEL) != null)
						.findFirst()
						.orElse(null);
					
					if(entry != null) {
						embed.appendDescription(String.format(" by **%s**", entry.getUser().getAsTag()));
					}
					
					LoggerClient.get().queue(event.getGuild(), LoggerType.GUILD_UPDATE_MFA_LEVEL, embed.build());
				});
			}else{
				LoggerClient.get().queue(event.getGuild(), LoggerType.GUILD_UPDATE_MFA_LEVEL, embed.build());
			}
		});
	}
	
	public void onGuildUpdateName(GuildUpdateNameEvent event) {
		Document logger = LoggerClient.get().getLogger(event.getGuild(), LoggerType.GUILD_UPDATE_NAME);
		if(logger == null) {
			return;
		}
		
		this.delay(() -> {
			WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
			embed.setDescription(String.format("The server name has been changed from `%s` to `%s`", event.getOldName(), event.getNewName()));
			embed.setColor(LoggerListener.COLOR_ORANGE);
			embed.setTimestamp(ZonedDateTime.now());
			embed.setAuthor(event.getGuild().getName(), event.getGuild().getIconUrl());
			
			if(event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
				event.getGuild().retrieveAuditLogs().type(ActionType.GUILD_UPDATE).queue(logs -> {
					AuditLogEntry entry = logs.stream()
						.filter(e -> e.getChangeByKey(AuditLogKey.GUILD_NAME) != null)
						.findFirst()
						.orElse(null);
					
					if(entry != null) {
						embed.appendDescription(String.format(" by **%s**", entry.getUser().getAsTag()));
					}
					
					LoggerClient.get().queue(event.getGuild(), LoggerType.GUILD_UPDATE_NAME, embed.build());
				});
			}else{
				LoggerClient.get().queue(event.getGuild(), LoggerType.GUILD_UPDATE_NAME, embed.build());
			}
		});
	}
	
	public void onGuildUpdateNotificationLevel(GuildUpdateNotificationLevelEvent event) {
		Document logger = LoggerClient.get().getLogger(event.getGuild(), LoggerType.GUILD_UPDATE_NOTIFICATION_LEVEL);
		if(logger == null) {
			return;
		}
		
		this.delay(() -> {
			WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
			embed.setDescription(String.format("The server notification level has been changed from `%s` to `%s`", event.getOldNotificationLevel().toString().replace("_", " ").toLowerCase(), event.getNewNotificationLevel().toString().replace("_", " ").toLowerCase()));
			embed.setColor(LoggerListener.COLOR_ORANGE);
			embed.setTimestamp(ZonedDateTime.now());
			embed.setAuthor(event.getGuild().getName(), event.getGuild().getIconUrl());
			
			if(event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
				event.getGuild().retrieveAuditLogs().type(ActionType.GUILD_UPDATE).queue(logs -> {
					AuditLogEntry entry = logs.stream()
						.filter(e -> e.getChangeByKey(AuditLogKey.GUILD_NOTIFICATION_LEVEL) != null)
						.findFirst()
						.orElse(null);
					
					if(entry != null) {
						embed.appendDescription(String.format(" by **%s**", entry.getUser().getAsTag()));
					}
					
					LoggerClient.get().queue(event.getGuild(), LoggerType.GUILD_UPDATE_NOTIFICATION_LEVEL, embed.build());
				});
			}else{
				LoggerClient.get().queue(event.getGuild(), LoggerType.GUILD_UPDATE_NOTIFICATION_LEVEL, embed.build());
			}
		});
	}
	
	public void onGuildUpdateOwner(GuildUpdateOwnerEvent event) {
		Document logger = LoggerClient.get().getLogger(event.getGuild(), LoggerType.GUILD_UPDATE_OWNER);
		if(logger == null) {
			return;
		}
		
		this.delay(() -> {
			WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
			embed.setDescription(String.format("The server ownership has been transferred from **%s** to **%s**", event.getOldOwner().getUser().getAsTag(), event.getNewOwner().getUser().getAsTag()));
			embed.setColor(LoggerListener.COLOR_ORANGE);
			embed.setTimestamp(ZonedDateTime.now());
			embed.setAuthor(event.getGuild().getName(), event.getGuild().getIconUrl());
			
			if(event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
				event.getGuild().retrieveAuditLogs().type(ActionType.GUILD_UPDATE).queue(logs -> {
					AuditLogEntry entry = logs.stream()
						.filter(e -> e.getChangeByKey(AuditLogKey.GUILD_OWNER) != null)
						.findFirst()
						.orElse(null);
					
					if(entry != null) {
						embed.appendDescription(String.format(" by **%s**", entry.getUser().getAsTag()));
					}
					
					LoggerClient.get().queue(event.getGuild(), LoggerType.GUILD_UPDATE_OWNER, embed.build());
				});
			}else{
				LoggerClient.get().queue(event.getGuild(), LoggerType.GUILD_UPDATE_OWNER, embed.build());
			}
		});
	}
	
	public void onGuildUpdateRegion(GuildUpdateRegionEvent event) {
		Document logger = LoggerClient.get().getLogger(event.getGuild(), LoggerType.GUILD_UPDATE_REGION);
		if(logger == null) {
			return;
		}
		
		this.delay(() -> {
			WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
			embed.setDescription(String.format("The server voice region has been changed from `%s` to `%s`", event.getOldRegion().getName(), event.getNewRegion().getName()));
			embed.setColor(LoggerListener.COLOR_ORANGE);
			embed.setTimestamp(ZonedDateTime.now());
			embed.setAuthor(event.getGuild().getName(), event.getGuild().getIconUrl());
			
			if(event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
				event.getGuild().retrieveAuditLogs().type(ActionType.GUILD_UPDATE).queue(logs -> {
					AuditLogEntry entry = logs.stream()
						.filter(e -> e.getChangeByKey(AuditLogKey.GUILD_REGION) != null)
						.findFirst()
						.orElse(null);
					
					if(entry != null) {
						embed.appendDescription(String.format(" by **%s**", entry.getUser().getAsTag()));
					}
					
					LoggerClient.get().queue(event.getGuild(), LoggerType.GUILD_UPDATE_REGION, embed.build());
				});
			}else{
				LoggerClient.get().queue(event.getGuild(), LoggerType.GUILD_UPDATE_REGION, embed.build());
			}
		});
	}
	
	public void onGuildUpdateSplash(GuildUpdateSplashEvent event) {
		Document logger = LoggerClient.get().getLogger(event.getGuild(), LoggerType.GUILD_UPDATE_SPLASH);
		if(logger == null) {
			return;
		}
		
		this.delay(() -> {
			WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
			embed.setDescription(String.format("The server splash has been changed"));
			embed.setColor(LoggerListener.COLOR_ORANGE);
			embed.setTimestamp(ZonedDateTime.now());
			embed.setAuthor(event.getGuild().getName(), event.getGuild().getIconUrl());
			
			if(event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
				event.getGuild().retrieveAuditLogs().type(ActionType.GUILD_UPDATE).queue(logs -> {
					AuditLogEntry entry = logs.stream()
						.filter(e -> e.getChangeByKey(AuditLogKey.GUILD_SPLASH) != null)
						.findFirst()
						.orElse(null);
					
					if(entry != null) {
						embed.appendDescription(String.format(" by **%s**", entry.getUser().getAsTag()));
					}
					
					LoggerClient.get().queue(event.getGuild(), LoggerType.GUILD_UPDATE_SPLASH, embed.build());
				});
			}else{
				LoggerClient.get().queue(event.getGuild(), LoggerType.GUILD_UPDATE_SPLASH, embed.build());
			}
		});
	}
	
	public void onGuildUpdateSystemChannel(GuildUpdateSystemChannelEvent event) {
		Document logger = LoggerClient.get().getLogger(event.getGuild(), LoggerType.GUILD_UPDATE_SYSTEM_CHANNEL);
		if(logger == null) {
			return;
		}
		
		this.delay(() -> {
			WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
			
			if(event.getOldSystemChannel() == null) {
				embed.setDescription(String.format("The server system channel has been set to %s", event.getNewSystemChannel().getAsMention()));
			}else if(event.getNewSystemChannel() == null) {
				embed.setDescription(String.format("The server system channel [%s] has been removed", event.getOldSystemChannel().getAsMention()));
			}else{
				embed.setDescription(String.format("The server system channel has been changed from %s to %s", event.getOldSystemChannel().getAsMention(), event.getNewSystemChannel().getAsMention()));
			}
			
			embed.setColor(LoggerListener.COLOR_ORANGE);
			embed.setTimestamp(ZonedDateTime.now());
			embed.setAuthor(event.getGuild().getName(), event.getGuild().getIconUrl());
			
			if(event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
				event.getGuild().retrieveAuditLogs().type(ActionType.GUILD_UPDATE).queue(logs -> {
					AuditLogEntry entry = logs.stream()
						.filter(e -> e.getChangeByKey(AuditLogKey.GUILD_SYSTEM_CHANNEL) != null)
						.findFirst()
						.orElse(null);
					
					if(entry != null) {
						embed.appendDescription(String.format(" by **%s**", entry.getUser().getAsTag()));
					}
					
					LoggerClient.get().queue(event.getGuild(), LoggerType.GUILD_UPDATE_SYSTEM_CHANNEL, embed.build());
				});
			}else{
				LoggerClient.get().queue(event.getGuild(), LoggerType.GUILD_UPDATE_SYSTEM_CHANNEL, embed.build());
			}
		});
	}
	
	public void onGuildUpdateVerificationLevel(GuildUpdateVerificationLevelEvent event) {
		Document logger = LoggerClient.get().getLogger(event.getGuild(), LoggerType.GUILD_UPDATE_VERIFICATION_LEVEL);
		if(logger == null) {
			return;
		}
		
		this.delay(() -> {
			WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
			embed.setDescription(String.format("The server verfication level has been changed from `%s` to `%s`", event.getOldVerificationLevel().toString().replace("_", " ").toLowerCase(), event.getNewVerificationLevel().toString().replace("_", " ").toLowerCase()));
			embed.setColor(LoggerListener.COLOR_ORANGE);
			embed.setTimestamp(ZonedDateTime.now());
			embed.setAuthor(event.getGuild().getName(), event.getGuild().getIconUrl());
			
			if(event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
				event.getGuild().retrieveAuditLogs().type(ActionType.GUILD_UPDATE).queue(logs -> {
					AuditLogEntry entry = logs.stream()
						.filter(e -> e.getChangeByKey(AuditLogKey.GUILD_VERIFICATION_LEVEL) != null)
						.findFirst()
						.orElse(null);
					
					if(entry != null) {
						embed.appendDescription(String.format(" by **%s**", entry.getUser().getAsTag()));
					}
					
					LoggerClient.get().queue(event.getGuild(), LoggerType.GUILD_UPDATE_VERIFICATION_LEVEL, embed.build());
				});
			}else{
				LoggerClient.get().queue(event.getGuild(), LoggerType.GUILD_UPDATE_VERIFICATION_LEVEL, embed.build());
			}
		});
	}
	
	public void onChannelCreate(GuildChannel channel, LoggerType type) {
		Document logger = LoggerClient.get().getLogger(channel.getGuild(), type);
		if(logger == null) {
			return;
		}
		
		this.delay(() -> {
			WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
			embed.setDescription(String.format("The " + this.getType(channel) + " %s has been created", (channel instanceof IMentionable ? ((IMentionable) channel).getAsMention() : "`" + channel.getName() + "`")));
			embed.setColor(LoggerListener.COLOR_GREEN);
			embed.setTimestamp(ZonedDateTime.now());
			embed.setAuthor(channel.getGuild().getName(), channel.getGuild().getIconUrl());
			embed.setFooter(String.format("%s ID: %s", channel.getType().equals(ChannelType.CATEGORY) ? "Category" : "Channel", channel.getId()));
			
			if(channel.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
				channel.getGuild().retrieveAuditLogs().type(ActionType.CHANNEL_CREATE).queue(logs -> {
					AuditLogEntry entry = logs.stream()
						.filter(e -> e.getTargetIdLong() == channel.getIdLong())
						.findFirst()
						.orElse(null);
					
					if(entry != null) {
						embed.appendDescription(String.format(" by **%s**", entry.getUser().getAsTag()));
					}
					
					LoggerClient.get().queue(channel.getGuild(), type, embed.build());
				});
			}else{
				LoggerClient.get().queue(channel.getGuild(), type, embed.build());
			}
		});
	}
	
	public void onTextChannelCreate(TextChannelCreateEvent event) {
		this.onChannelCreate(event.getChannel(), LoggerType.TEXT_CHANNEL_CREATE);
	}
	
	public void onVoiceChannelCreate(VoiceChannelCreateEvent event) {
		this.onChannelCreate(event.getChannel(), LoggerType.VOICE_CHANNEL_CREATE);
	}
	
	public void onStoreChannelCreate(StoreChannelCreateEvent event) {
		this.onChannelCreate(event.getChannel(), LoggerType.STORE_CHANNEL_CREATE);
	}
	
	public void onCategoryCreate(CategoryCreateEvent event) {
		this.onChannelCreate(event.getCategory(), LoggerType.CATEGORY_CHANNEL_CREATE);
	}
	
	public void onChannelDelete(GuildChannel channel, LoggerType type) {
		Document logger = LoggerClient.get().getLogger(channel.getGuild(), type);
		if(logger == null) {
			return;
		}
		
		this.delay(() -> {
			WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
			embed.setDescription(String.format("The " + this.getType(channel) + " `%s` has been deleted", channel.getName()));
			embed.setColor(LoggerListener.COLOR_RED);
			embed.setTimestamp(ZonedDateTime.now());
			embed.setAuthor(channel.getGuild().getName(), channel.getGuild().getIconUrl());
			embed.setFooter(String.format("%s ID: %s", channel.getType().equals(ChannelType.CATEGORY) ? "Category" : "Channel", channel.getId()));
			
			if(channel.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
				channel.getGuild().retrieveAuditLogs().type(ActionType.CHANNEL_DELETE).queue(logs -> {
					AuditLogEntry entry = logs.stream()
						.filter(e -> e.getTargetIdLong() == channel.getIdLong())
						.findFirst()
						.orElse(null);
					
					if(entry != null) {
						embed.appendDescription(String.format(" by **%s**", entry.getUser().getAsTag()));
					}
					
					LoggerClient.get().queue(channel.getGuild(), type, embed.build());
				});
			}else{
				LoggerClient.get().queue(channel.getGuild(), type, embed.build());
			}
		});
	}
	
	public void onTextChannelDelete(TextChannelDeleteEvent event) {
		this.onChannelDelete(event.getChannel(), LoggerType.TEXT_CHANNEL_DELETE);
	}
	
	public void onVoiceChannelDelete(VoiceChannelDeleteEvent event) {
		this.onChannelDelete(event.getChannel(), LoggerType.VOICE_CHANNEL_DELETE);
	}
	
	public void onStoreChannelDelete(StoreChannelDeleteEvent event) {
		this.onChannelDelete(event.getChannel(), LoggerType.STORE_CHANNEL_DELETE);
	}
	
	public void onCategoryDelete(CategoryDeleteEvent event) {
		this.onChannelDelete(event.getCategory(), LoggerType.CATEGORY_CHANNEL_DELETE);
	}
	
	public void onChannelUpdateName(GuildChannel channel, String before, String after, LoggerType type) {
		Document logger = LoggerClient.get().getLogger(channel.getGuild(), type);
		if(logger == null) {
			return;
		}
		
		this.delay(() -> {
			WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
			embed.setDescription(String.format("The name of the " + this.getType(channel) + " %s has been changed from `%s` to `%s`", (channel instanceof IMentionable ? ((IMentionable) channel).getAsMention() : "`" + channel.getName() + "`"), before, after));
			embed.setColor(LoggerListener.COLOR_ORANGE);
			embed.setTimestamp(ZonedDateTime.now());
			embed.setAuthor(channel.getGuild().getName(), channel.getGuild().getIconUrl());
			embed.setFooter(String.format("%s ID: %s", channel.getType().equals(ChannelType.CATEGORY) ? "Category" : "Channel", channel.getId()));
			
			if(channel.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
				channel.getGuild().retrieveAuditLogs().type(ActionType.CHANNEL_UPDATE).queue(logs -> {
					AuditLogEntry entry = logs.stream()
						.filter(e -> e.getChangeByKey(AuditLogKey.CHANNEL_NAME) != null)
						.filter(e -> e.getTargetIdLong() == channel.getIdLong())
						.findFirst()
						.orElse(null);
					
					if(entry != null) {
						embed.appendDescription(String.format(" by **%s**", entry.getUser().getAsTag()));
					}
					
					LoggerClient.get().queue(channel.getGuild(), type, embed.build());
				});
			}else{
				LoggerClient.get().queue(channel.getGuild(), type, embed.build());
			}
		});
	}
	
	public void onTextChannelUpdateName(TextChannelUpdateNameEvent event) {
		this.onChannelUpdateName(event.getChannel(), event.getOldName(), event.getNewName(), LoggerType.TEXT_CHANNEL_UPDATE_NAME);
	}
	
	public void onVoiceChannelUpdateName(VoiceChannelUpdateNameEvent event) {
		this.onChannelUpdateName(event.getChannel(), event.getOldName(), event.getNewName(), LoggerType.VOICE_CHANNEL_UPDATE_NAME);
	}
	
	public void onStoreChannelUpdateName(StoreChannelUpdateNameEvent event) {
		this.onChannelUpdateName(event.getChannel(), event.getOldName(), event.getNewName(), LoggerType.STORE_CHANNEL_UPDATE_NAME);
	}
	
	public void onCategoryUpdateName(CategoryUpdateNameEvent event) {
		this.onChannelUpdateName(event.getCategory(), event.getOldName(), event.getNewName(), LoggerType.CATEGORY_CHANNEL_UPDATE_NAME);
	}
	
	public void onChannelUpdateParent(GuildChannel channel, Category before, Category after, LoggerType type) {
		Document logger = LoggerClient.get().getLogger(channel.getGuild(), type);
		if(logger == null) {
			return;
		}
		
		this.delay(() -> {
			WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
			embed.setDescription(String.format("The parent of the " + this.getType(channel) + " %s has been changed from `%s` to `%s`", (channel instanceof IMentionable ? ((IMentionable) channel).getAsMention() : "`" + channel.getName() + "`"), before.getName(), after.getName()));
			embed.setColor(LoggerListener.COLOR_ORANGE);
			embed.setTimestamp(ZonedDateTime.now());
			embed.setAuthor(channel.getGuild().getName(), channel.getGuild().getIconUrl());
			embed.setFooter(String.format("Channel ID: %s", channel.getId()));
			
			if(channel.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
				channel.getGuild().retrieveAuditLogs().type(ActionType.CHANNEL_UPDATE).queue(logs -> {
					AuditLogEntry entry = logs.stream()
						.filter(e -> e.getChangeByKey(AuditLogKey.CHANNEL_PARENT) != null)
						.filter(e -> e.getTargetIdLong() == channel.getIdLong())
						.findFirst()
						.orElse(null);
					
					if(entry != null) {
						embed.appendDescription(String.format(" by **%s**", entry.getUser().getAsTag()));
					}
					
					LoggerClient.get().queue(channel.getGuild(), type, embed.build());
				});
			}else{
				LoggerClient.get().queue(channel.getGuild(), type, embed.build());
			}
		});
	}
	
	public void onTextChannelUpdateParent(TextChannelUpdateParentEvent event) {
		this.onChannelUpdateParent(event.getChannel(), event.getOldParent(), event.getNewParent(), LoggerType.TEXT_CHANNEL_UPDATE_PARENT);
	}
	
	public void onVoiceChannelUpdateParent(VoiceChannelUpdateParentEvent event) {
		this.onChannelUpdateParent(event.getChannel(), event.getOldParent(), event.getNewParent(), LoggerType.VOICE_CHANNEL_UPDATE_PARENT);
	}
	
	public void onTextChannelUpdateNSFW(TextChannelUpdateNSFWEvent event) {
		Document logger = LoggerClient.get().getLogger(event.getGuild(), LoggerType.TEXT_CHANNEL_UPDATE_NSFW);
		if(logger == null) {
			return;
		}
		
		this.delay(() -> {
			WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
			embed.setDescription(String.format("The text channel %s is %s NSFW", event.getChannel().getAsMention(), event.getNewValue() ? "now" : "no longer"));
			embed.setColor(LoggerListener.COLOR_ORANGE);
			embed.setTimestamp(ZonedDateTime.now());
			embed.setAuthor(event.getGuild().getName(), event.getGuild().getIconUrl());
			embed.setFooter(String.format("Channel ID: %s", event.getChannel().getId()));
			
			if(event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
				event.getGuild().retrieveAuditLogs().type(ActionType.CHANNEL_UPDATE).queue(logs -> {
					AuditLogEntry entry = logs.stream()
						.filter(e -> e.getChangeByKey(AuditLogKey.CHANNEL_NSFW) != null)
						.filter(e -> e.getTargetIdLong() == event.getChannel().getIdLong())
						.findFirst()
						.orElse(null);
					
					if(entry != null) {
						embed.appendDescription(String.format(" by **%s**", entry.getUser().getAsTag()));
					}
					
					LoggerClient.get().queue(event.getGuild(), LoggerType.TEXT_CHANNEL_UPDATE_NSFW, embed.build());
				});
			}else{
				LoggerClient.get().queue(event.getGuild(), LoggerType.TEXT_CHANNEL_UPDATE_NSFW, embed.build());
			}
		});
	}
	
	public void onTextChannelUpdateSlowmode(TextChannelUpdateSlowmodeEvent event) {
		Document logger = LoggerClient.get().getLogger(event.getGuild(), LoggerType.TEXT_CHANNEL_UPDATE_SLOWMODE);
		if(logger == null) {
			return;
		}
		
		this.delay(() -> {
			WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
			embed.setDescription(String.format("The slowmode of the text channel %s has been changed from `%s` seconds to `%s`", event.getChannel().getAsMention(), event.getOldSlowmode(), event.getNewSlowmode()));
			embed.setColor(LoggerListener.COLOR_ORANGE);
			embed.setTimestamp(ZonedDateTime.now());
			embed.setAuthor(event.getGuild().getName(), event.getGuild().getIconUrl());
			embed.setFooter(String.format("Channel ID: %s", event.getChannel().getId()));
			
			if(event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
				event.getGuild().retrieveAuditLogs().type(ActionType.CHANNEL_UPDATE).queue(logs -> {
					AuditLogEntry entry = logs.stream()
						.filter(e -> e.getChangeByKey(AuditLogKey.CHANNEL_SLOWMODE) != null)
						.filter(e -> e.getTargetIdLong() == event.getChannel().getIdLong())
						.findFirst()
						.orElse(null);
					
					if(entry != null) {
						embed.appendDescription(String.format(" by **%s**", entry.getUser().getAsTag()));
					}
					
					LoggerClient.get().queue(event.getGuild(), LoggerType.TEXT_CHANNEL_UPDATE_SLOWMODE, embed.build());
				});
			}else{
				LoggerClient.get().queue(event.getGuild(), LoggerType.TEXT_CHANNEL_UPDATE_SLOWMODE, embed.build());
			}
		});
	}
	
	public void onTextChannelUpdateTopic(TextChannelUpdateTopicEvent event) {
		Document logger = LoggerClient.get().getLogger(event.getGuild(), LoggerType.TEXT_CHANNEL_UPDATE_TOPIC);
		if(logger == null) {
			return;
		}
		
		this.delay(() -> {
			WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
			
			if(event.getOldTopic() == null || event.getOldTopic().isEmpty()) {
				embed.setDescription(String.format("The topic of the text channel %s has been set to `%s`", event.getChannel().getAsMention(), event.getNewTopic()));
			}else if(event.getNewTopic() == null || event.getNewTopic().isEmpty()) {
				embed.setDescription(String.format("The topic of the text channel %s [`%s`] has been removed", event.getChannel().getAsMention(), event.getOldTopic()));
			}else{
				embed.setDescription(String.format("The topic of the text channel %s has been changed from `%s` to `%s`", event.getChannel().getAsMention(), event.getOldTopic(), event.getNewTopic()));
			}
			
			embed.setColor(LoggerListener.COLOR_ORANGE);
			embed.setTimestamp(ZonedDateTime.now());
			embed.setAuthor(event.getGuild().getName(), event.getGuild().getIconUrl());
			embed.setFooter(String.format("Channel ID: %s", event.getChannel().getId()));
			
			if(event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
				event.getGuild().retrieveAuditLogs().type(ActionType.CHANNEL_UPDATE).queue(logs -> {
					AuditLogEntry entry = logs.stream()
						.filter(e -> e.getChangeByKey(AuditLogKey.CHANNEL_TOPIC) != null)
						.filter(e -> e.getTargetIdLong() == event.getChannel().getIdLong())
						.findFirst()
						.orElse(null);
					
					if(entry != null) {
						embed.appendDescription(String.format(" by **%s**", entry.getUser().getAsTag()));
					}
					
					LoggerClient.get().queue(event.getGuild(), LoggerType.TEXT_CHANNEL_UPDATE_TOPIC, embed.build());
				});
			}else{
				LoggerClient.get().queue(event.getGuild(), LoggerType.TEXT_CHANNEL_UPDATE_TOPIC, embed.build());
			}
		});
	}
	
	public void onVoiceChannelUpdateBitrate(VoiceChannelUpdateBitrateEvent event) {
		Document logger = LoggerClient.get().getLogger(event.getGuild(), LoggerType.VOICE_CHANNEL_UPDATE_BITRATE);
		if(logger == null) {
			return;
		}
		
		this.delay(() -> {
			WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
			embed.setDescription(String.format("The bitrate of the voice channel `%s` has been changed from `%s` to `%s`", event.getChannel().getName(), event.getOldBitrate(), event.getNewBitrate()));
			embed.setColor(LoggerListener.COLOR_ORANGE);
			embed.setTimestamp(ZonedDateTime.now());
			embed.setAuthor(event.getGuild().getName(), event.getGuild().getIconUrl());
			embed.setFooter(String.format("Channel ID: %s", event.getChannel().getId()));
			
			if(event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
				event.getGuild().retrieveAuditLogs().type(ActionType.CHANNEL_UPDATE).queue(logs -> {
					AuditLogEntry entry = logs.stream()
						.filter(e -> e.getChangeByKey(AuditLogKey.CHANNEL_BITRATE) != null)
						.filter(e -> e.getTargetIdLong() == event.getChannel().getIdLong())
						.findFirst()
						.orElse(null);
					
					if(entry != null) {
						embed.appendDescription(String.format(" by **%s**", entry.getUser().getAsTag()));
					}
					
					LoggerClient.get().queue(event.getGuild(), LoggerType.VOICE_CHANNEL_UPDATE_BITRATE, embed.build());
				});
			}else{
				LoggerClient.get().queue(event.getGuild(), LoggerType.VOICE_CHANNEL_UPDATE_BITRATE, embed.build());
			}
		});
	}
	
	public void onVoiceChannelUpdateUserLimit(VoiceChannelUpdateUserLimitEvent event) {
		Document logger = LoggerClient.get().getLogger(event.getGuild(), LoggerType.VOICE_CHANNEL_UPDATE_USER_LIMIT);
		if(logger == null) {
			return;
		}
		
		this.delay(() -> {
			WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
			embed.setDescription(String.format("The user limit of the voice channel `%s` has been changed from `%s` to `%s`", event.getChannel().getName(), event.getOldUserLimit(), event.getNewUserLimit()));
			embed.setColor(LoggerListener.COLOR_ORANGE);
			embed.setTimestamp(ZonedDateTime.now());
			embed.setAuthor(event.getGuild().getName(), event.getGuild().getIconUrl());
			embed.setFooter(String.format("Channel ID: %s", event.getChannel().getId()));
			
			if(event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
				event.getGuild().retrieveAuditLogs().type(ActionType.CHANNEL_UPDATE).queue(logs -> {
					AuditLogEntry entry = logs.stream()
						.filter(e -> e.getChangeByKey(AuditLogKey.CHANNEL_USER_LIMIT) != null)
						.filter(e -> e.getTargetIdLong() == event.getChannel().getIdLong())
						.findFirst()
						.orElse(null);
					
					if(entry != null) {
						embed.appendDescription(String.format(" by **%s**", entry.getUser().getAsTag()));
					}
					
					LoggerClient.get().queue(event.getGuild(), LoggerType.VOICE_CHANNEL_UPDATE_USER_LIMIT, embed.build());
				});
			}else{
				LoggerClient.get().queue(event.getGuild(), LoggerType.VOICE_CHANNEL_UPDATE_USER_LIMIT, embed.build());
			}
		});
	}
	
	public void onGuildVoiceGuildDeafen(GuildVoiceGuildDeafenEvent event) {
		Document logger = LoggerClient.get().getLogger(event.getGuild(), LoggerType.VOICE_DEAFEN);
		if(logger == null) {
			return;
		}
		
		this.delay(() -> {
			WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
			embed.setDescription(String.format("**%s** has been %sdeafened", event.getMember().getUser().getAsTag(), event.isGuildDeafened() ? "" : "un"));
			
			if(event.isGuildDeafened()) {
				embed.setColor(LoggerListener.COLOR_RED);
			}else{
				embed.setColor(LoggerListener.COLOR_GREEN);
			}

			embed.setTimestamp(ZonedDateTime.now());
			embed.setAuthor(event.getGuild().getName(), event.getGuild().getIconUrl());
			embed.setFooter(String.format("User ID: %s", event.getMember().getId()));
			
			if(event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
				event.getGuild().retrieveAuditLogs().type(ActionType.MEMBER_UPDATE).queue(logs -> {
					AuditLogEntry entry = logs.stream()
						.filter(e -> e.getChangeByKey(AuditLogKey.MEMBER_DEAF) != null)
						.filter(e -> e.getTargetIdLong() == event.getMember().getIdLong())
						.findFirst()
						.orElse(null);
					
					if(entry != null) {
						embed.appendDescription(String.format(" by **%s**", entry.getUser().getAsTag()));
					}
					
					LoggerClient.get().queue(event.getGuild(), LoggerType.VOICE_DEAFEN, embed.build());
				});
			}else{
				LoggerClient.get().queue(event.getGuild(), LoggerType.VOICE_DEAFEN, embed.build());
			}
		});
	}
	
	public void onGuildVoiceGuildMute(GuildVoiceGuildMuteEvent event) {
		Document logger = LoggerClient.get().getLogger(event.getGuild(), LoggerType.VOICE_MUTE);
		if(logger == null) {
			return;
		}
		
		this.delay(() -> {
			WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
			embed.setDescription(String.format("**%s** has been %smuted", event.getMember().getUser().getAsTag(), event.isGuildMuted() ? "" : "un"));
			
			if(event.isGuildMuted()) {
				embed.setColor(LoggerListener.COLOR_RED);
			}else{
				embed.setColor(LoggerListener.COLOR_GREEN);
			}
			
			embed.setTimestamp(ZonedDateTime.now());
			embed.setAuthor(event.getGuild().getName(), event.getGuild().getIconUrl());
			embed.setFooter(String.format("User ID: %s", event.getMember().getId()));
			
			if(event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
				event.getGuild().retrieveAuditLogs().type(ActionType.MEMBER_UPDATE).queue(logs -> {
					AuditLogEntry entry = logs.stream()
						.filter(e -> e.getChangeByKey(AuditLogKey.MEMBER_MUTE) != null)
						.filter(e -> e.getTargetIdLong() == event.getMember().getIdLong())
						.findFirst()
						.orElse(null);
					
					if(entry != null) {
						embed.appendDescription(String.format(" by **%s**", entry.getUser().getAsTag()));
					}
					
					LoggerClient.get().queue(event.getGuild(), LoggerType.VOICE_MUTE, embed.build());
				});
			}else{
				LoggerClient.get().queue(event.getGuild(), LoggerType.VOICE_MUTE, embed.build());
			}
		});
	}
	
	public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
		Guild guild = event.getEntity().getGuild();
		
		Document logger = LoggerClient.get().getLogger(guild, LoggerType.VOICE_MEMBER_CHANGE_CHANNEL);
		if(logger == null) {
			return;
		}
		
		this.delay(() -> {
			WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
			
			if(event.getChannelJoined() == null) {
				embed.setDescription(String.format("**%s** has left `%s`", event.getEntity().getUser().getAsTag(), event.getChannelLeft().getName()));
				embed.setColor(LoggerListener.COLOR_RED);
			}else if(event.getChannelLeft() == null) {
				embed.setDescription(String.format("**%s** has joined `%s`", event.getEntity().getUser().getAsTag(), event.getChannelJoined().getName()));
				embed.setColor(LoggerListener.COLOR_GREEN);
			}else{
				embed.setDescription(String.format("**%s** has moved from `%s` to `%s`", event.getEntity().getUser().getAsTag(), event.getChannelLeft().getName(), event.getChannelJoined().getName()));
				embed.setColor(LoggerListener.COLOR_ORANGE);
			}
			
			embed.setTimestamp(ZonedDateTime.now());
			embed.setAuthor(guild.getName(), guild.getIconUrl());
			embed.setFooter(String.format("User ID: %s", event.getEntity().getId()));
			
			LoggerClient.get().queue(guild, LoggerType.VOICE_MEMBER_CHANGE_CHANNEL, embed.build());
		});
	}
}