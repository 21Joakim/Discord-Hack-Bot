package com.jsb.bot.core;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.jockie.bot.core.command.impl.CommandListener;
import com.jockie.bot.core.command.impl.CommandStore;
import com.jsb.bot.database.Database;
import com.jsb.bot.logger.LoggerListener;
import com.jsb.bot.modlog.ModlogListener;
import com.jsb.bot.mute.MuteListener;
import com.jsb.bot.paged.PagedManager;
import com.jsb.bot.utility.CheckUtility;
import com.jsb.server.Webserver;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;

public class JSBBot {
	
	private static JSONObject config;
	
	private static ShardManager shardManager;
	
	public static void main(String[] args) throws Exception {
		try(FileInputStream stream = new FileInputStream(new File("./config/config.json"))) {
			JSBBot.config = new JSONObject(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
		}
		
		/* Connect to the database */
		Database.get();
		
		if(JSBBot.getConfig("webserver.enabled", false)) {
			System.out.println("Starting webserver...");
			
			Webserver.start();
			
			System.out.println("Started webserver!");
		}
		
		Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
			throwable.printStackTrace();
		});
		
		CommandListener listener = new CommandListener()
			.addCommandStore(CommandStore.of("com.jsb.bot.command"))
			.addCommandStore(CommandStore.of("com.jsb.bot.module"))
			.addDevelopers(281465397214052352L, 190551803669118976L, 402557516728369153L)
			.setDefaultPrefixes(JSBBot.config.getJSONArray("prefixes").toList().toArray(new String[0]));
		
		listener.removeDefaultPreExecuteChecks()
			.addPreExecuteCheck(listener.defaultBotPermissionCheck)
			.addPreExecuteCheck(listener.defaultNsfwCheck)
			.addPreExecuteCheck((event, command) -> CheckUtility.checkPermissions(event));
		
		ShardManager shardManager = new DefaultShardManagerBuilder(JSBBot.config.getString("token"))
			.addEventListeners(listener, PagedManager.get(), new LoggerListener(), new ModlogListener(), new MuteListener())
			.build();
		
		JSBBot.shardManager = shardManager;
		
		for(JDA shard : shardManager.getShards()) {
			shard.awaitReady();
		}
		
		MuteListener.ensureMutes();
		
		System.out.println(String.format("Started %s with %,d guilds", shardManager.getShards().get(0).getSelfUser().getAsTag(), shardManager.getGuilds().size()));
	}
	
	public static ShardManager getShardManager() {
		return JSBBot.shardManager;
	}
	
	public static JSONObject getConfig() {
		return JSBBot.config;
	}
	
	public static <T> T getConfig(String path) {
		return getConfig(path, (T) null);
	}
	
	public static <T> T getConfig(String path, Class<T> clazz) {
		return getConfig(path, (T) null);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T getConfig(String path, T defaultValue) {
		String[] splitPath = path.split("\\.");
		
		JSONObject position = JSBBot.config;
		for(int i = 0; i < splitPath.length; i++) {
			Object value = position.opt(splitPath[i]);
			
			if(value == null) {
				return defaultValue;
			}
			
			if(value instanceof JSONObject) {
				position = (JSONObject) value;
			}
			
			if(i == splitPath.length - 1) {
				if(value instanceof JSONArray) {
					try {
						return (T) value;
					}catch(ClassCastException e) {}
					
					try {
						return (T) (value = ((JSONArray) value).toList());
					}catch(ClassCastException e) {}
					
					try {
						return (T) ((List<?>) value).toArray();
					}catch(ClassCastException e) {}
				}else{
					return (T) value;
				}
			}
		}
		
		return defaultValue;
	}
}