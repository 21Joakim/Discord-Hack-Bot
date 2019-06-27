package com.jsb.bot.utility;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.bson.Document;

import com.jockie.bot.core.command.impl.CommandEvent;
import com.jsb.bot.database.Database;
import com.mongodb.client.model.Projections;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;

public class CheckUtility {

	public static boolean checkPermissions(CommandEvent event) {
		if (event.isAuthorDeveloper() || event.getGuild().getOwner().equals(event.getMember()) || event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
			return true;
		} else {
			EnumSet<Permission> userPermissions = event.getMember().getPermissions(event.getTextChannel());
			Document data = Database.get().getGuildById(event.getGuild().getIdLong(), null, Projections.include("botPermissions.roles", "botPermissions.users"));
			Document permissionsData = data.get("botPermissions", Document.class);
			if (permissionsData != null) {
				List<Document> roles = permissionsData.getList("roles", Document.class, Collections.emptyList());
				List<Document> users = permissionsData.getList("users", Document.class, Collections.emptyList());
				
				long permissionsRaw = 0L;
				for (Role role : event.getMember().getRoles()) {
					for (Document roleData : roles) {
						if (roleData.getLong("id") == role.getIdLong()) {
							permissionsRaw |= roleData.getLong("permissions");
						}
					}
				}
				
				for (Document userData : users) {
					if (event.getMember().getIdLong() == userData.getLong("id")) {
						permissionsRaw |= userData.getLong("permissions");
					}
				}
				
				userPermissions.addAll(Permission.getPermissions(permissionsRaw));
			}
			
			if (userPermissions.contains(Permission.ADMINISTRATOR)) {
				return true;
			} else {
				EnumSet<Permission> missingPermissions = EnumSet.noneOf(Permission.class);
				for (Permission neededPermission : event.getCommand().getAuthorDiscordPermissions()) {
					if (!userPermissions.contains(neededPermission)) {
						missingPermissions.add(neededPermission);
					}
				}
				
				if (!missingPermissions.isEmpty()) {
					event.reply("You are missing the permission" + (missingPermissions.size() == 1 ? "" : "s") + " `" + MiscUtility.join(missingPermissions, "`, `") + "` to execute this command :no_entry:").queue();
					return false;
				}
			}
		}
		
		return true;
	}
	
}
