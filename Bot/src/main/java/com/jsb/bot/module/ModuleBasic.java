package com.jsb.bot.module;

import java.awt.Color;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import org.bson.Document;

import com.jockie.bot.core.argument.Argument;
import com.jockie.bot.core.command.Command;
import com.jockie.bot.core.command.Command.AuthorPermissions;
import com.jockie.bot.core.command.Command.BotPermissions;
import com.jockie.bot.core.command.impl.CommandEvent;
import com.jockie.bot.core.command.impl.CommandImpl;
import com.jockie.bot.core.module.Module;
import com.jsb.bot.database.Database;
import com.jsb.bot.modlog.Action;
import com.jsb.bot.modlog.ModlogListener;
import com.jsb.bot.mute.MuteListener;
import com.jsb.bot.utility.ArgumentUtility;
import com.jsb.bot.utility.TimeUtility;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Updates;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Guild.Ban;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;

@Module
public class ModuleBasic {
	
	public static final Pattern TEMPLATE_PATTERN = Pattern.compile("t(|emplate):[a-z0-9_-]+", Pattern.CASE_INSENSITIVE);
	
	public static void getReason(Guild guild, String reason, Consumer<String> consumer) {
		if(reason == null) {
			consumer.accept(null);
			
			return;
		}
		
		Matcher matcher = ModuleBasic.TEMPLATE_PATTERN.matcher(reason);
		List<String> foundTemplates = new ArrayList<>();
		
		while(matcher.find()) {
			foundTemplates.add(matcher.group());
		}
		
		if(foundTemplates.size() > 0) {
			Database.get().getGuildById(guild.getIdLong(), null, Projections.include("template.reasons"), (document, exception) -> {
				/* 
				 * In case this fails we will just silently pass through and pretend like nothing happened 
				 * to avoid the user not being able to use the command 
				 */
				if(exception != null) {
					exception.printStackTrace();
					
					consumer.accept(reason);
					
					return;
				}
				
				String newReason = reason;
				
				List<Document> templates = document.getEmbedded(List.of("template", "reasons"), Collections.emptyList());
				Map<String, String> templateMap = templates.stream().collect(Collectors.toMap(t -> t.getString("name").toLowerCase(), t -> t.getString("template")));
				
				for(String template : foundTemplates) {
					String templateKey = template.substring(template.indexOf(":") + 1).toLowerCase();
					
					if(templateMap.containsKey(templateKey)) {
						newReason = newReason.replace(template, templateMap.get(templateKey));
					}
				}
				
				consumer.accept(newReason);
			});
			
			return;
		}
		
		consumer.accept(reason);
	}
	
	@Command(description="Shows my ping to Discord")
	public void ping(CommandEvent event) {
		event.getJDA().getRestPing().queue(ping -> {
			event.reply(":stopwatch: **" + ping + "ms**\n:heartbeat: **" + event.getJDA().getGatewayPing() + "ms**").queue();
		});
	}
	
	public class PruneCommand extends CommandImpl {
		
		private final int pruneAgeLimit = 1209600;
		private final int pruneLimit = 100;
		
		public PruneCommand() {
			super("prune");
			
			super.setDescription("Prune a set amount of messages with various filters");
			super.setAuthorDiscordPermissions(Permission.MESSAGE_MANAGE);
			super.setBotDiscordPermissions(Permission.MESSAGE_MANAGE, Permission.MESSAGE_HISTORY);
		}
		
		public void onCommand(CommandEvent event, @Argument(value="amount", nullDefault=true) Integer amount) {
			int limit = amount == null ? 100 : amount;
			
			if (limit > 100) {
				event.reply("You can only prune up to 100 messages :no_entry:").queue();
				return;
			}
			
			if (limit < 1) {
				event.reply("You have to prune at least 1 message :no_entry:").queue();
				return;
			}
			
			event.getMessage().delete().queue();
			
			event.getTextChannel().getHistory().retrievePast(limit).queue(messages -> {
				ZonedDateTime dateTime = ZonedDateTime.now(ZoneId.of("UTC"));
				for (Message message : new ArrayList<>(messages)) {
					if (Duration.between(message.getTimeCreated(), dateTime).toSeconds() > this.pruneAgeLimit) {
						messages.remove(message);
					}
				}
				
				if (messages.isEmpty()) {
					return;
				}
				
				if (messages.size() == 1) {
					messages.get(0).delete().queue();
				} else {
					event.getTextChannel().deleteMessages(messages).queue();
				}
			});
		}
		
