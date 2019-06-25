package com.jsb.bot.command;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.jockie.bot.core.argument.Argument;
import com.jockie.bot.core.command.Command;
import com.jockie.bot.core.command.Command.AuthorPermissions;
import com.jockie.bot.core.command.Command.BotPermissions;
import com.jockie.bot.core.command.impl.CommandEvent;
import com.jockie.bot.core.command.impl.CommandImpl;
import com.jsb.bot.database.Database;
import com.jsb.bot.database.callback.Callback;
import com.jsb.bot.paged.PagedResult;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;

public class CommandLogger extends CommandImpl {

	public CommandLogger() {
		super("logger");
	}
	
	@BotPermissions(Permission.MANAGE_WEBHOOKS)
	@AuthorPermissions(Permission.MANAGE_SERVER)
	@Command(value="add")
	public void add(CommandEvent event, @Argument("channel") TextChannel channel) {
		Database.get().getGuildById(event.getGuild().getIdLong(), null, Projections.include("logger.loggers.channel", "logger.loggers.enabled"), (data, exception) -> {
			if(exception != null) {
				exception.printStackTrace();
				
				event.reply("Something went wrong :no_entry:").queue();
			}else{
				List<Document> loggers = data.getEmbedded(List.of("logger", "loggers"), Collections.emptyList());
				for(Document logger : loggers) {
					Long loggerChannel = logger.getLong("channel");
					if(loggerChannel != null && loggerChannel.equals(channel.getIdLong())) {
						if(logger.getBoolean("enabled", true)) {
							event.reply(":no_entry: That channel already has a logger").queue();
						}else{
							event.reply(":no_entry: That channel already has a logger but it is not enabled, enable it with the `logger enable` command!").queue();
						}
						
						return;
					}
				}
				
				channel.createWebhook("Logger").queue(webhook -> {
					Document logger = new Document()
						.append("enabled", true)
						.append("channelId", channel.getIdLong())
						.append("webhookId", webhook.getIdLong())
						.append("webhookToken", webhook.getToken())
						.append("enabledEvents", Collections.emptyList())
						.append("disabledEvents", Collections.emptyList());
					
					Database.get().updateGuildById(event.getGuild().getIdLong(), Updates.push("logger.loggers", logger), (result, exception2) -> {
						if(exception2 != null) {
							exception2.printStackTrace();
							
							event.reply("Something went wrong :no_entry:").queue();
						}else{
							event.reply("Created logger for " + channel.getAsMention()).queue();
						}
					});
				}, exception2 -> {
					event.reply(":no_entry: Failed to create the webhook!").queue();
				});
			}
		});
	}
	
	private String getTextChannelName(Guild guild, long id) {
		TextChannel channel = guild.getTextChannelById(id);
		
		if(channel == null) {
			return "Unknown Channel (" + id + ")";
		}
		
		return channel.getAsMention();
	}
	
	private void enableLogger(CommandEvent event, Document logger, boolean enable) {
		if(enable == logger.getBoolean("enabled", true)) {
			event.reply("The logger for " + this.getTextChannelName(event.getGuild(), logger.getLong("channelId")) + " is already " + (enable ? "enabled" : "disabled")).queue();
		}else{
			UpdateOptions options = new UpdateOptions().arrayFilters(List.of(Filters.eq("logger.channelId", logger.getLong("channelId"))));
			
			Database.get().updateGuildById(event.getGuild().getIdLong(), Updates.set("logger.loggers.$[logger].enabled", enable), options, (result, exception) -> {
				if(exception != null) {
					exception.printStackTrace();
					
					event.reply("Something went wrong :no_entry:").queue();
				}else{
					event.reply((enable ? "Enabled" : "Disabled") + " logger for " + this.getTextChannelName(event.getGuild(), logger.getLong("channelId"))).queue();
				}
			});
		}
	}
	
	private void getLoggerPerform(CommandEvent event, TextChannel channel, Consumer<Document> consumer) {
		Callback<Document> callback = (data, exception) -> {
			if(exception != null) {
				exception.printStackTrace();
				
				event.reply("Something went wrong :no_entry:").queue();
			}else{
				List<Document> loggers = data.getEmbedded(List.of("logger", "loggers"), Collections.emptyList());
				if(loggers.size() > 0) {
					if(loggers.size() == 1) {
						consumer.accept(data);
					}else{
						new PagedResult<>(loggers)
							.setDisplayFunction(logger -> this.getTextChannelName(event.getGuild(), logger.getLong("channelId")))
							.onSelect(selectEvent -> {
								consumer.accept(selectEvent.entry);
							})
							.send(event);
					}
				}else{
					if(channel != null) {
						event.reply("That channel has no logger :no_entry:").queue();
					}else{
						event.reply("No loggers are setup for this server").queue();
					}
				}
			}
		};
		
		if(channel != null) {
			Bson filter = Filters.eq("logger.loggers.channelId", channel.getIdLong());
			Bson include = Projections.include("logger.loggers.$");
			
			Database.get().getGuildById(event.getGuild().getIdLong(), filter, include, callback);
		}else{
			Database.get().getGuildById(event.getGuild().getIdLong(), callback);
		}
	}
	
	@AuthorPermissions(Permission.MANAGE_SERVER)
	@Command(value="disable")
	public void disable(CommandEvent event, @Argument(value="channel", nullDefault=true) TextChannel channel) {
		this.getLoggerPerform(event, channel, document -> {
			this.enableLogger(event, document, false);
		});
	}
	
	@AuthorPermissions(Permission.MANAGE_SERVER)
	@Command(value="enable")
	public void enable(CommandEvent event, @Argument(value="channel", nullDefault=true) TextChannel channel) {
		this.getLoggerPerform(event, channel, document -> {
			this.enableLogger(event, document, true);
		});
	}
	
	@Command(value="disable event")
	public void disableEvent(CommandEvent event, @Argument(value="channel", nullDefault=true) TextChannel channel, @Argument(value="event") String eventType) {
		String type = eventType.toUpperCase();
		
		this.getLoggerPerform(event, channel, logger -> {
			Document document = logger.getList("disabledEvents", Document.class, Collections.emptyList()).stream()
				.filter(e -> e.getString("type").equals(type))
				.findFirst()
				.orElse(null);
			
			if(document != null) {
				event.reply("`" + type + "` has already been disabled for " + this.getTextChannelName(event.getGuild(), logger.getLong("channelId"))).queue();
				
				return;
			}
			
			Document loggerEvent = new Document("type", type);
			
			UpdateOptions options = new UpdateOptions().arrayFilters(List.of(Filters.eq("logger.channelId", logger.getLong("channelId"))));
			
			Database.get().updateGuildById(event.getGuild().getIdLong(), Updates.push("logger.loggers.$[logger].disabledEvents", loggerEvent), options, (result, exception) -> {
				if(exception != null) {
					exception.printStackTrace();
					
					event.reply("Something went wrong :no_entry:").queue();
				}else{
					event.reply("Disabled `" + type + "` for " + this.getTextChannelName(event.getGuild(), logger.getLong("channelId"))).queue();
				}
			});
		});
	}
}