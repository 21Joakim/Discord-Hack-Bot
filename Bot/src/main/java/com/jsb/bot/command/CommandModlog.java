package com.jsb.bot.command;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bson.Document;

import com.jockie.bot.core.argument.Argument;
import com.jockie.bot.core.command.Command;
import com.jockie.bot.core.command.Command.AuthorPermissions;
import com.jockie.bot.core.command.Command.BotPermissions;
import com.jockie.bot.core.command.impl.CommandEvent;
import com.jockie.bot.core.command.impl.CommandImpl;
import com.jsb.bot.database.Database;
import com.jsb.bot.modlog.Action;
import com.jsb.bot.utility.ArgumentUtility;
import com.jsb.bot.utility.MiscUtility;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Updates;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageEmbed.Field;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

public class CommandModlog extends CommandImpl {

	public CommandModlog() {
		super("modlog");
		
		super.setDescription("Set up modlogs in the server to easily log all mod actions which occur in the server");
	}
	
	@Command(value="toggle", description="Enable/disable modlogs in the server depending on its current state")
	@AuthorPermissions({Permission.MANAGE_SERVER})
	public void toggle(CommandEvent event) {
		Database.get().getGuildById(event.getGuild().getIdLong(), null, Projections.include("modlog.enabled"), (data, readException) -> {
			if (readException != null) {
				readException.printStackTrace();
				
				event.reply("Something went wrong :no_entry:").queue();
			} else {
				boolean enabled = data.getEmbedded(List.of("modlog", "enabled"), false);
				
				Database.get().updateGuildById(event.getGuild().getIdLong(), Updates.set("modlog.enabled", !enabled), (result, writeException) -> {
					if (writeException != null) {
						writeException.printStackTrace();
						
						event.reply("Something went wrong :no_entry:").queue();
					} else {
						event.reply("Modlogs are now " + (enabled ? "disabled" : "enabled")).queue();
					}
				});
			}
		});
	}
	
	@Command(value="channel", description="Set the modlog channel for the server")
	@AuthorPermissions({Permission.MANAGE_SERVER})
	public void channel(CommandEvent event, @Argument(value="channel", endless=true) String channelArgument) {
		TextChannel channel = ArgumentUtility.getTextChannel(event.getGuild(), channelArgument);
		if (channel == null) {
			event.reply("I could not find that channel :no_entry:").queue();
			return;
		}
		
		Database.get().getGuildById(event.getGuild().getIdLong(), null, Projections.include("modlog.channel"), (data, readException) -> {
			if (readException != null) {
				readException.printStackTrace();
				
				event.reply("Something went wrong :no_entry:").queue();
			} else {
				long channelId = data.getEmbedded(List.of("modlog", "channel"), -1L);
				if (channelId == channel.getIdLong()) {
					event.reply("The modlog channel is already set to " + channel.getAsMention() + " :no_entry:").queue();
					return;
				}
				
				Database.get().updateGuildById(event.getGuild().getIdLong(), Updates.set("modlog.channel", channel.getIdLong()), (result, writeException) -> {
					if (writeException != null) {
						writeException.printStackTrace();
						
						event.reply("Something went wrong :no_entry:").queue();
					} else {
						event.reply("The modlog channel has been set to " + channel.getAsMention()).queue();
					}
				});
			}
		});
	}
	
