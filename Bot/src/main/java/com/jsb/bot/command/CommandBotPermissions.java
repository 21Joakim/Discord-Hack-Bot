package com.jsb.bot.command;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.bson.Document;

import com.jockie.bot.core.argument.Argument;
import com.jockie.bot.core.command.Command;
import com.jockie.bot.core.command.Command.AuthorPermissions;
import com.jockie.bot.core.command.impl.CommandEvent;
import com.jockie.bot.core.command.impl.CommandImpl;
import com.jsb.bot.database.Database;
import com.jsb.bot.utility.ArgumentUtility;
import com.jsb.bot.utility.MiscUtility;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Updates;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

public class CommandBotPermissions extends CommandImpl {

	public CommandBotPermissions() {
		super("bot permissions");
		
		super.setAliases("botpermissions", "bot perms", "botperms");
		super.setDescription("Add permissions to roles/users which only apply on the bot");
	}
	
	@Command(value="add", description="Add permission(s) to a user/role which only applies on the bot")
	@AuthorPermissions({Permission.ADMINISTRATOR})
	public void add(CommandEvent event, @Argument(value="user | role") String argument, @Argument(value="permissions") String[] permissionNames) {
		EnumSet<Permission> permissions = EnumSet.noneOf(Permission.class);
		for (String permissionName : permissionNames) {
			try {
				permissions.add(Permission.valueOf(permissionName.toUpperCase()));
			} catch(IllegalArgumentException e) {
				event.reply("`" + permissionName.toUpperCase() + "` is not a valid permission :no_entry:").queue();
				return;
			}
		}
		
		long rawPermissions = Permission.getRaw(permissions);
		
		Role role = ArgumentUtility.getRole(event.getGuild(), argument);
		Member member = ArgumentUtility.getMember(event.getGuild(), argument);
		if (role == null && member == null) {
			event.reply("I could not find that user/role :no_entry:").queue();
			return;
		}
		
		Database.get().getGuildById(event.getGuild().getIdLong(), null, Projections.include("botPermissions.roles", "botPermissions.users"), (data, readException) -> {
			if (readException != null) {
				readException.printStackTrace();
				
				event.reply("Something went wrong :no_entry:").queue();
			} else {
				if (role != null) {
					List<Document> roles = data.getEmbedded(List.of("botPermissions", "roles"), new ArrayList<>());
					for (Document roleData : roles) {
						if (roleData.getLong("id") == role.getIdLong()) {
							long oldRawPermissions = roleData.getLong("permissions");
							long newRawPermissions = oldRawPermissions | rawPermissions; 
							
							if (oldRawPermissions == newRawPermissions) {
								event.reply("That role already has all of those permissions :no_entry:").queue();
								return;
							}
							
							roles.remove(roleData);
							roleData.put("permissions", newRawPermissions);
							roles.add(roleData);
							
							Database.get().updateGuildById(event.getGuild().getIdLong(), Updates.set("botPermissions.roles", roles), (result, writeException) -> {
								if (writeException != null) {
									writeException.printStackTrace();
									
									event.reply("Something went wrong :no_entry:").queue();
								} else {
									event.reply("`" + role.getName() + "` can now use commands which require `" + MiscUtility.join(Permission.getPermissions(newRawPermissions - oldRawPermissions), "`, `") + "`").queue();
								}
							});
							
							return;
						}
					}

					roles.add(new Document()
							.append("id", role.getIdLong())
							.append("permissions", rawPermissions));
					
					Database.get().updateGuildById(event.getGuild().getIdLong(), Updates.set("botPermissions.roles", roles), (result, writeException) -> {
						if (writeException != null) {
							writeException.printStackTrace();
							
							event.reply("Something went wrong :no_entry:").queue();
						} else {
							event.reply("`" + role.getName() + "` can now use commands which require `" + MiscUtility.join(permissions, "`, `") + "`").queue();
						}
					});
				} else if (member != null) {
					List<Document> users = data.getEmbedded(List.of("botPermissions", "users"), new ArrayList<>());
					for (Document userData : users) {
						if (userData.getLong("id") == member.getIdLong()) {
							long oldRawPermissions = userData.getLong("permissions");
							long newRawPermissions = oldRawPermissions | rawPermissions; 
							
							if (oldRawPermissions == newRawPermissions) {
								event.reply("That role already has all of those permissions :no_entry:").queue();
								return;
							}
							
							users.remove(userData);
							userData.put("permissions", newRawPermissions);
							users.add(userData);
							
							Database.get().updateGuildById(event.getGuild().getIdLong(), Updates.set("botPermissions.users", users), (result, writeException) -> {
								if (writeException != null) {
									writeException.printStackTrace();
									
									event.reply("Something went wrong :no_entry:").queue();
								} else {
									event.reply("**" + member.getUser().getAsTag() + "** can now use commands which require `" + MiscUtility.join(Permission.getPermissions(newRawPermissions - oldRawPermissions), "`, `") + "`").queue();
								}
							});
							
							return;
						}
					}
					
					users.add(new Document()
							.append("id", member.getIdLong())
							.append("permissions", rawPermissions));
					
					Database.get().updateGuildById(event.getGuild().getIdLong(), Updates.set("botPermissions.users", users), (result, writeException) -> {
						if (writeException != null) {
							writeException.printStackTrace();
							
							event.reply("Something went wrong :no_entry:").queue();
						} else {
							event.reply("**" + member.getUser().getAsTag() + "** can now use commands which require `" + MiscUtility.join(permissions, "`, `") + "`").queue();
						}
					});
				}
			}
		});
	}
	
