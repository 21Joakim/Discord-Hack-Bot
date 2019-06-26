package com.jsb.bot.mute;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import org.bson.Document;

import com.jsb.bot.database.Database;
import com.jsb.bot.database.callback.Callback;
import com.jsb.bot.modlog.Action;
import com.jsb.bot.modlog.ModlogListener;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Updates;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.sharding.ShardManager;

public class MuteListener extends ListenerAdapter {
	
	public static final String MUTE_ROLE_NAME = "Muted";
	
	public static ScheduledExecutorService scheduledExector = Executors.newSingleThreadScheduledExecutor();
	
	private static Map<Long, Map<Long, ScheduledFuture<?>>> executors = new HashMap<>();

	public static void getOrCreateMuteRole(Guild guild, Callback<Role> callback) {
		Database.get().getGuildById(guild.getIdLong(), null, Projections.include("mute.role"), (data, readException) -> {
			if (readException != null) {
				readException.printStackTrace();
			} else {
				long muteRoleId = data.getEmbedded(List.of("mute", "role"), 0L);
				Role role = guild.getRoleById(muteRoleId);
				if (role == null) {
					if (guild.getRoles().size() >= 250) {
						callback.onResult(null, new Throwable("I could not create the mute role because the server has the max amount of role (250)"));
					} else {
						guild.createRole().setName(MuteListener.MUTE_ROLE_NAME).queue(muteRole -> {
							Database.get().updateGuildById(guild.getIdLong(), Updates.set("mute.role", muteRole.getIdLong()), (result, writeException) -> {
								if (writeException != null) {
									writeException.printStackTrace();
									
									callback.onResult(null, writeException);
								} else {
									for (TextChannel channel : guild.getTextChannels()) {
										channel.putPermissionOverride(muteRole).setDeny(Permission.MESSAGE_WRITE).queue();
									}
									
									callback.onResult(muteRole, null);
								}
							});
						});
					}
				} else {
					callback.onResult(role, null);
				}
			}
		});
	}
	
	public static void unmuteUser(ShardManager shardManager, long guildId, long memberId, long muteRoleId) {
		Guild guild = shardManager.getGuildById(guildId);
		if (guild != null) {
			Member member = guild.getMemberById(memberId);
			Role role = guild.getRoleById(muteRoleId);
			
			Database.get().getGuildById(guildId, null, Projections.include("mute.users"), (data, readException) -> {
				if (readException != null) {
					readException.printStackTrace();
				} else {
					List<Document> users = data.getEmbedded(List.of("mute", "users"), new ArrayList<>());
					
					for (Document user : users) {
						if (user.getLong("id") == memberId) {
							users.remove(user);
							break;
						}
					}
					
					Database.get().updateGuildById(guildId, Updates.set("mute.users", users), (result, writeException) -> {
						if (writeException != null) {
							writeException.printStackTrace();
						} else {
							if (role != null && member != null) {
								if (member.getRoles().contains(role) && guild.getSelfMember().canInteract(role)) {
									guild.removeRoleFromMember(member, role).queue();
								}
							}
							
							if (member != null) {
								ModlogListener.createModlog(guild, guild.getJDA().getSelfUser(), member.getUser(), "Time limit served", true, Action.UNMUTE);
							}
							
							MuteListener.removeExecutor(guildId, memberId);
						}
					});
				}
			});
		}
	}
	
	public static Map<Long, Map<Long, ScheduledFuture<?>>> getExecutors() {
		return executors;
	}
	
	public static void putExecutor(long guildId, long userId, ScheduledFuture<?> executor) {
		Map<Long, ScheduledFuture<?>> userExecutors = executors.containsKey(guildId) ? executors.get(guildId) : new HashMap<>();
		userExecutors.put(userId, executor);
		executors.put(guildId, userExecutors);
	}
	
	public static void removeExecutor(long guildId, long userId) {
		if (executors.containsKey(guildId)) {
			Map<Long, ScheduledFuture<?>> userExecutors = executors.get(guildId);
			if (userExecutors.containsKey(userId)) {
				ScheduledFuture<?> executor = userExecutors.get(userId);
				if (!executor.isDone()) {
					executor.cancel(false);
				}
				
				userExecutors.remove(userId);
				executors.put(guildId, userExecutors);
			}
		}
	}
	
