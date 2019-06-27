package com.jsb.bot.utility;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.bson.Document;

import com.jsb.bot.database.Database;
import com.jsb.bot.database.callback.Callback;
import com.jsb.bot.logger.LoggerType;
import com.mongodb.client.model.Projections;

import net.dv8tion.jda.api.entities.Guild;

public class LoggerUtility {
	
	private LoggerUtility() {}
	
	public static EnumSet<LoggerType> getEvents(List<Document> events) {
		EnumSet<LoggerType> set = EnumSet.noneOf(LoggerType.class);
		
		for(Document event : events) {
			/* 
			 * The try catch is here just in case we ever remove any of the events
			 * and don't update the data.
			 */
			try {
				set.add(LoggerType.valueOf(event.getString("type")));
			}catch(IllegalArgumentException e) {
				System.err.println("Found a non-existant LoggerType (" + event.getString("type") + ")");
			}
		}
		
		return set;
	}
	
	public static EnumSet<LoggerType> getEnabledEvents(Document logger) {
		return LoggerUtility.getEnabledEvents(logger, false);
	}
	
	public static EnumSet<LoggerType> getEnabledEvents(Document logger, boolean forceEnabled) {
		if(!forceEnabled && !logger.getBoolean("enabled", true)) {
			return EnumSet.noneOf(LoggerType.class);
		}
		
		List<Document> events = logger.getList("events", Document.class, Collections.emptyList());
		boolean mode = logger.getBoolean("mode");
		
		if(!mode && events.size() == 0) {
			return EnumSet.allOf(LoggerType.class);
		}
		
		if(mode) {
			EnumSet<LoggerType> set = EnumSet.noneOf(LoggerType.class);
			set.addAll(LoggerUtility.getEvents(events));
			
			return set;
		}else if(!mode) {
			EnumSet<LoggerType> set = EnumSet.allOf(LoggerType.class);
			set.removeAll(LoggerUtility.getEvents(events));
			
			return set;
		}
		
		return null;
	}
	
	public static Document findLogger(List<Document> loggers, LoggerType type) {
		for(Document logger : loggers) {
			if(LoggerUtility.getEnabledEvents(logger).contains(type)) {
				return logger;
			}
		}
		
		return null;
	}
	
	public static boolean isAnyTypesInUse(List<Document> loggers, EnumSet<LoggerType> types) {
		for(Document logger : loggers) {
			if(LoggerUtility.getEnabledEvents(logger).stream().anyMatch(type -> types.contains(type))) {
				return true;
			}
		}
		
		return false;
	}
	
	public static void getLoggers(Guild guild, Callback<List<Document>> callback) {
		Database.get().getGuildById(guild.getIdLong(), null, Projections.include("logger.loggers"), (document, exception) -> {
			if(exception != null) {
				callback.onResult(null, exception);
			}else{
				callback.onResult(document.getEmbedded(List.of("logger", "loggers"), Collections.emptyList()), null);
			}
		});
	}
}