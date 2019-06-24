package com.jsb.bot.module;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.jockie.bot.core.argument.Argument;
import com.jockie.bot.core.command.Command;
import com.jockie.bot.core.command.Command.AuthorPermissions;
import com.jockie.bot.core.command.Command.BotPermissions;
import com.jockie.bot.core.command.impl.CommandEvent;
import com.jockie.bot.core.command.impl.CommandImpl;
import com.jockie.bot.core.module.Module;
import com.jockie.bot.core.utility.ArgumentUtility;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;

@Module
public class ModuleBasic {
	
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
		
		@Command(value="user", description="Prune a set amount of messages from a specified user")
		@AuthorPermissions({Permission.MESSAGE_MANAGE})
		@BotPermissions({Permission.MESSAGE_MANAGE, Permission.MESSAGE_HISTORY})
		public void user(CommandEvent event, @Argument(value="member") String memberArgument, @Argument(value="amount", nullDefault=true) Integer amount) {
			int limit = amount == null ? 100 : amount;
			
			if (limit > 100) {
				event.reply("You can only prune up to 100 messages :no_entry:").queue();
				return;
			}
			
			if (limit < 1) {
				event.reply("You have to prune at least 1 message :no_entry:").queue();
				return;
			}
			
			Member member = ArgumentUtility.getMemberByIdOrTag(event.getGuild(), memberArgument, true);
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
}