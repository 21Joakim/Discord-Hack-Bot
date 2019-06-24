package com.jsb.bot.database;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.bson.Document;
import org.json.JSONObject;

import com.jsb.bot.core.JSBBot;
import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.async.client.MongoDatabase;

public class Database {
	
	public static final Database INSTANCE = new Database();
	
	public static Database get() {
		return Database.INSTANCE;
	}
	
	private MongoClient client;
	private MongoDatabase database;
	
	private MongoCollection<Document> guilds;
	
	private Database() {
		JSONObject mongodb = JSBBot.config.getJSONObject("mongodb");
		
		this.client = MongoClients.create("mongodb://" + mongodb.optString("host", "localhost") + ":" + mongodb.optString("port", "27017"));
		this.database = this.client.getDatabase(mongodb.getString("database"));
		
		this.guilds = this.database.getCollection("guilds");
		
		if(!this.isConnected()) {
			throw new RuntimeException("MongoDB failed to connect");
		}
	}
	
	public boolean isConnected() {
		CompletableFuture<Void> future = new CompletableFuture<>();
		this.client.listDatabases().first((document, throwable) -> {
			if(throwable != null) {
				future.completeExceptionally(throwable);
			}
			
			future.complete(null);
		});
		
		try {
			future.get();
		}catch(InterruptedException | ExecutionException e) {
			return false;
		}
		
		return true;
	}
	
	public MongoCollection<Document> getGuilds() {
		return this.guilds;
	}
}