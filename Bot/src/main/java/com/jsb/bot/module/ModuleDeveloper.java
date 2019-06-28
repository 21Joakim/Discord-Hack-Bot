package com.jsb.bot.module;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.jockie.bot.core.category.impl.CategoryImpl;
import com.jockie.bot.core.command.Command;
import com.jockie.bot.core.command.Command.Developer;
import com.jockie.bot.core.command.ICommand;
import com.jockie.bot.core.command.impl.CommandEvent;
import com.jockie.bot.core.command.impl.CommandListener;
import com.jockie.bot.core.module.Module;
import com.jsb.bot.category.Category;

import net.dv8tion.jda.api.Permission;

@Module
public class ModuleDeveloper {
	
	public static JSONArray getAllCommandsAsJson(CommandListener listener) {
		JSONArray commands = new JSONArray();
		for (CategoryImpl category : Category.ALL) {
			commands.put(ModuleDeveloper.getCategoryAsJson(category));
		}
		
		return commands;
	}
	
	public static JSONObject getCategoryAsJson(CategoryImpl category) {
		JSONArray commands = new JSONArray();
		for (ICommand command : category.getCommands()) {
			commands.put(ModuleDeveloper.getCommandAsJson(command, true));
		}
		
		return new JSONObject()
			.put("category", category.getName())
			.put("commands", commands);
	}
	
	public static JSONObject getCommandAsJson(ICommand command, boolean useCommandTrigger) {
		List<JSONObject> subCommands = new ArrayList<>();
		for (ICommand subCommand : command.getSubCommands()) {
			subCommands.add(ModuleDeveloper.getCommandAsJson(subCommand, false));
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
		JSONObject data = new JSONObject().put("categories", ModuleDeveloper.getAllCommandsAsJson(event.getCommandListener()));
		
		event.replyFile(data.toString().getBytes(StandardCharsets.UTF_8), "commands.json").queue();
	}
}