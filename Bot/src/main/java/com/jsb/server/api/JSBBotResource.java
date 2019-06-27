package com.jsb.server.api;

import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import com.jsb.bot.core.JSBBot;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;

@Path("/bot")
public class JSBBotResource {
	
	private OkHttpClient client = new OkHttpClient();
	
	@GET
	@Path("/ping")
	public Response ping() {
		return Response.ok("pong").build();
	}
	
	@GET
	@Path("/auth")
	public Response auth(@QueryParam("code") String code) {
		if(code == null || code.trim().isEmpty()) {
			return Response.status(400).entity("Missing code").build();
		}
		
		// https://discordapp.com/api/oauth2/authorize?client_id=593709124261511170&redirect_uri=http%3A%2F%2Fjockie.ddns.net%3A8080%2Fapi%2Fbot%2Fauth&response_type=code&scope=identify%20guilds
		
		Request request = new Request.Builder()
			.url("https://discordapp.com/api/v6/oauth2/token")
			.post(new FormBody.Builder()
				.add("client_id", JSBBot.getConfig("clientId", String.class))
				.add("client_secret", JSBBot.getConfig("clientSecret", String.class))
				.add("grant_type", "authorization_code")
				.add("code", code)
				.add("redirect_uri", "http://jockie.ddns.net:8080/api/bot/auth")
				.add("scope", "identify guilds")
				.build())
			.build();
		
		try(okhttp3.Response response = this.client.newCall(request).execute()) {
			if(response.isSuccessful()) {
				System.out.println(response.body().string());
			}
		}catch(IOException e) {}
		
		return Response.status(500).entity("Something went wrong").build();
	}
}