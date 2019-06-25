package com.jsb.bot.utility;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message.MentionType;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.internal.requests.EmptyRestAction;

public class ArgumentUtility {
	
	private static final Pattern idRegex = Pattern.compile("(\\d+)");
	private static final Pattern tagRegex = Pattern.compile("(.{2,32})#(\\d{4})");

	public static Role getRole(Guild guild, String argument) {
		if (argument.length() <= 100) {
			try {
				Matcher idMatch = idRegex.matcher(argument);
				Matcher mentionMatch = MentionType.ROLE.getPattern().matcher(argument);
				if (idMatch.matches()) {
					guild.getRoleById(idMatch.group(1));
				} else if (mentionMatch.matches()) {
					guild.getRoleById(mentionMatch.group(1));
				} else {
					List<Role> roles = guild.getRolesByName(argument, true);
					if (!roles.isEmpty()) {
						return roles.get(0);
					}
				}
			} catch(NumberFormatException e) {}
		}
		
		return null;
	}
	
	public static Member getMember(Guild guild, String argument) {
		try {
			Matcher idMatch = idRegex.matcher(argument);
			Matcher mentionMatch = MentionType.USER.getPattern().matcher(argument);
			Matcher tagMatch = tagRegex.matcher(argument);
			if (idMatch.matches()) {
				guild.getMemberById(idMatch.group(1));
			} else if (mentionMatch.matches()) {
				guild.getMemberById(mentionMatch.group(1));
			} else if (tagMatch.matches()) {
				String name = tagMatch.group(1).toLowerCase();
				String discriminator = tagMatch.group(2);
				for (Member member : guild.getMembers()) {
					if (member.getUser().getName().toLowerCase().equals(name) && member.getUser().getDiscriminator().equals(discriminator)) {
						return member;
					}
				}
			} else {
				List<Member> effectiveNameMembers = guild.getMembersByEffectiveName(argument, true);
				if (effectiveNameMembers.isEmpty()) {
					List<Member> nameMembers = guild.getMembersByName(argument, true);
					if (!nameMembers.isEmpty()) {
						return nameMembers.get(0);
					}
				} else {
					return effectiveNameMembers.get(0);
				}
			}
		} catch(NumberFormatException e) {}
		
		return null;
	}
	
	public static User getUser(ShardManager shardManager, String argument) {
		try {
			Matcher idMatch = idRegex.matcher(argument);
			Matcher mentionMatch = MentionType.USER.getPattern().matcher(argument);
			Matcher tagMatch = tagRegex.matcher(argument);
			if (idMatch.matches()) {
				shardManager.getUserById(idMatch.group(1));
			} else if (mentionMatch.matches()) {
				shardManager.getUserById(mentionMatch.group(1));
			} else if (tagMatch.matches()) {
				String name = tagMatch.group(1).toLowerCase();
				String discriminator = tagMatch.group(2);
				for (User user : shardManager.getUsers()) {
					if (user.getName().toLowerCase().equals(name) && user.getDiscriminator().equals(discriminator)) {
						return user;
					}
				}
			} else {
				argument = argument.toLowerCase();
				for (User user : shardManager.getUsers()) {
					if (user.getName().toLowerCase().equals(argument)) {
						return user;
					}
				}
			}
		} catch(NumberFormatException e) {}
		
		return null;
	}
	
	public static RestAction<User> retrieveUser(ShardManager shardManager, String argument) {
		try {
			Matcher idMatch = idRegex.matcher(argument);
			Matcher mentionMatch = MentionType.USER.getPattern().matcher(argument);
			if (idMatch.matches()) {
				return shardManager.retrieveUserById(idMatch.group(1));
			} else if (mentionMatch.matches()) {
				return shardManager.retrieveUserById(mentionMatch.group(1));
			}
		} catch(NumberFormatException e) {}
		
		return new EmptyRestAction<User>(shardManager.getShardCache().getElementById(0), null);
	}
	
}
