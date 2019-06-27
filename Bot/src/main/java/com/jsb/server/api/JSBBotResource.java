package com.jsb.server.api;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.bson.Document;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.json.JSONObject;

import com.jsb.bot.core.JSBBot;
import com.jsb.bot.database.Database;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

@Path("/bot")
public class JSBBotResource {
	
	@GET
	@Path("/ping")
	public Response ping() {
		return Response.ok("pong").build();
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/guild/{guildId}/data")
	public Response guildData(@HeaderParam("Authorization") String auth, @PathParam("guildId") String guildIdStr) {
		Long id = UserResource.verify(auth);
		if(id != null) {
			long guildId;
			try {
				guildId = Long.valueOf(guildIdStr);
			}catch(NumberFormatException e) {
				return Response.status(Status.BAD_REQUEST).entity(new JSONObject()
					.put("success", false)
					.put("error", "Invalid guild id")
					.put("code", Status.BAD_REQUEST.getStatusCode())
				).build();
			}
			
			Guild guild = JSBBot.getShardManager().getGuildById(guildId);
			User user = JSBBot.getShardManager().getUserById(id);
			
			if(guild == null || user == null || !guild.isMember(user)) {
				return Response.status(Status.UNAUTHORIZED).entity(new JSONObject()
					.put("success", false)
					.put("error", "You are not a part of that guild")
					.put("code", Status.UNAUTHORIZED.getStatusCode())
				).build();
			}
			
			if(!guild.getMember(user).hasPermission(Permission.MANAGE_SERVER)) {
				return Response.status(Status.UNAUTHORIZED).entity(new JSONObject()
					.put("success", false)
					.put("error", "You do not have the MANAGE_SERVER permission")
					.put("code", Status.UNAUTHORIZED.getStatusCode())
				).build();
			}
			
			Document guildData = Database.get().getGuildById(guildId);
			JSONObject data = new JSONObject(guildData.toJson(JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build()));
			
			return Response.ok(new JSONObject()
				.put("success", true)
				.put("data", data)
			).build();
		}else{
			return Response.status(Status.UNAUTHORIZED).entity(new JSONObject()
				.put("success", false)
				.put("error", "Missing or invalid authorization")
				.put("code", Status.UNAUTHORIZED.getStatusCode())
			).build();
		}
	}
}