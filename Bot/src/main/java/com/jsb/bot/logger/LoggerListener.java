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
import net.dv8tion.jda.api.entities.Guild.MFALevel;
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
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
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
			embed.setDescription(String.format("The server ownership has been transferred from %s to %s", event.getOldOwner().getUser().getAsMention(), event.getNewOwner().getUser().getAsMention()));
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
	
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		Document logger = LoggerClient.get().getLogger(event.getGuild(), LoggerType.MESSAGE_RECEIVE);
		if(logger == null) {
			return;
		}
		
		this.delay(() -> {
			WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
			embed.setDescription(String.format("A message was sent by **%s**", event.getAuthor().getAsTag()));
			embed.setColor(LoggerListener.COLOR_GREEN);
			embed.setTimestamp(ZonedDateTime.now());
			embed.setAuthor(event.getMember().getEffectiveName(), event.getAuthor().getEffectiveAvatarUrl());
			embed.setFooter(String.format("User ID: %s", event.getAuthor().getId()));
			embed.addField("Content", event.getMessage().getContentRaw(), true);
			
			LoggerClient.get().queue(event.getGuild(), LoggerType.MESSAGE_RECEIVE, embed.build());
		});
	}
}