	@Command(value="remove", description="Remove bot permissions from a user or role")
	@AuthorPermissions({Permission.ADMINISTRATOR})
	public void remove(CommandEvent event, @Argument(value="user | role") String argument, @Argument(value="permissions") String[] permissionNames) {
		EnumSet<Permission> permissions = EnumSet.noneOf(Permission.class);
		for (String permissionName : permissionNames) {
			try {
				permissions.add(Permission.valueOf(permissionName.toUpperCase()));
			} catch(IllegalArgumentException e) {
				event.reply("`" + permissionName.toUpperCase() + "` is not a valid permission :no_entry:").queue();
				return;
			}
		}
		
		Role role = ArgumentUtility.getRole(event.getGuild(), argument);
		Member member = ArgumentUtility.getMember(event.getGuild(), argument);
		if (role == null && member == null) {
			event.reply("I could not find that user/role :no_entry:").queue();
			return;
		}
		
		Database.get().getGuildById(event.getGuild().getIdLong(), null, Projections.include("botPermissions.roles", "botPermissions.users"), (data, readException) -> {
			if (readException != null) {
				readException.printStackTrace();
				
				event.reply("Something went wrong :no_entry:").queue();
			} else {
				if (role != null) {
					List<Document> roles = data.getEmbedded(List.of("botPermissions", "roles"), new ArrayList<>());
					for (Document roleData : roles) {
						if (roleData.getLong("id") == role.getIdLong()) {
							long roleRawPermissions = roleData.getLong("permissions");
							EnumSet<Permission> rolePermissions = Permission.getPermissions(roleRawPermissions);
							long rawPermissionsToRemove = 0;
							for (Permission permission : permissions) {
								if (rolePermissions.contains(permission)) {
									rawPermissionsToRemove |= permission.getRawValue();
								} else {
									event.reply("That role does not have the permission `" + permission.toString() + "` :no_entry:").queue();
									return;
								}
							}
							
							if (rawPermissionsToRemove == 0) {
								event.reply("That role does not have the permission" + (permissions.size() == 1 ? "" : "s") + "`" + MiscUtility.join(permissions, "`, `") + "` :no_entry:").queue();
								return;
							}
							
							EnumSet<Permission> permissionsToRemove = Permission.getPermissions(rawPermissionsToRemove);
							
							long newRawPermissions = roleRawPermissions - rawPermissionsToRemove;
							roles.remove(roleData);
							if (newRawPermissions != 0) {
								roleData.put("permissions", newRawPermissions);
								roles.add(roleData);
							}
							
							Database.get().updateGuildById(event.getGuild().getIdLong(), Updates.set("botPermissions.roles", roles), (result, writeException) -> {
								if (writeException != null) {
									writeException.printStackTrace();
									
									event.reply("Something went wrong :no_entry:").queue();
								} else {
									event.reply("`" + role.getName() + "` can no longer use commands which require `" + MiscUtility.join(permissionsToRemove, "`, `") + "`").queue();
								}
							});
							
							return;
						}
					}
					
					event.reply("That role does not have the permission" + (permissions.size() == 1 ? "" : "s") + "`" + MiscUtility.join(permissions, "`, `") + "` :no_entry:").queue();
				} else if (member != null) {
					List<Document> users = data.getEmbedded(List.of("botPermissions", "users"), new ArrayList<>());
					for (Document userData : users) {
						if (userData.getLong("id") == member.getIdLong()) {
							long roleRawPermissions = userData.getLong("permissions");
							EnumSet<Permission> rolePermissions = Permission.getPermissions(roleRawPermissions);
							long rawPermissionsToRemove = 0;
							for (Permission permission : permissions) {
								if (rolePermissions.contains(permission)) {
									rawPermissionsToRemove |= permission.getRawValue();
								} else {
									event.reply("That user does not have the permission `" + permission.toString() + "` ").queue();
									return;
								}
							}
							
							if (rawPermissionsToRemove == 0) {
								event.reply("That user does not have the permission " + (permissions.size() == 1 ? "" : "s") + "`" + MiscUtility.join(permissions, "`, `") + "` :no_entry:").queue();
								return;
							}
							
							EnumSet<Permission> permissionsToRemove = Permission.getPermissions(rawPermissionsToRemove);
							
							long newRawPermissions = roleRawPermissions - rawPermissionsToRemove;
							users.remove(userData);
							if (newRawPermissions != 0) {
								userData.put("permissions", newRawPermissions);
								users.add(userData);
							}
							
							Database.get().updateGuildById(event.getGuild().getIdLong(), Updates.set("botPermissions.users", users), (result, writeException) -> {
								if (writeException != null) {
									writeException.printStackTrace();
									
									event.reply("Something went wrong :no_entry:").queue();
								} else {
									event.reply("**" + member.getUser().getAsTag() + "** can no longer use commands which require `" + MiscUtility.join(permissionsToRemove, "`, `") + "`").queue();
								}
							});
							
							return;
						}
					}
					
					event.reply("That user does not have the permission" + (permissions.size() == 1 ? "" : "s") + "`" + MiscUtility.join(permissions, "`, `") + "` :no_entry:").queue();
				}
			}
		});
	}
	
}
