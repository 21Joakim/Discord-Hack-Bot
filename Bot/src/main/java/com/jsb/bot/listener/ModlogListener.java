package com.jsb.bot.listener;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.bson.Document;

import com.jsb.bot.command.CommandModlog.ModlogAction;
import com.jsb.bot.database.Database;
import com.mongodb.client.model.Projections;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

public class ModlogListener {

	public static void createModlog(Guild guild, User moderator, User user, String reason, boolean automatic, ModlogAction action) {
		Database.get().getGuildById(guild.getIdLong(), null, Projections.include("modlog.enabled", "modlog.channel", "modlog.disabledActions"), (data, readException) -> {
			if (readException != null) {
				readException.printStackTrace();
			} else {
				Document modlogData = data.getEmbedded(List.of("modlog"), new Document());
				boolean enabled = modlogData.getBoolean("enabled", false);
				if (enabled) {
					List<String> disabledActions = modlogData.getList("disabledActions", String.class);
					if (!disabledActions.contains(action.toString())) {
						long caseId = Database.get().getModlogCasesAmountFromGuild(guild.getIdLong()) + 1;
						TextChannel channel = guild.getTextChannelById(modlogData.getLong("channel"));
						if (channel != null) {
							EmbedBuilder embed = new EmbedBuilder();
							embed.setTitle("Case " + caseId + " | " + action.getName());
							embed.addField("Moderator", moderator.getAsTag() + " (" + moderator.getId() + ")", false);
							embed.addField("User", user.getAsTag() + " (" + user.getId() + ")", false);
							embed.addField("Reason", reason == null ? "None Given" : reason, false);
							embed.setTimestamp(Instant.now());
							
							if (guild.getSelfMember().hasPermission(channel, Permission.MESSAGE_EMBED_LINKS) && guild.getSelfMember().hasPermission(channel, Permission.MESSAGE_WRITE)) {
								channel.sendMessage(embed.build()).queue();
							}
						}
						
						Document newCase = new Document()
								.append("guildId", guild.getIdLong())
								.append("moderator", moderator.getIdLong())
								.append("user", user.getIdLong())
								.append("createdAt", Clock.systemUTC().instant().getEpochSecond())
								.append("id", caseId)
								.append("reason", reason)
								.append("automatic", automatic);
						
						Database.get().insertModlogCase(newCase);
					}
				}
			}
		});
	}
	
}