		@Command(value="images", aliases={"image"}, description="Prune a set amount of messages which contain images")
		@AuthorPermissions({Permission.MESSAGE_MANAGE})
		@BotPermissions({Permission.MESSAGE_MANAGE, Permission.MESSAGE_HISTORY})
		public void images(CommandEvent event, @Argument(value="amount", nullDefault=true) Integer amount) {
			int limit = amount == null ? 100 : amount;
			
			if (limit > 100) {
				event.reply("You can only prune up to 100 messages :no_entry:").queue();
				return;
			}
			
			if (limit < 1) {
				event.reply("You have to prune at least 1 message :no_entry:").queue();
				return;
			}
			
			event.getMessage().delete().queue();
			
			event.getTextChannel().getHistory().retrievePast(this.pruneLimit).queue(messages -> {
				ZonedDateTime dateTime = ZonedDateTime.now(ZoneId.of("UTC"));
				for (Message message : new ArrayList<>(messages)) {
					if (Duration.between(message.getTimeCreated(), dateTime).toSeconds() > this.pruneAgeLimit) {
						messages.remove(message);
					} else if (!message.getAttachments().isEmpty()) {
						boolean delete = false;
						for (Attachment attachment : message.getAttachments()) {
							if (attachment.isImage()) {
								delete = true;
								break;
							}
						}
						
						if (!delete) {
							messages.remove(message);
						}
					} else {
						messages.remove(message);
					}
				}
				
				if (messages.isEmpty()) {
					return;
				}
				
				messages = messages.subList(0, Math.min(limit, messages.size()));
				
				if (messages.size() == 1) {
					messages.get(0).delete().queue();
				} else {
					event.getTextChannel().deleteMessages(messages).queue();
				}
			});
		}
		
		@Command(value="user", aliases={"member"}, description="Prune a set amount of messages from a specified user")
		@AuthorPermissions({Permission.MESSAGE_MANAGE})
		@BotPermissions({Permission.MESSAGE_MANAGE, Permission.MESSAGE_HISTORY})
		public void user(CommandEvent event, @Argument(value="user") String userArgument, @Argument(value="amount", nullDefault=true) Integer amount) {
			int limit = amount == null ? 100 : amount;
			
			if (limit > 100) {
				event.reply("You can only prune up to 100 messages :no_entry:").queue();
				return;
			}
			
			if (limit < 1) {
				event.reply("You have to prune at least 1 message :no_entry:").queue();
				return;
			}
			
			Member member = ArgumentUtility.getMember(event.getGuild(), userArgument);
			if (member == null) {
				event.reply("I could not find that user :no_entry:").queue();
				return;
			}
			
			event.getMessage().delete().queue();
			
			event.getTextChannel().getHistory().retrievePast(this.pruneLimit).queue(messages -> {
				ZonedDateTime dateTime = ZonedDateTime.now(ZoneId.of("UTC"));
				for (Message message : new ArrayList<>(messages)) {
					if (Duration.between(message.getTimeCreated(), dateTime).toSeconds() > this.pruneAgeLimit) {
						messages.remove(message);
					} else if (!message.getMember().equals(member)) {
						messages.remove(message);
					}
				}
				
				if (messages.isEmpty()) {
					return;
				}
				
				messages = messages.subList(0, Math.min(limit, messages.size()));
				
				if (messages.size() == 1) {
					messages.get(0).delete().queue();
				} else {
					event.getTextChannel().deleteMessages(messages).queue();
				}
			});
		}
		
		@Command(value="embeds", aliases={"embed"}, description="Prune a set amount of messages which contain embeds")
		@AuthorPermissions({Permission.MESSAGE_MANAGE})
		@BotPermissions({Permission.MESSAGE_MANAGE, Permission.MESSAGE_HISTORY})
		public void embeds(CommandEvent event, @Argument(value="amount", nullDefault=true) Integer amount) {
			int limit = amount == null ? 100 : amount;
			
			if (limit > 100) {
				event.reply("You can only prune up to 100 messages :no_entry:").queue();
				return;
			}
			
			if (limit < 1) {
				event.reply("You have to prune at least 1 message :no_entry:").queue();
				return;
			}
			
			event.getMessage().delete().queue();
			
			event.getTextChannel().getHistory().retrievePast(this.pruneLimit).queue(messages -> {
				ZonedDateTime dateTime = ZonedDateTime.now(ZoneId.of("UTC"));
				for (Message message : new ArrayList<>(messages)) {
					if (Duration.between(message.getTimeCreated(), dateTime).toSeconds() > this.pruneAgeLimit) {
						messages.remove(message);
					} else if (message.getEmbeds().isEmpty()) {
						messages.remove(message);
					}
				}
				
				if (messages.isEmpty()) {
					return;
				}
				
				messages = messages.subList(0, Math.min(limit, messages.size()));
				
				if (messages.size() == 1) {
					messages.get(0).delete().queue();
				} else {
					event.getTextChannel().deleteMessages(messages).queue();
				}
			});
		}
		
