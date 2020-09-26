package com.jsb.server.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import com.jsb.bot.core.JSBBot;
import com.jsb.bot.database.Database;
import com.jsb.bot.logger.LoggerType;
import com.jsb.bot.module.ModuleDeveloper;
import com.jsb.bot.utility.LoggerUtility;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

@Path("/bot")
public class JSBBotResource {
	
	private static class GuildAuthorization {
		
		private Response response;
		private long guildId;
		private long userId;
		
		public GuildAuthorization(Response response) {
			this.response = response;
		}
		
		public GuildAuthorization(long guildId, long userId) {
			this.guildId = guildId;
			this.userId = userId;
		}
		
		public boolean isAuthorized() {
			return this.response == null;
		}
		
		public Guild getGuild() {
			return JSBBot.getShardManager().getGuildById(this.guildId);
		}
		
		public User getUser() {
			return JSBBot.getShardManager().getUserById(this.userId);
		}
	}
	
	private GuildAuthorization ensureAuthorized(String token, String guildIdStr) {
		Long id = UserResource.verify(token);
		if(id != null) {
			long guildId;
			try {
				guildId = Long.valueOf(guildIdStr);
			}catch(NumberFormatException e) {
				return new GuildAuthorization(Response.status(Status.BAD_REQUEST).entity(new JSONObject()
					.put("success", false)
					.put("error", "Invalid guild id")
					.put("code", Status.BAD_REQUEST.getStatusCode())
				).build());
			}
			
			Guild guild = JSBBot.getShardManager().getGuildById(guildId);
			User user = JSBBot.getShardManager().getUserById(id);
			
			if(guild == null || user == null || !guild.isMember(user)) {
				return new GuildAuthorization(Response.status(Status.UNAUTHORIZED).entity(new JSONObject()
					.put("success", false)
					.put("error", "You are not a part of that guild")
					.put("code", Status.UNAUTHORIZED.getStatusCode())
				).build());
			}
			
			if(!guild.getMember(user).hasPermission(Permission.MANAGE_SERVER)) {
				return new GuildAuthorization(Response.status(Status.UNAUTHORIZED).entity(new JSONObject()
					.put("success", false)
					.put("error", "You do not have the MANAGE_SERVER permission")
					.put("code", Status.UNAUTHORIZED.getStatusCode())
				).build());
			}
			
			return new GuildAuthorization(guild.getIdLong(), user.getIdLong());
		}else{
			return new GuildAuthorization(Response.status(Status.UNAUTHORIZED).entity(new JSONObject()
				.put("success", false)
				.put("error", "Missing or invalid authorization")
				.put("code", Status.UNAUTHORIZED.getStatusCode())
			).build());
		}
	}
	
