package com.jsb.bot.command;

import java.util.ArrayList;
import java.util.List;

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
import net.dv8tion.jda.api.entities.TextChannel;

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
	
	public enum ModlogAction {
		BAN("Ban"),
		UNBAN("Unban"),
		KICK("Kick"),
		MUTE("Mute"),
		WARN("Warm"),
		VOICE_KICK("Voice Kick");
		
		private String name;
		
		private ModlogAction(String name) {
			this.name = name;
		}
		
		public String getName() {
			return this.name;
		}
	}
	
	@Command(value="toggle action", aliases={"toggleaction"}, description="Enable/disable whether an action should be logged or not")
	@AuthorPermissions({Permission.MANAGE_SERVER})
	public void toggleAction(CommandEvent event, @Argument(value="action") String actionArgument) {
		ModlogAction action;
		try {
			action = ModlogAction.valueOf(actionArgument.toUpperCase());
		} catch(IllegalArgumentException e) {
			event.reply("I could not find that action, valid actions are `" + MiscUtility.join(List.of(ModlogAction.values()), "`, `") + "` :no_entry:").queue();
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
	
}
