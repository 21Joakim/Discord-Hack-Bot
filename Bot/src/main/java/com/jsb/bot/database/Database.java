package com.jsb.bot.database;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import com.jsb.bot.core.JSBBot;
import com.jsb.bot.database.callback.Callback;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;

public class Database {
	
	public static final Database INSTANCE = new Database();
	
	public static final Document EMPTY_DOCUMENT = new Document();
	
	public static Database get() {
		return Database.INSTANCE;
	}
	
	private ExecutorService queryExecutor = Executors.newCachedThreadPool();
	
	private MongoClient client;
	private MongoDatabase database;
	
	private MongoCollection<Document> guilds;
	private MongoCollection<Document> modlogCases;
	
	private Database() {
		JSONObject mongodb = JSBBot.config.getJSONObject("mongodb");
		
		MongoClientSettings.Builder settings = MongoClientSettings.builder()
			.applyToClusterSettings(builder -> {
				builder.hosts(List.of(new ServerAddress(mongodb.optString("host", "localhost"), mongodb.optInt("port", 27017))));
			});
		
		if(!mongodb.isNull("username") && !mongodb.isNull("password")) {
			String username = mongodb.getString("username");
			String database = mongodb.getString("database");
			char[] password = mongodb.getString("password").toCharArray();
			
			settings.credential(MongoCredential.createCredential(username, database, password));
		}
		
		this.client = MongoClients.create(MongoClientSettings.builder().build());
		this.database = this.client.getDatabase(mongodb.getString("database"));
		
		this.guilds = this.database.getCollection("guilds");
		
		this.modlogCases = this.database.getCollection("modlogCases");
		this.modlogCases.createIndex(Indexes.ascending("guildId"));
		this.modlogCases.createIndex(Indexes.ascending("id"));
		
		System.out.println("Connecting to MongoDB...");
		
		try {
			this.client.listDatabaseNames().first();
			
			System.out.println("Connected to MongoDB");
		}catch(Exception e) {
			throw new RuntimeException("MongoDB failed to connect");
		}
	}
	
	private UpdateOptions defaultUpdateOptions = new UpdateOptions().upsert(true);
	
	public MongoCollection<Document> getModlogCases() {
		return this.modlogCases;
	}
	
	public long getModlogCasesAmountFromGuild(long guildId) {
		return this.modlogCases.countDocuments(Filters.eq("guildId", guildId));
	}
	
	public FindIterable<Document> getModlogCasesFromGuild(long guildId) {
		return this.modlogCases.find(Filters.eq("guildId", guildId));
	}
	
	public Document getModlogCase(long guildId, int id) {
		return this.getModlogCasesFromGuild(guildId).filter(Filters.eq("id", id)).first();
	}
	
	public void getModlogCase(long guildId, int id, Callback<Document> callback) {
		this.queryExecutor.submit(() -> {
			try {
				callback.onResult(this.getModlogCase(guildId, id), null);
			}catch(Throwable e) {
				callback.onResult(null, e);
			}
		});
	}
	
	public void insertModlogCase(Document document) {
		this.queryExecutor.submit(() -> {
			this.modlogCases.insertOne(document);
		});
	}
	
	public void updateModlogCase(ObjectId id, Bson update, Callback<UpdateResult> callback) {
		this.queryExecutor.submit(() -> {
			try {
				callback.onResult(this.modlogCases.updateOne(Filters.eq("_id", id), update, this.defaultUpdateOptions), null);
			}catch(Throwable e) {
				callback.onResult(null, e);
			}
		});
	}
	
	public MongoCollection<Document> getGuilds() {
		return this.guilds;
	}
	
	public Document getGuildById(long guildId, Bson filter, Bson projection) {
		Document document;
		if(filter != null) {
			document = this.guilds.find(Filters.and(Filters.eq("_id", guildId), filter)).projection(projection).first();
		}else{
			document = this.guilds.find(Filters.eq("_id", guildId)).projection(projection).first();
		}
		
		return document == null ? Database.EMPTY_DOCUMENT : document;
	}
	
	public void getGuildById(long guildId, Bson filter, Bson projection, Callback<Document> callback) {
		this.queryExecutor.submit(() -> {
			try {
				callback.onResult(this.getGuildById(guildId, filter, projection), null);
			}catch(Throwable e) {
				callback.onResult(null, e);
			}
		});
	}
	
	public Document getGuildById(long guildId) {
		return this.getGuildById(guildId, null, null);
	}
	
	public void getGuildById(long guildId, Callback<Document> callback) {
		this.getGuildById(guildId, null, null, callback);
	}
	
	public UpdateResult updateGuildById(long guildId, Bson filter, Bson update, UpdateOptions options) {
		if(filter != null) {
			return this.guilds.updateOne(Filters.and(Filters.eq("_id", guildId), filter), update, options != null ? options : this.defaultUpdateOptions);
		}else{
			return this.guilds.updateOne(Filters.eq("_id", guildId), update, options != null ? options : this.defaultUpdateOptions);
		}
	}
	
	public UpdateResult updateGuildById(long guildId, Bson filter, Bson update) {
		return this.updateGuildById(guildId, (Bson) null, update, (UpdateOptions) null);
	}
	
	public UpdateResult updateGuildById(long guildId, Bson update, UpdateOptions options) {
		return this.updateGuildById(guildId, (Bson) null, update, options);
	}
	
	public UpdateResult updateGuildById(long guildId, Bson update) {
		return this.updateGuildById(guildId, (Bson) null, update, (UpdateOptions) null);
	}
	
	public void updateGuildById(long guildId, Bson filter, Bson update, UpdateOptions options, Callback<UpdateResult> callback) {
		this.queryExecutor.submit(() -> {
			try {
				callback.onResult(this.updateGuildById(guildId, filter, update, options), null);
			}catch(Throwable e) {
				callback.onResult(null, e);
			}
		});
	}
	
	public void updateGuildById(long guildId, Bson filter, Bson update, Callback<UpdateResult> callback) {
		this.updateGuildById(guildId, filter, update, null, callback);
	}
	
	public void updateGuildById(long guildId, Bson update, UpdateOptions options, Callback<UpdateResult> callback) {
		this.updateGuildById(guildId, null, update, options, callback);
	}
	
	public void updateGuildById(long guildId, Bson update, Callback<UpdateResult> callback) {
		this.updateGuildById(guildId, null, update, null, callback);
	}
}