	@GET
	@Path("/ping")
	public Response ping() {
		return Response.ok("pong").build();
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/guild/{guildId}/data")
	public Response guildData(@HeaderParam("Authorization") String token, @PathParam("guildId") String guildIdStr) {
		GuildAuthorization auth = this.ensureAuthorized(token, guildIdStr);
		if(auth.isAuthorized()) {
			Document guildData = Database.get().getGuildById(auth.guildId);
			JSONObject data = new JSONObject(guildData.toJson(JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build()));
			
			return Response.ok(new JSONObject()
				.put("success", true)
				.put("data", data)
			).build();
		}else{
			return auth.response;
		}
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/guild/{guildId}/logger")
	public Response logger(@HeaderParam("Authorization") String token, @PathParam("guildId") String guildIdStr) {
		GuildAuthorization auth = this.ensureAuthorized(token, guildIdStr);
		if(auth.isAuthorized()) {
			JSONObject data = new JSONObject();
			
			JSONArray loggerTypes = new JSONArray();
			for(LoggerType type : LoggerType.values()) {
				JSONObject loggerType = new JSONObject()
					.put("type", type)
					/* Temporary */
					.put("name", type.toString().replace("_", " ").toLowerCase());
				
				loggerTypes.put(loggerType);
			}
			
			JSONArray loggersData = new JSONArray();
			
			List<Document> loggers = Database.get().getGuildById(auth.guildId, null, Projections.include("logger.loggers")).getEmbedded(List.of("logger", "loggers"), Collections.emptyList());
			for(Document logger : loggers) {
				JSONObject loggerData = new JSONObject();
				
				long channelId = logger.getLong("channelId");
				TextChannel channel = JSBBot.getShardManager().getTextChannelById(channelId);
				
				loggerData.put("channel", new JSONObject()
					.put("id", String.valueOf(channelId))
					.put("name", channel.getName()));
				
				loggerData.put("enabled", logger.getBoolean("enabled"));
				
				EnumSet<LoggerType> types = LoggerUtility.getEnabledEvents(logger);
				
				loggerData.put("events", types);
				
				loggersData.put(loggerData);
			}
			
			data.put("types", loggerTypes);
			data.put("loggers", loggersData);
			
			return Response.ok(new JSONObject().put("success", true).put("data", data)).build();
		}else{
			return auth.response;
		}
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/guild/{guildId}/warnings")
	public Response warnings(@HeaderParam("Authorization") String token, @PathParam("guildId") String guildIdStr, @QueryParam("from") String from, @QueryParam("limit") Integer limit) {
		if(limit == null || limit <= 0 || limit >= 100) {
			limit = 10;
		}
		
		GuildAuthorization auth = this.ensureAuthorized(token, guildIdStr);
		if(auth.isAuthorized()) {
			Bson filter;
			if(from != null) {
				filter = Filters.and(Filters.eq("guildId", auth.guildId), Filters.gt("_id", new ObjectId(from)));
			}else{
				filter = Filters.eq("guildId", auth.guildId);
			}
			
			List<Document> warnings = Database.get().getWarnings()
				.find(filter)
				.limit(limit)
				.into(new ArrayList<>());
			
			if(warnings.size() == 0) {
				return Response.ok(new JSONObject().put("success", true).put("data", new JSONObject().put("warnings", new JSONArray()).put("paginationId", JSONObject.NULL))).build();
			}
			
			JSONArray jsonWarnings = new JSONArray();
			
			for(Document warning : warnings) {
				try {
					long moderatorId = warning.getLong("moderatorId");
					User moderator = JSBBot.getShardManager().getUserById(moderatorId);
					
					long userId = warning.getLong("userId");
					User targetUser = JSBBot.getShardManager().getUserById(userId);
					
					JSONObject jsonWarning = new JSONObject()
						.put("id", warning.getObjectId("_id").toString())
						.put("moderator", new JSONObject()
							.put("id", String.valueOf(moderatorId))
							.put("name", moderator != null ? moderator.getName() : null)
							.put("discriminator", moderator != null ? moderator.getDiscriminator() : null))
						.put("user", new JSONObject()
							.put("id", String.valueOf(userId))
							.put("name", targetUser != null ? targetUser.getName() : null)
							.put("discriminator", targetUser != null ? targetUser.getDiscriminator() : null))
						.put("createdAt", warning.getLong("createdAt"))
						.put("reason", warning.getString("reason"))
						.put("worth", warning.getInteger("worth"));
					
					jsonWarnings.put(jsonWarning);
				}catch(Exception e) {
					e.printStackTrace();
				}
			}
			
			String lastId = jsonWarnings.getJSONObject(jsonWarnings.length() - 1).getString("id");
			
			return Response.ok(new JSONObject().put("success", true).put("data", new JSONObject().put("warnings", jsonWarnings).put("paginationId", lastId))).build();
		}else{
			return auth.response;
		}
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/guild/{guildId}/modlogs")
	public Response modlogs(@HeaderParam("Authorization") String token, @PathParam("guildId") String guildIdStr, @QueryParam("from") String from, @QueryParam("limit") Integer limit) {
		if(limit == null || limit <= 0 || limit >= 100) {
			limit = 10;
		}
		
		GuildAuthorization auth = this.ensureAuthorized(token, guildIdStr);
		if(auth.isAuthorized()) {
			Bson filter;
			if(from != null) {
				filter = Filters.and(Filters.eq("guildId", auth.guildId), Filters.gt("_id", new ObjectId(from)));
			}else{
				filter = Filters.eq("guildId", auth.guildId);
			}
			
			List<Document> modlogCases = Database.get().getModlogCases()
				.find(filter)
				.limit(limit)
				.into(new ArrayList<>());
			
			if(modlogCases.size() == 0) {
				return Response.ok(new JSONObject().put("success", true).put("data", new JSONObject().put("modlogs", new JSONArray()).put("paginationId", JSONObject.NULL))).build();
			}
			
			JSONArray jsonModlogCases = new JSONArray();
			
			for(Document modlogCase : modlogCases) {
				try {
					long moderatorId = modlogCase.getLong("moderatorId");
					User moderator = JSBBot.getShardManager().getUserById(moderatorId);
					
					long userId = modlogCase.getLong("userId");
					User targetUser = JSBBot.getShardManager().getUserById(userId);
					
					JSONObject jsonModlogCase = new JSONObject()
						.put("id", modlogCase.getObjectId("_id").toString())
						.put("caseId", modlogCase.getLong("id"))
						.put("moderator", new JSONObject()
							.put("id", String.valueOf(moderatorId))
							.put("name", moderator != null ? moderator.getName() : null)
							.put("discriminator", moderator != null ? moderator.getDiscriminator() : null))
						.put("user", new JSONObject()
							.put("id", String.valueOf(userId))
							.put("name", targetUser != null ? targetUser.getName() : null)
							.put("discriminator", targetUser != null ? targetUser.getDiscriminator() : null))
						.put("createdAt", modlogCase.getLong("createdAt"))
						.put("reason", modlogCase.getString("reason"))
						.put("action", modlogCase.getString("action"))
						.put("automatic", modlogCase.getBoolean("automatic"));
					
					jsonModlogCases.put(jsonModlogCase);
				}catch(Exception e) {
					e.printStackTrace();
				}
			}
			
			String lastId = jsonModlogCases.getJSONObject(jsonModlogCases.length() - 1).getString("id");
			
			return Response.ok(new JSONObject().put("success", true).put("data", new JSONObject().put("modlogs", jsonModlogCases).put("paginationId", lastId))).build();
		}else{
			return auth.response;
		}
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/commands")
	public Response commands() {
		return Response.ok()
			.entity(new JSONObject()
				.put("success", true)
				.put("data", new JSONObject()
					.put("commands", ModuleDeveloper.getAllCommandsAsJson(JSBBot.getCommandListener()))))
			.build();
	}
	
	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/guild/{guildId}/warnings/{warningId}")
	public Response deleteWarning(@HeaderParam("Authorization") String token, @PathParam("guildId") String guildIdStr, @PathParam("warningId") String warningId) {
		GuildAuthorization auth = this.ensureAuthorized(token, guildIdStr);
		if(auth.isAuthorized()) {
			Document document = Database.get().getWarnings().find(Filters.and(Filters.eq("guildId", auth.guildId), Filters.eq("_id", new ObjectId(warningId)))).first();
			if(document == null) {
				return Response.status(Status.BAD_REQUEST).entity(new JSONObject().put("success", false).put("error", "Invalid warning")).build();
			}
			
			Database.get().getWarnings().deleteOne(Filters.eq("_id", new ObjectId(warningId)));
			
			return Response.ok().entity(new JSONObject().put("success", true)).build();
		}else{
			return auth.response;
		}
	}
}