		@Command(value="regex", description="Prune a set amount of messages which matches the specified regex")
		@AuthorPermissions({Permission.MESSAGE_MANAGE})
		@BotPermissions({Permission.MESSAGE_MANAGE, Permission.MESSAGE_HISTORY})
		public void regex(CommandEvent event, @Argument(value="regex") String regexArgument, @Argument(value="amount", nullDefault=true) Integer amount) {
			int limit = amount == null ? 100 : amount;
			
			if (limit > 100) {
				event.reply("You can only prune up to 100 messages :no_entry:").queue();
				return;
			}
			
			if (limit < 1) {
				event.reply("You have to prune at least 1 message :no_entry:").queue();
				return;
			}
			
			Pattern regex;
			try {
				regex = Pattern.compile(regexArgument, Pattern.CASE_INSENSITIVE);
			} catch(PatternSyntaxException e) {
				event.reply("The syntax for the regex you supplied was invalid :no_entry:\n\n" + e.getMessage()).queue();
				return;
			}
			
			event.getMessage().delete().queue();
			
			event.getTextChannel().getHistory().retrievePast(this.pruneLimit).queue(messages -> {
				ZonedDateTime dateTime = ZonedDateTime.now(ZoneId.of("UTC"));
				for (Message message : new ArrayList<>(messages)) {
					if (Duration.between(message.getTimeCreated(), dateTime).toSeconds() > this.pruneAgeLimit) {
						messages.remove(message);
					} else if (!regex.matcher(message.getContentRaw()).matches()) {
						messages.remove(message);
					}
				}
				
				if (messages.isEmpty()) {
					return;
				}
				
				messages = messages.subList(0, Math.min(limit, messages.size()));
				
				if (messages.size() == 1) {
					messages.get(0).delete().queue();
				} else {
					event.getTextChannel().deleteMessages(messages).queue();
				}
			});
		}
		
		@Command(value="bots", aliases={"bot"}, description="Prune a set amount of messages which are sent by bots")
		@AuthorPermissions({Permission.MESSAGE_MANAGE})
		@BotPermissions({Permission.MESSAGE_MANAGE, Permission.MESSAGE_HISTORY})
		public void bots(CommandEvent event, @Argument(value="amount", nullDefault=true) Integer amount) {
			int limit = amount == null ? 100 : amount;
			
			if (limit > 100) {
				event.reply("You can only prune up to 100 messages :no_entry:").queue();
				return;
			}
			
			if (limit < 1) {
				event.reply("You have to prune at least 1 message :no_entry:").queue();
				return;
			}
			
			event.getMessage().delete().queue();
			
			event.getTextChannel().getHistory().retrievePast(this.pruneLimit).queue(messages -> {
				ZonedDateTime dateTime = ZonedDateTime.now(ZoneId.of("UTC"));
				for (Message message : new ArrayList<>(messages)) {
					if (Duration.between(message.getTimeCreated(), dateTime).toSeconds() > this.pruneAgeLimit) {
						messages.remove(message);
					} else if (!message.getAuthor().isBot()) {
						messages.remove(message);
					}
				}
				
				if (messages.isEmpty()) {
					return;
				}
				
				messages = messages.subList(0, Math.min(limit, messages.size()));
				
				if (messages.size() == 1) {
					messages.get(0).delete().queue();
				} else {
					event.getTextChannel().deleteMessages(messages).queue();
				}
			});
		}
		
	}
	
