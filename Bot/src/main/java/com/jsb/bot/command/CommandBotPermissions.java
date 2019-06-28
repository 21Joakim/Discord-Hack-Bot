package com.jsb.bot.command;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bson.Document;

import com.jockie.bot.core.argument.Argument;
import com.jockie.bot.core.command.Command;
import com.jockie.bot.core.command.Command.AuthorPermissions;
import com.jockie.bot.core.command.ICommand;
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
	
	public class CommandCommand extends CommandImpl {
		  
		public CommandCommand() {
			super("command");
			
			super.setDescription("Add/set what permissions a specific command should require");
		}
		
		@Command(value="set", description="Set what permissions a specific command should require")
		@AuthorPermissions({Permission.ADMINISTRATOR})
		public void set(CommandEvent event, @Argument(value="command") String commandArgument, @Argument(value="permissions") String[] permissionNames) {
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
			
			ICommand command = ArgumentUtility.getCommand(event.getCommandListener(), commandArgument);
			if (command == null) {
				event.reply("I could not find that command :no_entry:").queue();
				return;
			}
			
			Database.get().getGuildById(event.getGuild().getIdLong(), null, Projections.include("botPermissions.commands"), (data, readException) -> {
				if (readException != null) {
					readException.printStackTrace();
					
					event.reply("Something went wrong :no_entry:").queue();
				} else {
					List<Document> commands = data.getEmbedded(List.of("botPermissions", "commands"), new ArrayList<>());
					for (Document commandData : commands) {
						if (commandData.getString("id").equals(command.getCommandTrigger())) {
							commands.remove(commandData);
							commandData.put("permissions", rawPermissions);
							commands.add(commandData);
							
							Database.get().updateGuildById(event.getGuild().getIdLong(), Updates.set("botPermissions.commands", commands), (result, writeException) -> {
								if (writeException != null) {
									writeException.printStackTrace();
									
									event.reply("Something went wrong :no_entry:").queue();
								} else {
									event.reply("`" + command.getCommandTrigger() + "` now requires the permission" + (permissions.size() == 1 ? "" : "s") + " `" + MiscUtility.join(permissions, "`, `") + "` to be executed").queue();
								}
							});
							
							return;
						}
					}
					
					commands.add(new Document()
							.append("id", command.getCommandTrigger())
							.append("permissions", rawPermissions)
							.append("bypassUsers", new ArrayList<>())
							.append("bypassRoles", new ArrayList<>()));
					
					Database.get().updateGuildById(event.getGuild().getIdLong(), Updates.set("botPermissions.commands", commands), (result, writeException) -> {
						if (writeException != null) {
							writeException.printStackTrace();
							
							event.reply("Something went wrong :no_entry:").queue();
						} else {
							event.reply("`" + command.getCommandTrigger() + "` now requires the permission" + (permissions.size() == 1 ? "" : "s") + " `" + MiscUtility.join(permissions, "`, `") + "` to be executed").queue();
						}
					});
				}
			});
		}
		
		@Command(value="add", description="Add permissions which should be required when executing a command")
		@AuthorPermissions({Permission.ADMINISTRATOR})
		public void add(CommandEvent event, @Argument(value="command") String commandArgument, @Argument(value="permissions") String[] permissionNames) {
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
			
			ICommand command = ArgumentUtility.getCommand(event.getCommandListener(), commandArgument);
			if (command == null) {
				event.reply("I could not find that command :no_entry:").queue();
				return;
			}
			
			Database.get().getGuildById(event.getGuild().getIdLong(), null, Projections.include("botPermissions.commands"), (data, readException) -> {
				if (readException != null) {
					readException.printStackTrace();
					
					event.reply("Something went wrong :no_entry:").queue();
				} else {
					List<Document> commands = data.getEmbedded(List.of("botPermissions", "commands"), new ArrayList<>());
					for (Document commandData : commands) {
						if (commandData.getString("id").equals(command.getCommandTrigger())) {
							long oldRawPermissions = commandData.getLong("permissions");
							long newRawPermissions = oldRawPermissions | rawPermissions;
							if (oldRawPermissions == newRawPermissions) {
								event.reply("That command already requires all of those permissions :no_entry:").queue();
								return;
							}
							
							EnumSet<Permission> newPermissions = Permission.getPermissions(newRawPermissions - oldRawPermissions);
							
							commands.remove(commandData);
							commandData.put("permissions", newRawPermissions);
							commands.add(commandData);
							
							Database.get().updateGuildById(event.getGuild().getIdLong(), Updates.set("botPermissions.commands", commands), (result, writeException) -> {
								if (writeException != null) {
									writeException.printStackTrace();
									
									event.reply("Something went wrong :no_entry:").queue();
								} else {
									event.reply("`" + command.getCommandTrigger() + "` now requires the permission" + (newPermissions.size() == 1 ? "" : "s") + " `" + MiscUtility.join(newPermissions, "`, `") + "` to be executed").queue();
								}
							});
							
							return;
						}
					}
					
					List<Permission> requiredPermissions = command.getAuthorDiscordPermissions();
					long oldRawPermissions = Permission.getRaw(requiredPermissions);
					long newRawPermissions = oldRawPermissions | rawPermissions;
					if (oldRawPermissions == newRawPermissions) {
						event.reply("That command already requires all of those permissions :no_entry:").queue();
						return;
					}
					
					EnumSet<Permission> newPermissions = Permission.getPermissions(newRawPermissions - oldRawPermissions);
					
					commands.add(new Document()
							.append("id", command.getCommandTrigger())
							.append("permissions", newRawPermissions)
							.append("bypassUsers", new ArrayList<>())
							.append("bypassRoles", new ArrayList<>()));
					
					Database.get().updateGuildById(event.getGuild().getIdLong(), Updates.set("botPermissions.commands", commands), (result, writeException) -> {
						if (writeException != null) {
							writeException.printStackTrace();
							
							event.reply("Something went wrong :no_entry:").queue();
						} else {
							event.reply("`" + command.getCommandTrigger() + "` now requires the permission" + (newPermissions.size() == 1 ? "" : "s") + " `" + MiscUtility.join(newPermissions, "`, `") + "` to be executed").queue();
						}
					});
				}
			});
		}
		
		@Command(value="remove", description="Removes permissions required when executing a command")
		@AuthorPermissions({Permission.ADMINISTRATOR})
		public void remove(CommandEvent event, @Argument(value="command") String commandArgument, @Argument(value="permissions") String[] permissionNames) {
			EnumSet<Permission> permissions = EnumSet.noneOf(Permission.class);
			for (String permissionName : permissionNames) {
				try {
					permissions.add(Permission.valueOf(permissionName.toUpperCase()));
				} catch(IllegalArgumentException e) {
					event.reply("`" + permissionName.toUpperCase() + "` is not a valid permission :no_entry:").queue();
					return;
				}
			}
			
			ICommand command = ArgumentUtility.getCommand(event.getCommandListener(), commandArgument);
			if (command == null) {
				event.reply("I could not find that command :no_entry:").queue();
				return;
			}
			
			Database.get().getGuildById(event.getGuild().getIdLong(), null, Projections.include("botPermissions.commands"), (data, readException) -> {
				if (readException != null) {
					readException.printStackTrace();
					
					event.reply("Something went wrong :no_entry:").queue();
				} else {
					List<Document> commands = data.getEmbedded(List.of("botPermissions", "commands"), new ArrayList<>());
					for (Document commandData : commands) {
						if (commandData.getString("id").equals(command.getCommandTrigger())) {
							long commandRawPermissions = commandData.getLong("permissions");
							EnumSet<Permission> commandPermissions = Permission.getPermissions(commandRawPermissions);
							long rawPermissionsToRemove = 0;
							for (Permission permission : permissions) {
								if (commandPermissions.contains(permission)) {
									rawPermissionsToRemove |= permission.getRawValue();
								} else {
									event.reply("That command does not require the permission `" + permission.toString() + "` :no_entry:").queue();
									return;
								}
							}
							
							if (rawPermissionsToRemove == 0) {
								event.reply("That command does not require the permission" + (permissions.size() == 1 ? "" : "s") + "`" + MiscUtility.join(permissions, "`, `") + "` :no_entry:").queue();
								return;
							}
							
							EnumSet<Permission> permissionsToRemove = Permission.getPermissions(rawPermissionsToRemove);
							
							long newRawPermissions = commandRawPermissions - rawPermissionsToRemove;
							commands.remove(commandData);
							if (newRawPermissions != 0) {
								commandData.put("permissions", newRawPermissions);
								commands.add(commandData);
							}
							
							Database.get().updateGuildById(event.getGuild().getIdLong(), Updates.set("botPermissions.commands", commands), (result, writeException) -> {
								if (writeException != null) {
									writeException.printStackTrace();
									
									event.reply("Something went wrong :no_entry:").queue();
								} else {
									event.reply("`" + command.getCommandTrigger() + "` no longer requires the permission" + (permissionsToRemove.size() == 1 ? "" : "s") + " `" + MiscUtility.join(permissionsToRemove, "`, `") + "` to be executed").queue();
								}
							});
							
							return;
						}
					}
					
					event.reply("That command does not require the permission" + (permissions.size() == 1 ? "" : "s") + "`" + MiscUtility.join(permissions, "`, `") + "` :no_entry:").queue();
				}
			});
		}
		
		@Command(value="bypass", aliases={"whitelist"}, description="Toggle whether a user/role should bypass a command so they can use it under any circumstance")
		@AuthorPermissions({Permission.ADMINISTRATOR})
		public void bypass(CommandEvent event, @Argument(value="command") String commandArgument, @Argument(value="user | role", endless=true) String argument) {
			ICommand command = ArgumentUtility.getCommand(event.getCommandListener(), commandArgument);
			if (command == null) {
				event.reply("I could not find that command :no_entry:").queue();
				return;
			}
			
			Role role = ArgumentUtility.getRole(event.getGuild(), argument);
			Member member = ArgumentUtility.getMember(event.getGuild(), argument);
			if (role == null && member == null) {
				event.reply("I could not find that user/role :no_entry:").queue();
				return;
			}
			
			Database.get().getGuildById(event.getGuild().getIdLong(), null, Projections.include("botPermissions.commands"), (data, readException) -> {
				if (readException != null) {
					readException.printStackTrace();
					
					event.reply("Something went wrong :no_entry:").queue();
				} else {
					List<Document> commands = data.getEmbedded(List.of("botPermissions", "commands"), new ArrayList<>());
					for (Document commandData : commands) {
						if (commandData.getString("id").equals(command.getCommandTrigger())) {
							AtomicBoolean contains = new AtomicBoolean(false);
							if (role != null) {
								List<Long> roles = commandData.getList("bypassRoles", Long.class);
								contains.set(roles.contains(role.getIdLong()));
								if (contains.get()) {
									roles.remove(role.getIdLong());
								} else {
									roles.add(role.getIdLong());
								}
								
								commands.remove(commandData);
								commandData.put("bypassRoles", roles);
								commands.add(commandData);
							} else if (member != null) {
								List<Long> users = commandData.getList("bypassUsers", Long.class);
								contains.set(users.contains(member.getIdLong()));
								if (contains.get()) {
									users.remove(member.getIdLong());
								} else {
									users.add(member.getIdLong());
								}
								
								commands.remove(commandData);
								commandData.put("bypassUsers", users);
								commands.add(commandData);
							}
							
							Database.get().updateGuildById(event.getGuild().getIdLong(), Updates.set("botPermissions.commands", commands), (result, writeException) -> {
								if (writeException != null) {
									writeException.printStackTrace();
									
									event.reply("Something went wrong :no_entry:").queue();
								} else {
									event.reply((member != null ? "**" + member.getUser().getAsTag() + "**" : "`" + role.getName() + "`") + " can " + (contains.get() ? "no longer" : "now") + " use `" + command.getCommandTrigger() + "` under any circumstance").queue();
								}
							});
							
							return;
						}
					}
					
					if (role != null) {
						commands.add(new Document()
								.append("id", command.getCommandTrigger())
								.append("permissions", null)
								.append("bypassRoles", List.of(role.getIdLong()))
								.append("bypassUsers", new ArrayList<>()));
					} else if (member != null) {
						commands.add(new Document()
								.append("id", command.getCommandTrigger())
								.append("permissions", null)
								.append("bypassRoles", new ArrayList<>())
								.append("bypassUsers", List.of(member.getIdLong())));
					}
					
					Database.get().updateGuildById(event.getGuild().getIdLong(), Updates.set("botPermissions.commands", commands), (result, writeException) -> {
						if (writeException != null) {
							writeException.printStackTrace();
							
							event.reply("Something went wrong :no_entry:").queue();
						} else {
							event.reply((member != null ? "**" + member.getUser().getAsTag() + "**" : "`" + role.getName() + "`") + " can now use `" + command.getCommandTrigger() + "` under any circumstance").queue();
						}
					});
				}
			});
		}
		
	}
	
}
