package com.jsb.bot.module;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.bson.Document;

import com.jockie.bot.core.argument.Argument;
import com.jockie.bot.core.command.Command;
import com.jockie.bot.core.command.Command.AuthorPermissions;
import com.jockie.bot.core.command.impl.CommandEvent;
import com.jockie.bot.core.command.impl.CommandImpl;
import com.jockie.bot.core.module.Module;
import com.jsb.bot.database.Database;
import com.jsb.bot.database.callback.Callback;
import com.jsb.bot.modlog.Action;
import com.jsb.bot.mute.MuteListener;
import com.jsb.bot.paged.PagedResult;
import com.jsb.bot.utility.ArgumentUtility;
import com.jsb.bot.utility.TimeUtility;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.internal.utils.tuple.Pair;

@Module
public class ModuleWarn {
	
	public static class WarningResult {
		
		private List<Document> warnings;
		private Document warning;
		
		private Document actionTaken;
		private Throwable actionError;
		
		public WarningResult(List<Document> warnings, Document warning, Document actionTaken, Throwable actionError) {
			this.warnings = warnings;
			this.warning = warning;
			
			this.actionTaken = actionTaken;
			this.actionError = actionError;
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
		
		public Throwable getActionError() {
			return this.actionError;
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
	
	public static void doAction(Guild guild, User target, Document action, Callback<Void> callback) {
		Action actionType = Action.valueOf(action.getString("action").toUpperCase());
		
		Member member = guild.getMember(target);
		if(member == null) {
			throw new IllegalArgumentException("Target is not a member of the guild");
		}
		
		switch(actionType) {
			case BAN: {
				if(guild.getSelfMember().hasPermission(Permission.BAN_MEMBERS)) {
					if(guild.getSelfMember().canInteract(member)) {
						guild.ban(target, 7).queue($ -> {
							callback.onResult(null, null);
						}, reason -> {
							callback.onResult(null, reason);
						});
					}else{
						callback.onResult(null, new HierarchyException("Unable to ban that user because they are higher up than me"));
					}
				}else{
					callback.onResult(null, new PermissionException("Missing the `ban members` permissions to ban that user"));
				}
				
				break;
			}
			case KICK: {
				if(guild.getSelfMember().hasPermission(Permission.KICK_MEMBERS)) {
					if(guild.getSelfMember().canInteract(member)) {
						guild.kick(member).queue($ -> {
							callback.onResult(null, null);
						}, reason -> {
							callback.onResult(null, reason);
						});
					}else{
						callback.onResult(null, new HierarchyException("Unable to kick that user because they are higher up than me"));
					}
				}else{
					callback.onResult(null, new PermissionException("Missing the `kick members` permissions to kick that user"));
				}
				
				break;
			}
			case MUTE: {
				if(guild.getSelfMember().hasPermission(Permission.MANAGE_ROLES)) {
					MuteListener.getOrCreateMuteRole(guild, (muteRole, exception) -> {
						if(exception != null) {
							exception.printStackTrace();
							
							callback.onResult(null, new IllegalStateException("Something went wrong"));
							
							return;
						}
						
						if(guild.getSelfMember().canInteract(muteRole)) {
							guild.addRoleToMember(member, muteRole).queue($ -> {
								callback.onResult(null, null);
							}, reason -> {
								callback.onResult(null, reason);
							});
						}else{
							callback.onResult(null, new HierarchyException("Unable to mute that user because the mute role is higher up than me"));
						}
					});
				}else{
					callback.onResult(null, new PermissionException("Missing the `manage roles` permissions to mute that user"));
				}
				
				break;
			}
			default: {
				throw new UnsupportedOperationException("Unsupported action type");
			}
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
								ModuleWarn.doAction(guild, user, action, ($$, exception4) -> {
									if(exception4 != null) {
										callback.onResult(new WarningResult(warnings, warning, action, exception4), null);
									}else{
										callback.onResult(new WarningResult(warnings, warning, action, null), null);
									}
								});
								
								return;
							}else if(reApplyAction) {
								ModuleWarn.doAction(guild, user, action, ($$, exception4) -> {
									if(exception4 != null) {
										callback.onResult(new WarningResult(warnings, warning, action, exception4), null);
									}else{
										callback.onResult(new WarningResult(warnings, warning, action, null), null);
									}
								});
								
								return;
							}
						}
						
						callback.onResult(new WarningResult(warnings, warning, null, null), null);
					});
				});
			});
		});
	}
	
	public static class CommandWarn extends CommandImpl {
		
		public CommandWarn() {
			super("warn");
			
			super.setDescription("Warn a user about something they have done wrong");
		}
		
		@AuthorPermissions(Permission.MESSAGE_MANAGE)
		public void onCommand(CommandEvent event, @Argument("user") String user, @Argument(value="reason", nullDefault=true) String reason, @Argument(value="worth", nullDefault=true) Integer worth) {
			Member member = ArgumentUtility.getMember(event.getGuild(), user);
			if(member == null) {
				event.reply("I could not find that member :no_entry:").queue();
				
				return;
			}
			
			ModuleWarn.doWarn(event.getGuild(), event.getAuthor(), member.getUser(), reason, worth, (warning, exception) -> {
				if(exception != null) {
					exception.printStackTrace();
					
					event.reply("Something went wrong :no_entry:").queue();
					
					return;
				}
				
				int warnings = warning.getWarningCount();
				
				String message = "**" + member.getUser().getAsTag() + "** has been warned, they now have `" + warnings + "` warning" + (warnings == 1 ? "" : "s") + " (worth **" + warning.getTotalWorth() + "**)";
				if(warning.getActionTaken() != null) {
					if(warning.getActionError() != null) {
						message += ", failed to perform action `" + warning.getActionTaken().getString("action").toLowerCase() + "` due to `" + warning.getActionError().getMessage() + "`";
					}else{
						message += ", action taken: `" + warning.getActionTaken().getString("action").toLowerCase() + "`";
					}
				}
				
				event.reply(message).queue();
			});
		}
		
		@AuthorPermissions(Permission.MANAGE_SERVER)
		@Command(value="action reapply toggle", description="Toggle whether or not actions should re-apply if the user gets another warning")
		public void actionReapply(CommandEvent event) {
			Database.get().getGuildById(event.getGuild().getIdLong(), null, Projections.include("warning.reapply"), (data, exception) -> {
				if(exception != null) {
					exception.printStackTrace();
					
					event.reply("Something went wrong :no_entry:").queue();
					
					return;
				}
				
				boolean current = data.getEmbedded(List.of("warning", "reapply"), false);
				
				Database.get().updateGuildById(event.getGuild().getIdLong(), Updates.set("warning.reapply", !current), (result, exception2) -> {
					if(exception2 != null) {
						exception2.printStackTrace();
						
						event.reply("Something went wrong :no_entry:").queue();
						
						return;
					}
					
					event.reply("I will " + (!current ? "now" : "no longer") + " re-apply actions").queue();
				});
			});
		}
		
		@AuthorPermissions(Permission.MANAGE_SERVER)
		@Command(value="action set", description="Set an action for when a user reaches a certain amount of warnings")
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
	
	private void showUserWarnings(CommandEvent event, Member member) {
		Database.get().getWarnings(event.getGuild().getIdLong(), member.getIdLong(), (warnings, exception) -> {
			if(exception != null) {
				exception.printStackTrace();
				
				event.reply("Something went wrong :no_entry:").queue();
				
				return;
			}
			
			if(warnings.size() > 0) {
				new PagedResult<>(warnings)
					.setDisplayFunction(document -> {
						String reason = document.getString("reason");
						if(reason == null) {
							reason = "No reason provided";
						}
						
						if(reason.length() > 30) {
							reason = reason.substring(0, 31) + "...";
						}
						
						return reason + " (worth **" + document.getInteger("worth") + "**)";
					})
					.onSelect(selectEvent -> {
						Document warning = selectEvent.entry;
						
						ArgumentUtility.retrieveUser(event.getShardManager(), String.valueOf(warning.getLong("moderatorId"))).queue(moderator -> {
							EmbedBuilder embed = new EmbedBuilder();
							embed.setTitle("Worth " + warning.getInteger("worth"));
							
							embed.addField("User", member.getUser().getAsTag() + " (" + member.getId() + ")", false);
							embed.addField("Moderator", moderator.getAsTag() + " (" + moderator.getId() + ")", false);
							embed.addField("Reason", warning.getString("reason"), false);
							
							embed.setFooter("Created");
							embed.setTimestamp(Instant.ofEpochSecond(warning.getLong("createdAt")));
							
							event.reply(embed.build()).queue();
						});
					})
					.send(event);
			}else{
				event.reply("**" + member.getUser().getAsTag() + "** does not have any warnings").queue();
			}
		});
	}
	
	@Command(value="warnings", description="View all or a member's warnings")
	public void warnings(CommandEvent event, @Argument(value="member", nullDefault=true) String userStr) {
		if(userStr != null) {
			Member member = ArgumentUtility.getMember(event.getGuild(), userStr);
			if(member == null) {
				event.reply("I could not find that member :no_entry:").queue();
				
				return;
			}
			
			this.showUserWarnings(event, member);
		}else{
			Database.get().getWarnings(event.getGuild().getIdLong(), (warnings, exception) -> {
				if(exception != null) {
					exception.printStackTrace();
					
					event.reply("Something went wrong :no_entry:").queue();
					
					return;
				}
				
				Map<Long, Pair<AtomicInteger, AtomicInteger>> warningsPerUser = new HashMap<>();
				for(Document warning : warnings) {
					Pair<AtomicInteger, AtomicInteger> pair = warningsPerUser.computeIfAbsent(warning.getLong("userId"), $ -> Pair.of(new AtomicInteger(), new AtomicInteger()));
					
					/* Total warnings */
					pair.getLeft().addAndGet(1);
					
					/* Total worth */
					pair.getRight().addAndGet(warning.getInteger("worth"));
				}
				
				List<Entry<Long, Pair<AtomicInteger, AtomicInteger>>> list = new ArrayList<>(warningsPerUser.entrySet());
				list.removeIf(entry -> event.getShardManager().getUserById(entry.getKey()) == null);
				list.sort((a, b) -> -Integer.compare(a.getValue().getRight().get(), b.getValue().getRight().get()));
				
				new PagedResult<>(list)
					.setDisplayFunction(entry -> {
						User user = event.getShardManager().getUserById(entry.getKey());
						Pair<AtomicInteger, AtomicInteger> pair = entry.getValue();
						
						int warningCount = pair.getLeft().get();
						int warningWorth = pair.getRight().get();
						
						return user.getAsTag() + " **" + warningCount + "** warning" + (warningCount == 1 ? "" : "s") + " (worth **" + warningWorth + "**)";
					})
					.onSelect(selectEvent -> {
						this.showUserWarnings(event, event.getGuild().getMemberById(selectEvent.entry.getKey()));
					})
					.send(event);
			});
		}
	}
}