	@Command(value="kick", description="Kick a user from the server")
	@AuthorPermissions({Permission.KICK_MEMBERS})
	@BotPermissions({Permission.KICK_MEMBERS})
	public void kick(CommandEvent event, @Argument(value="user") String userArgument, @Argument(value="reason", endless=true, nullDefault=true) String reason) {
		Member member = ArgumentUtility.getMember(event.getGuild(), userArgument);
		if (member == null) {
			event.reply("I could not find that user :no_entry:").queue();
			return;
		}
		
		if (member.equals(event.getMember())) {
			event.reply("You cannot kick youself :no_entry:").queue();
			return;
		}
		
		if (member.equals(event.getSelfMember())) {
			event.reply("I cannot kick myself :no_entry:").queue();
			return;
		}
		
		if (!event.getMember().canInteract(member)) {
			event.reply("You cannot kick a user with a higher or equal top role than yours :no_entry:").queue();
			return;
		}
		
		if (!event.getSelfMember().canInteract(member)) {
			event.reply("I cannot kick a user with a higher or equal top role than me :no_entry:").queue();
			return;
		}
		
		ModuleBasic.getReason(event.getGuild(), reason, templatedReason -> {
			event.getGuild().kick(member).reason((templatedReason == null ? "" : templatedReason) + " [" + event.getAuthor().getAsTag() + "]").queue($ -> {
				event.reply("**" + member.getUser().getAsTag() + "** has been kicked").queue();
				
				ModlogListener.createModlog(event.getGuild(), event.getAuthor(), member.getUser(), templatedReason, false, Action.KICK);
			});
		});
	}
	
	private void banUser(CommandEvent event, User user, String reason) {
		event.getGuild().retrieveBanList().queue(bans -> {
			for (Ban ban : bans) {
				if (ban.getUser().equals(user)) {
					event.reply("That user is already banned :no_entry:").queue();
					return;
				}
			}
			
			ModuleBasic.getReason(event.getGuild(), reason, templatedReason -> {
				event.getGuild().ban(user, 1).reason((templatedReason == null ? "" : templatedReason) + " [" + event.getAuthor().getAsTag() + "]").queue($ -> {
					event.reply("**" + user.getAsTag() + "** has been banned").queue();
					
					ModlogListener.createModlog(event.getGuild(), event.getAuthor(), user, templatedReason, false, Action.BAN);
				});
			});
		});
	}
	
	@Command(value="ban", description="Ban a user from the server")
	@AuthorPermissions({Permission.BAN_MEMBERS})
	@BotPermissions({Permission.BAN_MEMBERS})
	public void ban(CommandEvent event, @Argument(value="user") String userArgument, @Argument(value="reason", endless=true, nullDefault=true) String reason) {
		Member member = ArgumentUtility.getMember(event.getGuild(), userArgument);
		if (member == null) {
			User user = ArgumentUtility.getUser(event.getShardManager(), userArgument);
			if (user == null) {
				ArgumentUtility.retrieveUser(event.getShardManager(), userArgument).queue(retrievedUser -> {
					if (retrievedUser != null) {
						this.banUser(event, retrievedUser, reason);
					} else {
						event.reply("I could not find that user :no_entry:").queue();
					}
				}, e -> {
					if (e instanceof ErrorResponseException) {
						ErrorResponseException exception = (ErrorResponseException) e;
						if (exception.getErrorResponse().equals(ErrorResponse.UNKNOWN_USER)) {
							event.reply("I could not find that user :no_entry:").queue();
						}
					}
				});
			} else {
				this.banUser(event, user, reason);
			}
			
			return;
		}
		
		if (member.equals(event.getMember())) {
			event.reply("You cannot ban youself :no_entry:").queue();
			return;
		}
		
		if (member.equals(event.getSelfMember())) {
			event.reply("I cannot ban myself :no_entry:").queue();
			return;
		}
		
		if (!event.getMember().canInteract(member)) {
			event.reply("You cannot ban a user with a higher or equal top role than yours :no_entry:").queue();
			return;
		}
		
		if (!event.getSelfMember().canInteract(member)) {
			event.reply("I cannot ban a user with a higher or equal top role than me :no_entry:").queue();
			return;
		}
		
		this.banUser(event, member.getUser(), reason);
	}
	
	private void unbanUser(CommandEvent event, User user, String reason) {
		event.getGuild().retrieveBanList().queue(bans -> {
			for (Ban ban : bans) {
				if (ban.getUser().equals(user)) {
					ModuleBasic.getReason(event.getGuild(), reason, templatedReason -> {
						event.getGuild().unban(user).reason((templatedReason == null ? "" : templatedReason) + " [" + event.getAuthor().getAsTag() + "]").queue($ -> {
							event.reply("**" + user.getAsTag() + "** has been unbanned").queue();
							
							ModlogListener.createModlog(event.getGuild(), event.getAuthor(), user, templatedReason, false, Action.UNBAN);
						});
					});
					
					return;
				}
			}
			
			event.reply("That user is not banned :no_entry:").queue();
		});
	}
	
