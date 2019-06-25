package com.jsb.bot.command;

import com.jockie.bot.core.argument.Argument;
import com.jockie.bot.core.command.ICommand;
import com.jockie.bot.core.command.impl.CommandEvent;
import com.jockie.bot.core.command.impl.CommandImpl;
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
			PagedResult<ICommand> paged = new PagedResult<>(event.getCommandListener().getAllCommands())
					.setEntriesPerPage(20)
					.setListIndexes(false)
					.setDisplayFunction(command -> "`" + command.getCommandTrigger() + "` - " + command.getDescription());
			
			paged.send(event);
		} else {
			ICommand command = ArgumentUtility.getCommand(event.getCommandListener(), commandArgument);
			if (command == null) {
				event.reply("I could not find that command :no_entry:").queue();
				return;
			}
			
			StringBuilder permissions = new StringBuilder();
			for (Permission permission : command.getAuthorDiscordPermissions()) {
				permissions.append("• " + permission.getName() + "\n");
			}
		
			EmbedBuilder embed = new EmbedBuilder();
			embed.addField("Command", command.getCommandTrigger(), false);
			embed.addField("Description", command.getDescription(), false);
			embed.addField("Usage", command.getUsage(event.getPrefix()), false);
			
			if (!command.getAuthorDiscordPermissions().isEmpty()) {
				embed.addField("Required Permissions", permissions.toString(), false);
			}
			
			event.reply(embed.build()).queue();
		}
	}
	
}