	@Command(value="toggle action", aliases={"toggleaction"}, description="Enable/disable whether an action should be logged or not")
	@AuthorPermissions({Permission.MANAGE_SERVER})
	public void toggleAction(CommandEvent event, @Argument(value="action") String actionArgument) {
		Action action;
		try {
			action = Action.valueOf(actionArgument.toUpperCase());
		} catch(IllegalArgumentException e) {
			event.reply("I could not find that action, valid actions are `" + MiscUtility.join(List.of(Action.values()), "`, `") + "` :no_entry:").queue();
			return;
		}
		
		Database.get().getGuildById(event.getGuild().getIdLong(), null, Projections.include("modlog.disabledActions"), (data, readException) -> {
			if (readException != null) {
				readException.printStackTrace();
				
				event.reply("Something went wrong :no_entry:").queue();
			} else {
				List<String> disabledActions = data.getEmbedded(List.of("modlog", "disabledActions"), new ArrayList<>());
				boolean contains = disabledActions.contains(action.toString());
				if (contains) {
					disabledActions.remove(action.toString());
				} else {
					disabledActions.add(action.toString());
				}				
				
				Database.get().updateGuildById(event.getGuild().getIdLong(), Updates.set("modlog.disabledActions", disabledActions), (result, writeException) -> {
					if (writeException != null) {
						writeException.printStackTrace();
						
						event.reply("Something went wrong :no_entry:").queue();
					} else {
						event.reply("The action `" + action.toString() + "` is now " + (contains ? "enabled" : "disabled")).queue();
					}
				});
			}
		});
	}
	
	@Command(value="edit", description="Edit a reasoning of a modlog you own")
	public void edit(CommandEvent event, @Argument(value="case id") int caseId, @Argument(value="reason", endless=true) String reason) {
		Database.get().getModlogCase(event.getGuild().getIdLong(), caseId, (data, readException) -> {
			if (readException != null) {
				readException.printStackTrace();
				
				event.reply("Something went wrong :no_entry:").queue();
			} else {
				if (data == null) {
					event.reply("I could not find that modlog case :no_entry:").queue();
					return;
				}
				
				String currentReason = data.getString("reason");
				Long moderatorId = data.getLong("moderatorId");
				Long messageId = data.getLong("messageId");
				Long channelId = data.getLong("channelId");
				
				if (moderatorId == null) {
					if (!event.getMember().hasPermission(Permission.MANAGE_SERVER)) {
						event.reply("You need the `Manage Server` permission to edit an unowned modlog :no_entry:").queue();
						return;
					}
				} else {
					if (moderatorId != event.getAuthor().getIdLong()) {
						event.reply("You cannot edit a modlog which you do not own :no_entry:").queue();
						return;
					}
				}
				
				if (currentReason != null && reason.equals(currentReason)) {
					event.reply("The reason is already set to that :no_entry:").queue();
					return;
				}
				
				if (channelId != null && messageId != null) {
					TextChannel channel = event.getGuild().getTextChannelById(channelId);
					if (channel != null) {
						channel.retrieveMessageById(messageId).queue(message -> {
							MessageEmbed embed = message.getEmbeds().get(0);
							
							EmbedBuilder newEmbed = new EmbedBuilder();
							newEmbed.setTitle(embed.getTitle());
							newEmbed.setTimestamp(embed.getTimestamp());
							
							for (Field field : embed.getFields()) {
								if (field.getName().equals("Reason")) {
									newEmbed.addField(field.getName(), reason, field.isInline());
								} else {
									newEmbed.addField(field);
								}
							}
							
							message.editMessage(newEmbed.build()).queue();
						}, e -> {});
					}
				}
				
				Database.get().updateModlogCase(data.getObjectId("_id"), Updates.set("reason", reason), (result, writeException) -> {
					if (writeException != null) {
						writeException.printStackTrace();
						
						event.reply("Something went wrong :no_entry:").queue();
					} else {
						event.reply("Case **" + data.getLong("id") + "** has been updated").queue();
					}
				});
			}
		});
	}
	
