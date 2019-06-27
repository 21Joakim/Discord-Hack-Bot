package com.jsb.bot.command;

import java.time.Clock;
import java.util.Collections;
import java.util.List;

import org.bson.Document;

import com.jockie.bot.core.argument.Argument;
import com.jockie.bot.core.command.Command;
import com.jockie.bot.core.command.Command.AuthorPermissions;
import com.jockie.bot.core.command.impl.CommandEvent;
import com.jockie.bot.core.command.impl.CommandImpl;
import com.jsb.bot.database.Database;
import com.jsb.bot.database.callback.Callback;
import com.jsb.bot.modlog.Action;
import com.jsb.bot.module.ModuleBasic;
import com.jsb.bot.utility.ArgumentUtility;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;

public class CommadWarn extends CommandImpl {

	public CommadWarn() {
		super("warn");
	}
	
	@AuthorPermissions(Permission.MESSAGE_MANAGE)
	public void onCommand(CommandEvent event, @Argument("user") String user, @Argument(value="reason", nullDefault=true) String reason, @Argument(value="worth", nullDefault=true) Integer worth) {
		Member member = ArgumentUtility.getMember(event.getGuild(), user);
		if(member == null) {
			event.reply("I could not find that member :no_entry:").queue();
			
			return;
		}
		
		ModuleBasic.getReason(event.getGuild(), reason, templatedReason -> {
			Document warning = new Document()
				.append("guildId", event.getGuild().getIdLong())
				.append("userId", member.getIdLong())
				.append("moderatorId", event.getAuthor().getIdLong())
				.append("worth", worth != null ? worth : 1)
				.append("createdAt", Clock.systemUTC().instant().getEpochSecond())
				.append("reason", templatedReason);
			
			Database.get().insertWarning(warning, ($, exception) -> {
				if(exception != null) {
					exception.printStackTrace();
					
					event.reply("Something went wrong :no_entry:").queue();
					
					return;
				}
				
				Database.get().getWarnings(event.getGuild().getIdLong(), member.getIdLong(), (warnings, exception2) -> {
					if(exception2 != null) {
						exception2.printStackTrace();
						
						event.reply("Something went wrong :no_entry:").queue();
						
						return;
					}
					
					Database.get().getGuildById(event.getGuild().getIdLong(), null, Projections.include("warning.actions"), (guildData, exception3) -> {
						if(exception3 != null) {
							exception3.printStackTrace();
							
							event.reply("Something went wrong :no_entry:").queue();
							
							return;
						}
						
						int totalWorth = warnings.stream()
							.mapToInt(w -> w.getInteger("worth"))
							.sum();
						
						// Check actions
						
						event.reply("**" + member.getUser().getAsTag() + "** has been warned, they now have `" + warnings.size() + "` warnings (" + totalWorth + " worth)").queue();
					});
				});
			});
		});
	}
	
	@AuthorPermissions(Permission.MANAGE_SERVER)
	@Command("action set")
	public void actionSet(CommandEvent event, @Argument(value="warning worth") int worth, @Argument(value="action") String rawAction) {
		Action action;
		try {
			action = Action.valueOf(rawAction.toUpperCase());
			if(!action.isWarnAction()) {
				throw new IllegalArgumentException();
			}
		}catch(IllegalArgumentException e) {
			event.reply("Invalid action").queue();
			
			return;
		}
		
		Database.get().getGuildById(event.getGuild().getIdLong(), null, Projections.include("warning.actions"), (document, exception) -> {
			if(exception != null) {
				exception.printStackTrace();
				
				event.reply("Something went wrong :no_entry:").queue();
				
				return;
			}
			
			Document actionDocument = new Document()
				.append("worth", worth)
				.append("action", action.toString());
			
			Callback<UpdateResult> callback = (result, exception2) -> {
				if(exception2 != null) {
					exception2.printStackTrace();
					
					event.reply("Something went wrong :no_entry:").queue();
					
					return;
				}
				
				event.reply("Set the action `" + action.getName() + "` for when they reach `" + worth + "` warnings").queue();
			};
			
			List<Document> actions = document.getEmbedded(List.of("warning", "actions"), Collections.emptyList());
			
			if(actions.stream().anyMatch(a -> a.getInteger("worth") == worth)) {
				Database.get().updateGuildById(event.getGuild().getIdLong(), Updates.set("warning.actions.$[action]", actionDocument), new UpdateOptions().arrayFilters(List.of(Filters.eq("action.worth", worth))), callback);
			}else{
				Database.get().updateGuildById(event.getGuild().getIdLong(), Updates.push("warning.actions", actionDocument), callback);
			}
		});
	}
}