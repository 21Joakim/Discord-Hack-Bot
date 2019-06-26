package com.jsb.bot.module;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import com.jockie.bot.core.command.Command;
import com.jockie.bot.core.command.Command.Developer;
import com.jockie.bot.core.command.ICommand;
import com.jockie.bot.core.command.impl.CommandEvent;
import com.jockie.bot.core.module.Module;

import net.dv8tion.jda.api.Permission;

@Module
public class ModuleDeveloper {
	
	private JSONObject getJsonDataFromCommand(ICommand command, boolean useCommandTrigger) {
		List<JSONObject> subCommands = new ArrayList<>();
		for (ICommand subCommand : command.getSubCommands()) {
			subCommands.add(this.getJsonDataFromCommand(subCommand, false));
		}
		
		return new JSONObject()
				.put("name", useCommandTrigger ? command.getCommandTrigger() : command.getCommand())
				.put("description", command.getDescription())
				.put("usage", command.getUsage())
				.put("aliases", command.getAliases())
				.put("subCommands", subCommands)
				.put("authorPermissions", Permission.getRaw(command.getAuthorDiscordPermissions()))
				.put("botPermissions", Permission.getRaw(command.getBotDiscordPermissions()));
	}

	@Command(value="json commands", description="Gives all the commands with their data in a json file")
	@Developer
	public void jsonCommands(CommandEvent event) {
		List<JSONObject> json = new ArrayList<>();
		for (ICommand command : event.getCommandListener().getAllCommands()) {
			json.add(this.getJsonDataFromCommand(command, true));
		}
		
		event.replyFile(new JSONObject().put("commands", json).toString().getBytes(StandardCharsets.UTF_8), "commands.json").queue();
	}
	
}