	@Command(value="unban", description="Unban a user from the server")
	@AuthorPermissions({Permission.BAN_MEMBERS})
	@BotPermissions({Permission.BAN_MEMBERS})
	public void unban(CommandEvent event, @Argument(value="user") String userArgument, @Argument(value="reason", endless=true, nullDefault=true) String reason) {
		User user = ArgumentUtility.getUser(event.getShardManager(), userArgument);
		if (user == null) {
			ArgumentUtility.retrieveUser(event.getShardManager(), userArgument).queue(retrievedUser -> {
				if (retrievedUser != null) {
					this.unbanUser(event, retrievedUser, reason);
				} else {
					event.reply("I could not find that user :no_entry:").queue();
				}
			}, e -> {
				if (e instanceof ErrorResponseException) {
					ErrorResponseException exception = (ErrorResponseException) e;
					if (exception.getErrorResponse().equals(ErrorResponse.UNKNOWN_USER)) {
						event.reply("I could not find that user :no_entry:").queue();
					}
				}
			});
			
			return;
		}
		
		if (event.getGuild().isMember(user)) {
			event.reply("That user is not banned :no_entry:").queue();
			return;
		}
		
		this.unbanUser(event, user, reason);
	}
	
	@Command(value="mute", description="Mutes a user server wide")
	@AuthorPermissions({Permission.MESSAGE_MANAGE})
	@BotPermissions({Permission.MANAGE_ROLES})
	public void mute(CommandEvent event, @Argument(value="user") String userArgument, @Argument(value="time") String timeString, @Argument(value="reason", endless=true, nullDefault=true) String reason) {
		Member member = ArgumentUtility.getMember(event.getGuild(), userArgument);
		if (member == null) {
			event.reply("I could not find that user :no_entry:");
			return;
		}
		
		if (member.equals(event.getMember())) {
			event.reply("You cannot mute yourself :no_entry:").queue();
			return;
		}
		
		if (member.equals(event.getSelfMember())) {
			event.reply("I cannot mute myself :no_entry:").queue();
			return;
		}
		
		if (!event.getMember().canInteract(member)) {
			event.reply("You cannot mute someone with a higher or equal top role than yours :no_entry:").queue();
			return;
		}
		
		if (member.hasPermission(Permission.ADMINISTRATOR)) {
			event.reply("I cannot mute someone with administrator permissions :no_entry:").queue();
			return;
		}
		
		long muteLength;
		try {
			muteLength = TimeUtility.timeStringToSeconds(timeString);
		} catch(IllegalArgumentException e) {
			event.reply(e.getMessage() + " :no_entry:").queue();
			return;
		}
		
		MuteListener.getOrCreateMuteRole(event.getGuild(), (role, exception) -> {
			if (!event.getSelfMember().canInteract(role)) {
				event.reply("I cannot give a role which is higher or equal then my top role :no_entry:").queue();
				return;
			}
			
			if (member.getRoles().contains(role)) {
				event.reply("That user is already muted :no_entry:").queue();
				return;
			}
			
			event.getGuild().addRoleToMember(member, role).queue($ -> {
				Database.get().getGuildById(event.getGuild().getIdLong(), null, Projections.include("mute.users"), (data, readException) -> {
					if (readException != null) {
						readException.printStackTrace();
						
						event.reply("Something went wrong :no_entry:").queue();
					} else {
						List<Document> users = data.getEmbedded(List.of("mute", "users"), new ArrayList<>());
						
						Document muteData = new Document()
								.append("length", muteLength)
								.append("time", Clock.systemUTC().instant().getEpochSecond())
								.append("id", member.getUser().getIdLong());
						
						users.add(muteData);
						
						Database.get().updateGuildById(event.getGuild().getIdLong(), Updates.set("mute.users", users), (result, writeException) -> {
							if (writeException != null) {
								writeException.printStackTrace();
								
								event.reply("Something went wrong :no_entry:").queue();
							} else {
								event.reply("**" + member.getUser().getAsTag() + "** has been muted for some time here").queue();
								
								ModuleBasic.getReason(event.getGuild(), reason, templatedReason -> {
									ModlogListener.createModlog(event.getGuild(), event.getAuthor(), member.getUser(), templatedReason, false, Action.MUTE);
									
									ScheduledFuture<?> executor = MuteListener.scheduledExector.schedule(() -> {
										MuteListener.unmuteUser(event.getShardManager(), event.getGuild().getIdLong(), member.getUser().getIdLong(), role.getIdLong());
									}, muteLength, TimeUnit.SECONDS);
									
									MuteListener.putExecutor(event.getGuild().getIdLong(), member.getUser().getIdLong(), executor);
								});
							}
						});
					}
				});
			});
		});
	}
	
