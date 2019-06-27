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
import com.jsb.bot.utility.TimeUtility;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

public class CommadWarn extends CommandImpl {

	public CommadWarn() {
		super("warn");
	}
	
	public static class WarningResult {
		
		private List<Document> warnings;
		private Document actionTaken;
		private Document warning;
		
		public WarningResult(List<Document> warnings, Document warning, Document actionTaken) {
			this.warnings = warnings;
			this.warning = warning;
			this.actionTaken = actionTaken;
		}
		
		public List<Document> getWarnings() {
			return this.warnings;
		}
		
		public Document getWarning() {
			return this.warning;
		}
		
		public Document getActionTaken() {
			return this.actionTaken;
		}
		
		public int getWarningCount() {
			return this.warnings.size() + 1;
		}
		
		public int getTotalWorth() {
			return this.warnings.stream()
				.mapToInt(w -> w.getInteger("worth"))
				.sum() + this.warning.getInteger("worth");
		}
	}
	
	public static void doWarn(Guild guild, User moderator, User user, String reason, Integer worth, Callback<WarningResult> callback) {
		ModuleBasic.getReason(guild, reason, templatedReason -> {
			Document warning = new Document()
				.append("guildId", guild.getIdLong())
				.append("userId", user.getIdLong())
				.append("moderatorId", moderator.getIdLong())
				.append("worth", worth != null ? worth : 1)
				.append("createdAt", Clock.systemUTC().instant().getEpochSecond())
				.append("reason", templatedReason);
			
			Database.get().getWarnings(guild.getIdLong(), user.getIdLong(), (warnings, exception) -> {
				if(exception != null) {
					callback.onResult(null, exception);
					
					return;
				}
				
				Database.get().insertWarning(warning, ($, exception2) -> {
					if(exception2 != null) {
						callback.onResult(null, exception2);
						
						return;
					}
					
					Database.get().getGuildById(guild.getIdLong(), null, Projections.include("warning.actions", "warning.reapply"), (guildData, exception3) -> {
						if(exception3 != null) {
							callback.onResult(null, exception3);
							
							return;
						}
						
						boolean reApplyAction = guildData.getEmbedded(List.of("warning", "reapply"), false);
						
						int totalWorth = warnings.stream()
							.mapToInt(w -> w.getInteger("worth"))
							.sum() + warning.getInteger("worth");
						
						List<Document> actions = guildData.getEmbedded(List.of("warning", "actions"), Collections.emptyList());
						
						Document action = actions.stream()
							.filter(a -> a.getInteger("worth") <= totalWorth)
							.sorted((a, a2) -> -Integer.compare(a.getInteger("worth"), a2.getInteger("worth")))
							.findFirst()
							.orElse(null);
						
						if(action != null) {
							if(action.getInteger("worth") == totalWorth) {
								/* Do action */
								
								callback.onResult(new WarningResult(warnings, warning, action), null);
								
								return;
							}else if(reApplyAction) {
								/* Do action */
								
								callback.onResult(new WarningResult(warnings, warning, action), null);
								
								return;
							}
						}
						
						callback.onResult(new WarningResult(warnings, warning, null), null);
					});
				});
			});
		});
	}
	
	@AuthorPermissions(Permission.MESSAGE_MANAGE)
	public void onCommand(CommandEvent event, @Argument("user") String user, @Argument(value="reason", nullDefault=true) String reason, @Argument(value="worth", nullDefault=true) Integer worth) {
		Member member = ArgumentUtility.getMember(event.getGuild(), user);
		if(member == null) {
			event.reply("I could not find that member :no_entry:").queue();
			
			return;
		}
		
		CommadWarn.doWarn(event.getGuild(), event.getAuthor(), member.getUser(), reason, worth, (warning, exception) -> {
			if(exception != null) {
				exception.printStackTrace();
				
				event.reply("Something went wrong :no_entry:").queue();
				
				return;
			}
			
			String message = "**" + member.getUser().getAsTag() + "** has been warned, they now have `" + warning.getWarningCount() + "` warnings (worth **" + warning.getTotalWorth() + "**)";
			if(warning.getActionTaken() != null) {
				message += ", action taken: `" + warning.getActionTaken().getString("action").toLowerCase() + "`";
			}
			
			event.reply(message).queue();
		});
	}
	
	@AuthorPermissions(Permission.MANAGE_SERVER)
	@Command("action set")
	public void actionSet(CommandEvent event, @Argument(value="warning worth") int worth, @Argument(value="action", endless=true) String rawAction) {
		String extraContent;
		if(rawAction.contains(" ")) {
			extraContent = rawAction.substring(rawAction.indexOf(" ") + 1);
			rawAction = rawAction.substring(0, rawAction.indexOf(" "));
		}else{
			extraContent = null;
		}
		
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
			
			if(action.equals(Action.MUTE)) {
				if(extraContent != null) {
					try {
						long duration = TimeUtility.timeStringToSeconds(extraContent);
						
						actionDocument.append("duration", duration);
					}catch(IllegalArgumentException e) {
						event.reply("Invalid mute duration").queue();
						
						return;
					}
				}else{
					event.reply("Missing mute duration").queue();
					
					return;
				}
			}
			
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