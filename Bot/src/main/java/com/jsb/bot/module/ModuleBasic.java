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
import net.dv8tion.jda.api.entities.VoiceChannel;

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
		
		@Command(value="user", aliases={"member"}, description="Prune a set amount of messages from a specified user")
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
	
	@Command(value="kick", description="Kick a user from the server")
	@AuthorPermissions({Permission.KICK_MEMBERS})
	@BotPermissions({Permission.KICK_MEMBERS})
	public void kick(CommandEvent event, @Argument(value="member") String memberArgument, @Argument(value="reason", endless=true, nullDefault=true) String reason) {
		Member member = ArgumentUtility.getMemberByIdOrTag(event.getGuild(), memberArgument, true);
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
		
		if (!event.getSelfMember().canInteract(member)) {
			event.reply("I cannot kick a user with a higher top role than me :no_entry:").queue();
			return;
		}
		
		event.getGuild().kick(member).reason((reason == null ? "" : reason) + " [" + event.getAuthor().getAsTag() + "]").queue();
		event.reply("**" + member.getUser().getAsTag() + "** has been kicked").queue();
	}
	
	@Command(value="ban", description="Ban a user from the server")
	@AuthorPermissions({Permission.BAN_MEMBERS})
	@BotPermissions({Permission.BAN_MEMBERS})
	public void ban(CommandEvent event, @Argument(value="member") String memberArgument, @Argument(value="reason", endless=true, nullDefault=true) String reason) {
		Member member = ArgumentUtility.getMemberByIdOrTag(event.getGuild(), memberArgument, true);
		if (member == null) {
			event.reply("I could not find that user :no_entry:").queue();
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
		
		if (!event.getSelfMember().canInteract(member)) {
			event.reply("I cannot ban a user with a higher top role than me :no_entry:").queue();
			return;
		}
		
		event.getGuild().ban(member, 1).reason((reason == null ? "" : reason) + " [" + event.getAuthor().getAsTag() + "]").queue();
		event.reply("**" + member.getUser().getAsTag() + "** has been banned").queue();
	}
	
	@Command(value="voice kick", aliases={"voicekick", "disconnect", "dc"}, description="Disconnect a user from the voice channel they are currently in")
	@AuthorPermissions({Permission.VOICE_MOVE_OTHERS})
	@BotPermissions({Permission.VOICE_MOVE_OTHERS})
	public void voiceKick(CommandEvent event, @Argument(value="member") String memberArgument, @Argument(value="reason", endless=true, nullDefault=true) String reason) {
		Member member = ArgumentUtility.getMemberByIdOrTag(event.getGuild(), memberArgument, true);
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
		
		event.getGuild().moveVoiceMember(member, null).queue();
		event.reply("**" + member.getUser().getAsTag() + "** has been disconnected from " + channel.getName()).queue();
	}
	
	@Command(value="rename", aliases={"nick", "nickname", "set nick", "setnick", "set nickname", "setnickname"}, description="Set a users nickname")
	@AuthorPermissions({Permission.NICKNAME_CHANGE})
	@BotPermissions({Permission.NICKNAME_MANAGE})
	public void rename(CommandEvent event, @Argument(value="member") String memberArgument, @Argument(value="nickname", endless=true, nullDefault=true) String nickname) {
		Member member = ArgumentUtility.getMemberByIdOrTag(event.getGuild(), memberArgument, true);
		if (member == null) {
			event.reply("I could not find that user :no_entry:").queue();
			return;
		}
		
		if (nickname.length() > 32) {
			event.reply("Nicknames cannot be any longer than 32 characters :no_entry:").queue();
			return;
		}
		
		if (!event.getSelfMember().canInteract(member)) {
			event.reply("I cannot rename a user with a higher top role than me").queue();
			return;
		}
		
		if (!member.equals(event.getMember())) {
			if (!member.hasPermission(Permission.NICKNAME_MANAGE)) {
				event.reply("You need the Manage Nicknames permission to rename someone else :no_entry:").queue();
				return;
			}
		}
		
		event.getGuild().modifyNickname(member, nickname).queue();
		event.reply("Renamed **" + member.getUser().getAsTag() + "** to `" + nickname + "`").queue();
	}
	
}