	@Command(value="unmute", description="Unmute a user who is currently muted")
	@AuthorPermissions({Permission.MESSAGE_MANAGE})
	@BotPermissions({Permission.MANAGE_ROLES})
	public void unmute(CommandEvent event, @Argument(value="user") String userArgument, @Argument(value="reason", endless=true, nullDefault=true) String reason) {
		Member member = ArgumentUtility.getMember(event.getGuild(), userArgument);
		if (member == null) {
			event.reply("I could not find that user :no_entry:");
			return;
		}
		
		if (!event.getMember().canInteract(member)) {
			event.reply("You cannot unmute someone with a higher or equal top role than yours :no_entry:").queue();
			return;
		}
		
		Database.get().getGuildById(event.getGuild().getIdLong(), null, Projections.include("mute.role", "mute.users"), (data, readException) -> {
			if (readException != null) {
				readException.printStackTrace();
				
				event.reply("Something went wrong :no_entry:").queue();
			} else {
				Document muteData = data.get("mute", Document.class);
				List<Document> users = muteData.getList("users", Document.class);
				long muteRoleId = muteData.getLong("role");
				
				Role muteRole = event.getGuild().getRoleById(muteRoleId);
				if (muteRole == null) {
					event.reply("That user is not muted :no_entry:").queue();
				} else {
					if (!member.getRoles().contains(muteRole)) {
						event.reply("That user is not muted :no_entry:").queue();
						return;
					}
					
					for (Document user : users) {
						if (member.getIdLong() == user.getLong("id")) {
							users.remove(user);
							break;
						}
					}
					
					Database.get().updateGuildById(event.getGuild().getIdLong(), Updates.set("mute.users", users), (result, writeException) -> {
						if (writeException != null) {
							writeException.printStackTrace();
							
							event.reply("Something went wrong :no_entry:").queue();
						} else {
							event.getGuild().removeRoleFromMember(member, muteRole).queue($ -> {
								event.reply("**" + member.getUser().getAsTag() + "** has been unmuted").queue();
								
								ModuleBasic.getReason(event.getGuild(), reason, templatedReason -> {
									ModlogListener.createModlog(event.getGuild(), event.getAuthor(), member.getUser(), templatedReason, false, Action.UNMUTE);
									
									MuteListener.removeExecutor(event.getGuild().getIdLong(), member.getIdLong());
								});
							});
						}
					});
				}
			}
		});
	}
	
	@Command(value="voice kick", aliases={"voicekick", "disconnect", "dc"}, description="Disconnect a user from the voice channel they are currently in")
	@AuthorPermissions({Permission.VOICE_MOVE_OTHERS})
	@BotPermissions({Permission.VOICE_MOVE_OTHERS})
	public void voiceKick(CommandEvent event, @Argument(value="user") String userArgument, @Argument(value="reason", endless=true, nullDefault=true) String reason) {
		Member member = ArgumentUtility.getMember(event.getGuild(), userArgument);
		if (member == null) {
			event.reply("I could not find that user :no_entry:").queue();
			return;
		}
		
		if (member.equals(event.getMember())) {
			event.reply("You cannot disconnect yourself :no_entry:").queue();
			return;
		}
		
		VoiceChannel channel = member.getVoiceState().getChannel();
		if (channel == null) {
			event.reply("That user is not in a voice channel :no_entry:").queue();
			return;
		}
		
		event.getGuild().moveVoiceMember(member, null).queue($ -> {
			event.reply("**" + member.getUser().getAsTag() + "** has been disconnected from " + channel.getName()).queue();
			
			ModuleBasic.getReason(event.getGuild(), reason, templatedReason -> {
				ModlogListener.createModlog(event.getGuild(), event.getAuthor(), member.getUser(), templatedReason, false, Action.VOICE_KICK);
			});
		});
	}
	
