package com.jsb.bot.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.jockie.bot.core.argument.Argument;
import com.jockie.bot.core.category.impl.CategoryImpl;
import com.jockie.bot.core.command.ICommand;
import com.jockie.bot.core.command.impl.CommandEvent;
import com.jockie.bot.core.command.impl.CommandImpl;
import com.jsb.bot.category.Category;
import com.jsb.bot.paged.PagedResult;
import com.jsb.bot.utility.ArgumentUtility;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;

public class CommandHelp extends CommandImpl {

	public CommandHelp() {
		super("help");
		
		super.setAliases("h", "commands", "cmds");
		super.setDescription("Shows all the commands on the bot with their descriptions");
	}
	
	public void onCommand(CommandEvent event, @Argument(value="command", endless=true, nullDefault=true) String commandArgument) {
		if (commandArgument == null) {
			PagedResult<CategoryImpl> paged = new PagedResult<>(Arrays.asList(Category.ALL))
					.setEntriesPerPage(1)
					.setListIndexes(false)
					.setDisplayFunction(category -> {
						StringBuilder page = new StringBuilder();
						page.append("**" + category.getName() + "**\n\n");
						
						List<ICommand> commands = new ArrayList<>(category.getCommands());
						commands.sort((a, b) -> a.getCommandTrigger().compareTo(b.getCommandTrigger()));
						for (ICommand command : commands) {
							if (!command.isPassive()) {
								page.append("`" + command.getCommandTrigger() + "` - " + command.getDescription() + "\n");
							}
						}
						
						return page.toString();
					});
			
			paged.send(event);
		} else {
			ICommand command = ArgumentUtility.getCommand(event.getCommandListener(), commandArgument);
			if (command == null) {
				event.reply("I could not find that command :no_entry:").queue();
				return;
			}
			
			StringBuilder permissions = new StringBuilder();
			for (int i = 0; i < command.getAuthorDiscordPermissions().size(); i++) {
				Permission permission = command.getAuthorDiscordPermissions().get(i);
				permissions.append(permission.getName());
				
				if (i != command.getAuthorDiscordPermissions().size() - 1) {
					permissions.append(", ");
				}
			}
			
			StringBuilder subCommands = new StringBuilder();
			for (int i = 0; i < command.getSubCommands().size(); i++) {
				ICommand subCommand = command.getSubCommands().get(i);
				subCommands.append(subCommand.getCommand());
				
				if (i != command.getSubCommands().size() - 1) {
					subCommands.append(", ");
				}
			}
		
			EmbedBuilder embed = new EmbedBuilder();
			embed.addField("Command", command.getCommandTrigger(), false);
			embed.addField("Description", command.getDescription(), false);
			embed.addField("Usage", command.getUsage(event.getPrefix()), false);
			
			if (!command.getAuthorDiscordPermissions().isEmpty()) {
				embed.addField("Required Permissions", permissions.toString(), false);
			}
			
			if (!command.getSubCommands().isEmpty()) {
				embed.addField("Sub Commands", subCommands.toString(), false);
			}
			
			event.reply(embed.build()).queue();
		}
	}
	
}
