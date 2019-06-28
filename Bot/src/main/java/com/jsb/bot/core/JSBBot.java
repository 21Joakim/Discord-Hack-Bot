package com.jsb.bot.core;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Array;
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
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Activity.ActivityType;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;

public class JSBBot {
	
	private static JSONObject config;
	
	private static ShardManager shardManager;
	
	private static CommandListener commandListener;
	
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
		
		String[] prefixes = JSBBot.getConfig("prefixes", String[].class);
		if(prefixes.length > 0) {
			throw new RuntimeException("Please provide at least one prefix");
		}
		
		CommandListener commandListener = new CommandListener()
			.addCommandStore(CommandStore.of("com.jsb.bot.command"))
			.addCommandStore(CommandStore.of("com.jsb.bot.module"))
			.addDevelopers(281465397214052352L, 190551803669118976L, 402557516728369153L)
			.setDefaultPrefixes("?");
		
		commandListener.removeDefaultPreExecuteChecks()
			.addPreExecuteCheck(commandListener.defaultBotPermissionCheck)
			.addPreExecuteCheck(commandListener.defaultNsfwCheck)
			.addPreExecuteCheck((event, command) -> CheckUtility.checkPermissions(event));
		
		String prefix = prefixes[0];
		
		ShardManager shardManager = new DefaultShardManagerBuilder(JSBBot.config.getString("token"))
			.addEventListeners(commandListener, PagedManager.get(), new LoggerListener(), new ModlogListener(), new MuteListener())
			.setActivity(Activity.of(ActivityType.LISTENING, prefix + "help"))
			.build();
		
		JSBBot.commandListener = commandListener;
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
	
	public static CommandListener getCommandListener() {
		return JSBBot.commandListener;
	}
	
	public static JSONObject getConfig() {
		return JSBBot.config;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T getConfig(String path) {
		return (T) JSBBot.getConfig(path, Object.class);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T getConfig(String path, T defaultValue) {
		T value = (T) JSBBot.getConfig(path, defaultValue.getClass());
		if(value == null) {
			return defaultValue;
		}
		
		return value;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T getConfig(String path, Class<T> clazz) {
		String[] splitPath = path.split("\\.");
		
		JSONObject position = JSBBot.config;
		for(int i = 0; i < splitPath.length; i++) {
			Object value = position.opt(splitPath[i]);
			
			if(value == null) {
				return null;
			}
			
			if(value instanceof JSONObject) {
				position = (JSONObject) value;
			}
			
			if(i == splitPath.length - 1) {
				if(value instanceof JSONArray) {
					try {
						return clazz.cast(value);
					}catch(ClassCastException e) {}
					
					try {
						value = ((JSONArray) value).toList();
						
						return clazz.cast(value);
					}catch(ClassCastException e) {}
					
					try {
						if(clazz.isArray()) {
							value = ((List<?>) value).toArray((T[]) Array.newInstance(clazz.getComponentType(), 0));
							
							return clazz.cast(value);
						}
					}catch(ClassCastException e) {}
				}else{
					return clazz.cast(value);
				}
			}
		}
		
		return null;
	}
}