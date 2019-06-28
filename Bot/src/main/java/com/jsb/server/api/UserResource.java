package com.jsb.server.api;

import java.io.IOException;
import java.time.Clock;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.jsb.bot.core.JSBBot;
import com.jsb.bot.database.Database;
import com.mongodb.client.model.Updates;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;

@Path("/user")
public class UserResource {
	
	private OkHttpClient client = new OkHttpClient();
	
	public static Long verify(String token) {
		if(token == null) {
			return null;
		}
		
		DecodedJWT decoded = JWT.decode(token);
		long id = decoded.getClaim("id").asLong();
		
		JSONArray secrets = JSBBot.getConfig("webserver.jwtSecrets", JSONArray.class);
		String secret = secrets.getString((int) (id % secrets.length()));
		
		try {
			JWT.require(Algorithm.HMAC256(secret)).build().verify(decoded);
		}catch(JWTVerificationException e) {
			return null;
		}
		
		return id;
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/me")
	public Response me(@HeaderParam("Authorization") String authorization, @QueryParam("force") boolean force) {
		Long id = UserResource.verify(authorization);
		if(id != null) {
			User user = JSBBot.getShardManager().getUserById(id);
			JSONObject data = new JSONObject();
			
			if(user != null && !force) {
				data.put("id", user.getId())
					.put("name", user.getName())
					.put("discriminator", user.getDiscriminator())
					.put("avatarUrl", user.getEffectiveAvatarUrl());
					
				JSONArray guilds = new JSONArray();
				
				for(Guild guild : JSBBot.getShardManager().getMutualGuilds(user)) {
					if(guild.getMember(user).hasPermission(Permission.MANAGE_SERVER)) {
						guilds.put(new JSONObject()
							.put("name", guild.getName())
							.put("iconUrl", guild.getIconUrl()));
					}
				}
				
				data.put("guilds", guilds);
			}else{
				/* TODO: Probably shouldn't make requests to Discord every /me request */
				
				Document dashboardData = Database.get().getDashboardUser(id);
				
				/* TODO: Check if token needs to be refreshed */
				
				Request request = new Request.Builder()
					.url("https://discordapp.com/api/users/@me")
					.header("Authorization", "Bearer " + dashboardData.getString("accessToken"))
					.build();
				
				try(okhttp3.Response response = this.client.newCall(request).execute()) {
					if(response.isSuccessful()) {
						JSONObject me = new JSONObject(response.body().string());
						System.out.println(me);
						
						data.put("id", me.getString("id"))
							.put("name", me.getString("username"))
							.put("discriminator", me.getString("discriminator"))
							.put("avatarUrl", "https://cdn.discordapp.com/avatars/" + me.getString("id") + "/" + me.getString("avatar"))
							.put("guilds", new JSONArray());
					}else{
						System.err.println("Failed to get me: " + new JSONObject(response.body().string()));
					}
				}catch(IOException e) {
					e.printStackTrace();
				}
			}
			
			return Response.ok(new JSONObject().put("success", true).put("data", data)).build();
		}else{
			return Response.status(Status.UNAUTHORIZED).entity(new JSONObject()
				.put("success", false)
				.put("error", "Missing or invalid authorization")
				.put("code", Status.UNAUTHORIZED.getStatusCode())
			).build();
		}
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/auth")
	public Response auth(@QueryParam("code") String code) {
		if(code == null || code.trim().isEmpty()) {
			return Response.status(400).entity("Missing code").build();
		}
		
		// https://discordapp.com/api/oauth2/authorize?client_id=593709124261511170&redirect_uri=http%3A%2F%2Fjockie.ddns.net%3A8080%2Fapi%2Fuser%2Fauth&response_type=code&scope=identify%20guilds
		
		Request request = new Request.Builder()
			.url("https://discordapp.com/api/v6/oauth2/token")
			.post(new FormBody.Builder()
				.add("client_id", JSBBot.getConfig("clientId", String.class))
				.add("client_secret", JSBBot.getConfig("clientSecret", String.class))
				.add("grant_type", "authorization_code")
				.add("code", code)
				.add("redirect_uri", "http://jockie.ddns.net:8080/api/user/auth")
				.add("scope", "identify guilds")
				.build())
			.build();
		
		try(okhttp3.Response response = this.client.newCall(request).execute()) {
			if(response.isSuccessful()) {
				JSONObject data = new JSONObject(response.body().string());
				
				request = new Request.Builder()
					.url("https://discordapp.com/api/users/@me")
					.header("Authorization", "Bearer " + data.getString("access_token"))
					.build();
				
				try(okhttp3.Response meResponse = this.client.newCall(request).execute()) {
					if(meResponse.isSuccessful()) {
						JSONObject me = new JSONObject(meResponse.body().string());
						
						long id = Long.parseLong(me.getString("id"));
						
						JSONArray secrets = JSBBot.getConfig("webserver.jwtSecrets", JSONArray.class);
						String secret = secrets.getString((int) (id % secrets.length()));
						
						Algorithm algorithm = Algorithm.HMAC256(secret);
						
						String token = JWT.create().withClaim("id", id).sign(algorithm);
						
						Database.get().updateDashboardUser(id, Updates.combine(
							Updates.set("accessToken", data.getString("access_token")),
							Updates.set("expires", Clock.systemUTC().instant().getEpochSecond() + data.getLong("expires_in")),
							Updates.set("refreshToken", data.getString("refresh_token"))
						));
						
						return Response.ok(new JSONObject().put("success", true).put("data", new JSONObject().put("token", token))).build();
					}else{
						System.err.println("Failed to get me: " + new JSONObject(meResponse.body().string()));
					}
				}
			}else{
				System.err.println("Failed to convert code to auth: " + new JSONObject(response.body().string()));
			}
		}catch(IOException e) {
			e.printStackTrace();
		}
		
		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new JSONObject()
			.put("success", false)
			.put("error", "Internal server error")
			.put("code", Status.INTERNAL_SERVER_ERROR.getStatusCode())
		).build();
	}
}