	@Command(value="rename", aliases={"nick", "nickname", "set nick", "setnick", "set nickname", "setnickname"}, description="Set a users nickname")
	@AuthorPermissions({Permission.NICKNAME_CHANGE})
	@BotPermissions({Permission.NICKNAME_MANAGE})
	public void rename(CommandEvent event, @Argument(value="user") String userArgument, @Argument(value="nickname", endless=true, nullDefault=true) String nickname) {
		Member member = ArgumentUtility.getMember(event.getGuild(), userArgument);
		if (member == null) {
			event.reply("I could not find that user :no_entry:").queue();
			return;
		}
		
		if (nickname.length() > 32) {
			event.reply("Nicknames cannot be any longer than 32 characters :no_entry:").queue();
			return;
		}
		
		if (!event.getMember().canInteract(member)) {
			event.reply("You cannot rename a user with a higher or equal top role than yours :no_entry:").queue();
			return;
		}
		
		if (!event.getSelfMember().canInteract(member)) {
			event.reply("I cannot rename a user with a higher or equal top role than me :no_entry:").queue();
			return;
		}
		
		if (!member.equals(event.getMember())) {
			if (!member.hasPermission(Permission.NICKNAME_MANAGE)) {
				event.reply("You need the Manage Nicknames permission to rename someone else :no_entry:").queue();
				return;
			}
		}
		
		event.getGuild().modifyNickname(member, nickname).queue($ -> {
			event.reply("Renamed **" + member.getUser().getAsTag() + "** to `" + nickname + "`").queue();
		});
	}
	
	@Command(value="add role", aliases={"ar", "addrole"}, description="Add a role to a user")
	@AuthorPermissions({Permission.MANAGE_ROLES})
	@BotPermissions({Permission.MANAGE_ROLES})
	public void addRole(CommandEvent event, @Argument(value="user") String userArgument, @Argument(value="role", endless=true) String roleArgument) {
		Member member = ArgumentUtility.getMember(event.getGuild(), userArgument);
		if (member == null) {
			event.reply("I could not find that user :no_entry:").queue();
			return;
		}
		
		Role role = ArgumentUtility.getRole(event.getGuild(), roleArgument);
		if (role == null) {
			event.reply("I could not find that role :no_entry:").queue();
			return;
		}
		
		if (role.isManaged()) {
			event.reply("I cannot give a managed role :no_entry:").queue();
			return;
		}
		
		if (!event.getMember().canInteract(role)) {
			event.reply("You cannot add a role which is higher or equal than your top role :no_entry:").queue();
			return;
		}
		
		if (!event.getSelfMember().canInteract(role)) {
			event.reply("I cannot add a role which is higher or equal than my top role :no_entry:").queue();
			return;
		}
		
		if (member.getRoles().contains(role)) {
			event.reply("That user already has that role :no_entry:").queue();
			return;
		}
		
		event.getGuild().addRoleToMember(member, role).queue($ -> {
			event.reply("**" + member.getUser().getAsTag() + "** now has the role `" + role.getName() + "`").queue();
		});
	}
	
	@Command(value="remove role", aliases={"rr", "removerole"}, description="Remove a role to a user")
	@AuthorPermissions({Permission.MANAGE_ROLES})
	@BotPermissions({Permission.MANAGE_ROLES})
	public void removeRole(CommandEvent event, @Argument(value="user") String userArgument, @Argument(value="role", endless=true) String roleArgument) {
		Member member = ArgumentUtility.getMember(event.getGuild(), userArgument);
		if (member == null) {
			event.reply("I could not find that user :no_entry:").queue();
			return;
		}
		
		Role role = ArgumentUtility.getRole(event.getGuild(), roleArgument);
		if (role == null) {
			event.reply("I could not find that role :no_entry:").queue();
			return;
		}
		
		if (role.isManaged()) {
			event.reply("I cannot remove a managed role :no_entry:").queue();
			return;
		}
		
		if (!event.getMember().canInteract(role)) {
			event.reply("You cannot remove a role which is higher or equal than your top role :no_entry:").queue();
			return;
		}
		
		if (!event.getSelfMember().canInteract(role)) {
			event.reply("I cannot remove a role which is higher or equal than my top role :no_entry:").queue();
			return;
		}
		
		if (!member.getRoles().contains(role)) {
			event.reply("That user doesn't have that role :no_entry:").queue();
			return;
		}
		
		event.getGuild().removeRoleFromMember(member, role).queue($ -> {
			event.reply("**" + member.getUser().getAsTag() + "** no longer has the role `" + role.getName() + "`").queue();
		});
	}
	