	@Command(value="remove", aliases={"delete"}, description="Delete a modlog case")
	@AuthorPermissions({Permission.ADMINISTRATOR})
	public void remove(CommandEvent event, @Argument(value="case id") int caseId) {
		Database.get().getModlogCase(event.getGuild().getIdLong(), caseId, (data, readException) -> {
			if (readException != null) {
				readException.printStackTrace();
				
				event.reply("Something went wrong :no_entry:").queue();
			} else {
				if (data == null) {
					event.reply("I could not find that modlog case :no_entry:").queue();
					return;
				}
				
				Long messageId = data.getLong("messageId");
				Long channelId = data.getLong("channelId");
				
				if (channelId != null && messageId != null) {
					TextChannel channel = event.getGuild().getTextChannelById(channelId);
					if (channel != null) {
						channel.retrieveMessageById(messageId).queue(message -> {
							message.delete().queue();
						}, e -> {});
					}
				}
				
				Database.get().deleteModlogCase(event.getGuild().getIdLong(), caseId, (result, writeException) -> {
					if (writeException != null) {
						writeException.printStackTrace();
						
						event.reply("Something went wrong :no_entry:").queue();
					} else {
						event.reply("Case **" + caseId + "** has been deleted").queue();
					}
				});
			}
		});
	}
	
	@Command(value="view", description="View a modlog case in the current channel")
	@AuthorPermissions({Permission.MANAGE_SERVER})
	public void view(CommandEvent event, @Argument(value="case id") int caseId) {
		Database.get().getModlogCase(event.getGuild().getIdLong(), caseId, (data, readException) -> {
			if (readException != null) {
				readException.printStackTrace();
				
				event.reply("Something went wrong :no_entry:").queue();
			} else {
				if (data == null) {
					event.reply("I could not find that modlog case :no_entry:").queue();
					return;
				}
				
				Long moderatorId = data.getLong("moderatorId");
				User moderator = null;
				if (moderatorId != null) {
					moderator = event.getShardManager().getUserById(moderatorId);
				}
				
				long userId = data.getLong("userId");
				User user = event.getShardManager().getUserById(userId);
				
				long createdAt = data.getLong("createdAt");
				String reason = data.getString("reason");
				Action action = Action.valueOf(data.getString("action"));
				
				EmbedBuilder embed = new EmbedBuilder();
				embed.setTitle("Case " + caseId + " | " + action.getName());
				embed.setTimestamp(Instant.ofEpochSecond(createdAt));
				embed.addField("Moderator", moderator == null ? moderatorId == null ? "Unknown" : "Unknown user (" + moderatorId + ")" : moderator.getAsTag() + " (" + moderatorId + ")", false);
				embed.addField("User", user == null ? "Unknown user (" + userId + ")" : user.getAsTag() + " (" + userId + ")", false);
				embed.addField("Reason", reason == null ? "None Given" : reason, false);
				
				event.reply(embed.build()).queue();
			}
		});
	}
	
	@Command(value="settings", description="View the servers current modlog settings")
	@BotPermissions({Permission.MESSAGE_EMBED_LINKS})
	public void settings(CommandEvent event) {
		Database.get().getGuildById(event.getGuild().getIdLong(), null, Projections.include("modlog.enabled", "modlog.disabledActions", "modlog.channel"), (data, readException) -> {
			if (readException != null) {
				readException.printStackTrace();
				
				event.reply("Something went wrong :no_entry:").queue();
			} else {
				Document document = data.getEmbedded(List.of("modlog"), new Document());
				List<String> disabledActions = document.getList("disabledActions", String.class, Collections.emptyList());
				boolean enabled = document.getBoolean("enabled", false);
				
				TextChannel channel = null;
				Long channelId = document.getLong("channel");
				if (channelId != null) {
					channel = event.getGuild().getTextChannelById(channelId);
				}
				
				EmbedBuilder embed = new EmbedBuilder();
				embed.setAuthor("Modlog Settings", null, event.getGuild().getIconUrl());
				embed.addField("Status", enabled ? "Enabled" : "Disabled", true);
				embed.addField("Channel", channel == null ? "Not Set" : channel.getAsMention(), true);
				
				if (disabledActions != null && !disabledActions.isEmpty()) {
					embed.addField("Disabled Actions", "`" + MiscUtility.join(disabledActions, "`\n`") + "`", false);
				}
				
				event.reply(embed.build()).queue();
			}
		});
	}
	
}