	public void onGuildMemberLeave(GuildMemberLeaveEvent event) {
		Database.get().getGuildById(event.getGuild().getIdLong(), null, Projections.include("mute.users", "mute.action"), (data, readException) -> {
			if (readException != null) {
				readException.printStackTrace();
			} else {
				Member selfMember = event.getGuild().getSelfMember();
				
				Document muteData = data.get("mute", Document.class);
				Document action = data.get("action", Document.class);
				
				List<Document> users = muteData.getList("users", Document.class);
				for (Document user : users) {
					if (user.getLong("id") == event.getMember().getIdLong()) {
						String actionMessage = action.getString("message");
						String actionTypeString = action.getString("type");
							
						MuteEvasionType actionType = actionTypeString == null ? MuteEvasionType.REMUTE_ON_JOIN : MuteEvasionType.valueOf(actionTypeString);
						if (actionType.equals(MuteEvasionType.BAN_ON_LEAVE)) {
							if (selfMember.hasPermission(Permission.BAN_MEMBERS)) {
								event.getGuild().ban(event.getMember(), 1).reason(actionMessage == null ? "Mute Evasion" : actionMessage).queue($ -> {
										ModlogListener.createModlog(event.getGuild(), event.getJDA().getSelfUser(), event.getMember().getUser(), actionMessage == null ? "Mute Evasion" : actionMessage, true, Action.BAN);
								});
							}
						}
						
						return;
					}
				}
			}
		});
	}
	
	public void onGuildMemberJoin(GuildMemberJoinEvent event) {
		Database.get().getGuildById(event.getGuild().getIdLong(), null, Projections.include("mute.users", "mute.role", "mute.action"), (data, readException) -> {
			if (readException != null) {
				readException.printStackTrace();
			} else {
				Member selfMember = event.getGuild().getSelfMember();
				
				Document muteData = data.get("mute", Document.class);
				Document action = data.get("action", Document.class);
				
				List<Document> users = muteData.getList("users", Document.class);
				for (Document user : users) {
					if (user.getLong("id") == event.getMember().getIdLong()) {
						if (user.getLong("length") + user.getLong("time") > Clock.systemUTC().instant().getEpochSecond()) {
							String actionMessage = action.getString("message");
							String actionTypeString = action.getString("type");
							
							MuteEvasionType actionType = actionTypeString == null ? MuteEvasionType.REMUTE_ON_JOIN : MuteEvasionType.valueOf(actionTypeString);
							if (actionType.equals(MuteEvasionType.BAN_ON_JOIN)) {
								if (selfMember.hasPermission(Permission.BAN_MEMBERS)) {
									event.getGuild().ban(event.getMember(), 1).reason(actionMessage == null ? "Mute Evasion" : actionMessage).queue($ -> {
										ModlogListener.createModlog(event.getGuild(), event.getJDA().getSelfUser(), event.getMember().getUser(), actionMessage == null ? "Mute Evasion" : actionMessage, true, Action.BAN);
									});
								}
							} else if (actionType.equals(MuteEvasionType.KICK_ON_JOIN)) {
								if (selfMember.hasPermission(Permission.KICK_MEMBERS)) {
									event.getGuild().kick(event.getMember()).reason(actionMessage == null ? "Mute Evasion" : actionMessage).queue($ -> {
										ModlogListener.createModlog(event.getGuild(), event.getJDA().getSelfUser(), event.getMember().getUser(), actionMessage == null ? "Mute Evasion" : actionMessage, true, Action.KICK);
									});
								}
							} else if (actionType.equals(MuteEvasionType.REMUTE_ON_JOIN)) {
								Role muteRole = event.getGuild().getRoleById(muteData.getLong("role"));
								if (muteRole != null && selfMember.hasPermission(Permission.MANAGE_ROLES)) {
									event.getGuild().addRoleToMember(event.getMember(), muteRole).reason(actionMessage == null ? "Mute Evasion" : actionMessage).queue();
								}
							} else if (actionType.equals(MuteEvasionType.WARN_ON_JOIN)) {
								//warn user
							}
						} else {
							users.remove(user);
							
							Database.get().updateGuildById(event.getGuild().getIdLong(), Updates.set("mute.users", users), (result, writeException) -> {
								if (writeException != null) {
									writeException.printStackTrace();
								} else {
									MuteListener.removeExecutor(event.getGuild().getIdLong(), event.getMember().getIdLong());
									
									ModlogListener.createModlog(event.getGuild(), event.getJDA().getSelfUser(), event.getMember().getUser(), "Time limit served", true, Action.UNMUTE);
								}
							});
						}
						
						return;
					}
				}
			}
		});
	}
	
}