	@Command(value="create role", aliases={"cr", "createrole"}, description="Crates a role in the current server")
	@AuthorPermissions({Permission.MANAGE_ROLES})
	@BotPermissions({Permission.MANAGE_ROLES})
	public void createRole(CommandEvent event, @Argument(value="name") String roleName, @Argument(value="hex", nullDefault=true) String hex, @Argument(value="hoisted", nullDefault=true) Boolean hoisted,
			@Argument(value="mentionable", nullDefault=true) Boolean mentionable, @Argument(value="permissions", nullDefault=true) Long permissions) {
		if (roleName.length() > 100) {
			event.reply("Role names can be no longer than 100 characters :no_entry:").queue();
			return;
		}
		
		if (event.getGuild().getRoles().size() >= 250) {
			event.reply("The server already has the max amount of roles it can have (250) :no_entry:").queue();
			return;
		}
		
		Color roleColour = null;
		if (hex != null) {
			hex = hex.startsWith("#") ? hex : "#" + hex;
			try {
				roleColour = Color.decode(hex);
			} catch(NumberFormatException e) {
				event.reply("The hex code provided was invalid :no_entry:").queue();
				return;
			}
		}
		
		boolean roleHoist = false;
		if (hoisted != null) {
			roleHoist = hoisted;
		}
		
		boolean roleMention = false;
		if (mentionable != null) {
			roleMention = mentionable;
		}
		
		event.getGuild().createRole().setName(roleName).setColor(roleColour).setHoisted(roleHoist).setMentionable(roleMention).setPermissions(permissions).queue(role -> {
			event.reply("`" + role.getName() + "` has been created").queue();
		});
	}
	
	@Command(value="delete role", aliases={"dr", "deleterole"}, description="Deletes a role in the current server")
	@AuthorPermissions({Permission.MANAGE_ROLES})
	@BotPermissions({Permission.MANAGE_ROLES})
	public void deleteRole(CommandEvent event, @Argument(value="role", endless=true) String roleArgument) {
		Role role = ArgumentUtility.getRole(event.getGuild(), roleArgument);
		if (role == null) {
			event.reply("I could not find that role :no_entry:").queue();
			return;
		}
		
		if (role.isManaged()) {
			event.reply("I cannot delete a managed role :no_entry:").queue();
			return;
		}
		
		if (!event.getMember().canInteract(role)) {
			event.reply("You cannot delete a role which is higher or equal than your top role :no_entry:").queue();
			return;
		}
		
		if (!event.getSelfMember().canInteract(role)) {
			event.reply("I cannot delete a role which is higher or equal than my top role :no_entry:").queue();
			return;
		}
		
		role.delete().queue($ -> {
			event.reply("`" + role.getName() + "` has been deleted").queue();
		});
	}
	
	@Command(value="lockdown", description="Lockdown a specific channel so no one without overrides can speak in the channel")
	@AuthorPermissions({Permission.MANAGE_SERVER})
	@BotPermissions({Permission.MANAGE_PERMISSIONS})
	public void lockdown(CommandEvent event, @Argument(value="channel", endless=true, nullDefault=true) String channelArgument) {
		TextChannel channel;
		if (channelArgument == null) {
			channel = event.getTextChannel();
		} else {
			channel = ArgumentUtility.getTextChannel(event.getGuild(), channelArgument);
			if (channel == null) {
				event.reply("I could not find that text channel :no_entry:").queue();
				return;
			}
		}
		
		PermissionOverride channelOverride = channel.getPermissionOverride(event.getGuild().getPublicRole());
		EnumSet<Permission> allowedPermissions = channelOverride == null ? EnumSet.noneOf(Permission.class) : channelOverride.getAllowed();
		EnumSet<Permission> deniedPermissions = channelOverride == null ? EnumSet.noneOf(Permission.class) : channelOverride.getDenied();
		if (deniedPermissions.contains(Permission.MESSAGE_WRITE)) {
			deniedPermissions.remove(Permission.MESSAGE_WRITE);
			channel.putPermissionOverride(event.getGuild().getPublicRole()).setPermissions(allowedPermissions, deniedPermissions).queue($ -> {
				event.reply(channel.getAsMention() + " is no longer locked down").queue();
			});
		} else {
			deniedPermissions.add(Permission.MESSAGE_WRITE);
			channel.putPermissionOverride(event.getGuild().getPublicRole()).setPermissions(allowedPermissions, deniedPermissions).queue($ -> {
				event.reply(channel.getAsMention() + " is now locked down").queue();
			});
		}
	}
	
}