package com.jsb.bot.core;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

import org.json.JSONObject;

import com.jockie.bot.core.command.impl.CommandListener;
import com.jockie.bot.core.command.impl.CommandStore;
import com.jsb.bot.database.Database;
import com.jsb.bot.logger.LoggerListener;
import com.jsb.bot.paged.PagedManager;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;

public class JSBBot {
	
	public static JSONObject config;
	
	public static void main(String[] args) throws Exception {
		try(FileInputStream stream = new FileInputStream(new File("./config/config.json"))) {
			JSBBot.config = new JSONObject(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
		}
		
		/* Connect to the database */
		Database.get();
		
		Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
			throwable.printStackTrace();
		});
		
		CommandListener listener = new CommandListener()
			.addCommandStore(CommandStore.of("com.jsb.bot.command"))
			.addCommandStore(CommandStore.of("com.jsb.bot.module"))
			.addDevelopers(281465397214052352L, 190551803669118976L, 402557516728369153L)
			.setDefaultPrefixes("!");
		
		ShardManager shardManager = new DefaultShardManagerBuilder(JSBBot.config.getString("token"))
			.addEventListeners(listener, PagedManager.get(), new LoggerListener())
			.build();
		
		for(JDA shard : shardManager.getShards()) {
			shard.awaitReady();
		}
		
		System.out.println(String.format("Started %s with %,d guilds", shardManager.getShards().get(0).getSelfUser().getAsTag(), shardManager.getGuilds().size()));
	}
}