package com.jsb.bot.listener;

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
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Updates;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.sharding.ShardManager;

public class MuteListener {
	
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
	
}
