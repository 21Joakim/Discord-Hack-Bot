package com.jsb.bot.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
import com.jsb.bot.logger.LoggerType;
import com.jsb.bot.paged.PagedResult;
import com.jsb.bot.utility.LoggerUtility;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;

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
		LoggerUtility.getLoggers(event.getGuild(), (loggers, exception) -> {
			if(exception != null) {
				exception.printStackTrace();
				
				event.reply("Something went wrong :no_entry:").queue();
				
				return;
			}
			
			if(loggers.size() >= 5) {
				event.reply("You can't have more than 5 loggers :no_entry:").queue();
				
				return;
			}
			
			for(Document logger : loggers) {
				Long loggerChannel = logger.getLong("channelId");
				if(loggerChannel != null && loggerChannel.equals(channel.getIdLong())) {
					if(logger.getBoolean("enabled", true)) {
						event.reply("That channel already has a logger :no_entry:").queue();
					}else{
						event.reply("That channel already has a logger but it is not enabled, enable it with the `logger enable` command :no_entry:").queue();
					}
					
					return;
				}
			}
			
			channel.createWebhook("Logger").queue(webhook -> {
				Document logger = new Document()
					.append("enabled", false)
					.append("mode", false)
					.append("events", Collections.emptyList())
					.append("channelId", channel.getIdLong())
					.append("webhookId", webhook.getIdLong())
					.append("webhookToken", webhook.getToken());
				
				Database.get().updateGuildById(event.getGuild().getIdLong(), Updates.push("logger.loggers", logger), (result, exception2) -> {
					if(exception2 != null) {
						exception2.printStackTrace();
						
						event.reply("Something went wrong :no_entry:").queue();
					}else{
						event.reply("Created logger for " + channel.getAsMention() + ", loggers are disabled by default, use the `logger enable` command to enable it").queue();
					}
				});
			}, exception2 -> {
				event.reply("Failed to create the webhook :no_entry:").queue();
			});
		});
	}
	
	@AuthorPermissions(Permission.MANAGE_SERVER)
	@Command("remove")
	public void remove(CommandEvent event, @Argument(value="channel", nullDefault=true) TextChannel channel) {
		this.getLoggerPerform(event, channel, logger -> {
			long channelId = logger.getLong("channelId");
			TextChannel loggerChannel = event.getGuild().getTextChannelById(channelId);
			
			AtomicBoolean removeWebhook = new AtomicBoolean(false);
			if(loggerChannel != null) {
				if(event.getSelfMember().hasPermission(loggerChannel, Permission.MANAGE_WEBHOOKS)) {
					loggerChannel.deleteWebhookById(String.valueOf(logger.getLong("webhookId"))).queue($ -> {}, $ -> {});
					
					removeWebhook.set(true);
				}
			}
			
			Database.get().updateGuildById(event.getGuild().getIdLong(), Updates.pull("logger.loggers", new Document("channelId", channelId)), (data, exception) -> {
				if(exception != null) {
					exception.printStackTrace();
					
					event.reply("Something went wrong :no_entry:").queue();
				}else{
					event.reply("Removed logger for " + this.getTextChannelName(event.getGuild(), channelId) + (!removeWebhook.get() ? ", I was unable to remove the webhook because I am missing the `MANAGE_WEBHOOK` permission!" : "")).queue();
				}
			});
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
			
			Database.get().getGuildById(event.getGuild().getIdLong(), null, Projections.include("logger.loggers"), (document, exception) -> {
				if(exception != null) {
					exception.printStackTrace();
					
					event.reply("Something went wrong :no_entry:").queue();
				}else{
					List<Document> loggers = document.getEmbedded(List.of("logger", "loggers"), Collections.emptyList());
					if(LoggerUtility.isAnyTypesInUse(loggers, LoggerUtility.getEnabledEvents(logger, true))) {
						event.reply("You are unable to enable this logger because one or more events are already enabled on another logger :no_entry:").queue();
						
						return;
					}
					
					Database.get().updateGuildById(event.getGuild().getIdLong(), Updates.set("logger.loggers.$[logger].enabled", enable), options, (result, exception2) -> {
						if(exception2 != null) {
							exception2.printStackTrace();
							
							event.reply("Something went wrong :no_entry:").queue();
						}else{
							event.reply((enable ? "Enabled" : "Disabled") + " logger for " + this.getTextChannelName(event.getGuild(), logger.getLong("channelId"))).queue();
						}
					});
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
						consumer.accept(loggers.get(0));
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
			Database.get().getGuildById(event.getGuild().getIdLong(), null, Projections.include("logger.loggers"), callback);
		}
	}
	
	@AuthorPermissions(Permission.MANAGE_SERVER)
	@Command(value="disable")
	public void disable(CommandEvent event, @Argument(value="channel", nullDefault=true) TextChannel channel) {
		this.getLoggerPerform(event, channel, logger -> {
			this.enableLogger(event, logger, false);
		});
	}
	
	@AuthorPermissions(Permission.MANAGE_SERVER)
	@Command(value="enable")
	public void enable(CommandEvent event, @Argument(value="channel", nullDefault=true) TextChannel channel) {
		this.getLoggerPerform(event, channel, logger -> {
			this.enableLogger(event, logger, true);
		});
	}
	
	@AuthorPermissions(Permission.MANAGE_SERVER)
	@Command(value="toggle")
	public void toggle(CommandEvent event, @Argument(value="channel", nullDefault=true) TextChannel channel) {
		this.getLoggerPerform(event, channel, logger -> {
			this.enableLogger(event, logger, logger.getBoolean("enabled", true));
		});
	}
	
	@Command(value="types")
	public void loggerTypes(CommandEvent event, @Argument(value="category", nullDefault=true) String categoryString) {
		if(categoryString != null) {
			try {
				LoggerType.Category category = LoggerType.Category.valueOf(categoryString.toUpperCase());
				
				new PagedResult<>(LoggerType.getByCategory(category))
					.setDisplayFunction(type -> "`" + type.toString() + "`")
					.send(event);
			}catch(IllegalArgumentException e) {
				event.reply("`" + categoryString.toUpperCase() + "` is not a valid category").queue();
			}
		}else{
			new PagedResult<>(LoggerType.values())
				.setDisplayFunction(type -> "`" + type.toString() + "`")
				.send(event);
		}
	}
	
	private void enableEvents(CommandEvent event, Document logger, boolean enable, EnumSet<LoggerType> events) {
		List<Document> loggerEvents = logger.getList("events", Document.class, new ArrayList<>());
		
		boolean mode;
		if(loggerEvents.size() == 0) {
			mode = enable;
			
			for(LoggerType type : events) {
				loggerEvents.add(new Document("type", type.toString()));
			}
		}else{
			mode = logger.getBoolean("mode");
			
			for(LoggerType type : events) {
				Document loggerEvent = loggerEvents.stream()
					.filter(e -> e.getString("type").equals(type.toString()))
					.findFirst()
					.orElse(null);
				
				if(enable) {
					if(!mode) {
						if(loggerEvent != null) {
							loggerEvents.remove(loggerEvent);
						}else{
							event.reply("`" + type.toString() + "` is already enabled :no_entry:").queue();
							
							return;
						}
					}else if(loggerEvent != null) {
						event.reply("`" + type.toString() + "` is already enabled :no_entry:").queue();
						
						return;
					}else{
						loggerEvents.add(new Document("type", type.toString()));
					}
				}else{
					if(mode) {
						if(loggerEvent != null) {
							loggerEvents.remove(loggerEvent);
						}else{
							event.reply("`" + type.toString() + "`is already disabled :no_entry:").queue();
							
							return;
						}
					}else if(loggerEvent != null) {
						event.reply("`" + type.toString() + "` is already disabled :no_entry:").queue();
						
						return;
					}else{
						loggerEvents.add(new Document("type", type.toString()));
					}
				}
			}
		}
		
		UpdateOptions options = new UpdateOptions().arrayFilters(List.of(Filters.eq("logger.channelId", logger.getLong("channelId"))));
		
		Callback<UpdateResult> callback = (result, exception) -> {
			if(exception != null) {
				exception.printStackTrace();
				
				event.reply("Something went wrong :no_entry:").queue();
			}else{
				event.reply((enable ? "Enabled" : "Disabled") + " `" + events.toString() + "` for " + this.getTextChannelName(event.getGuild(), logger.getLong("channelId"))).queue();
			}
		};
		
		if(logger.getBoolean("enabled", true)) {
			LoggerUtility.getLoggers(event.getGuild(), (loggers, exception) -> {
				if(exception != null) {
					event.reply("Something went wrong :no_entry:").queue();
				}else{
					EnumSet<LoggerType> set = null;
					if(loggerEvents.size() == 0) {
						set = EnumSet.allOf(LoggerType.class);
					}else if(mode) {
						set = EnumSet.noneOf(LoggerType.class);
						set.addAll(LoggerUtility.getEvents(loggerEvents));
					}else if(!mode) {
						set = EnumSet.allOf(LoggerType.class);
						set.removeAll(LoggerUtility.getEvents(loggerEvents));
					}
					
					loggers = loggers.stream()
						.filter(l -> !l.getLong("channelId").equals(logger.getLong("channelId")))
						.collect(Collectors.toList());
					
					if(LoggerUtility.isAnyTypesInUse(loggers, set)) {
						event.reply("You are unable to enable those events because one or more events are already enabled on another logger :no_entry:").queue();
						
						return;
					}
					
					Database.get().updateGuildById(event.getGuild().getIdLong(), Updates.combine(Updates.set("logger.loggers.$[logger].mode", mode), Updates.set("logger.loggers.$[logger].events", loggerEvents)), options, callback);
				}
			});
			
			return;
		}
		
		Database.get().updateGuildById(event.getGuild().getIdLong(), Updates.combine(Updates.set("logger.loggers.$[logger].mode", mode), Updates.set("logger.loggers.$[logger].events", loggerEvents)), options, callback);
	}
	
	@AuthorPermissions(Permission.MANAGE_SERVER)
	@Command(value="disable event")
	public void disableEvent(CommandEvent event, @Argument(value="channel", nullDefault=true) TextChannel channel, @Argument(value="event", nullDefault=true) String type) {
		if(type != null) {
			EnumSet<LoggerType> types;
			try {
				types = EnumSet.of(LoggerType.valueOf(type.toUpperCase()));
			}catch(IllegalArgumentException e) {
				try {
					types = LoggerType.getByCategory(LoggerType.Category.valueOf(type.toUpperCase()));
				}catch(IllegalArgumentException e2) {
					event.reply("`" + type.toUpperCase() + "` is not a valid type, check the `logger types` command to see all the events").queue();
					
					return;
				}
			}
			
			EnumSet<LoggerType> finalTypes = types;
			this.getLoggerPerform(event, channel, logger -> {
				this.enableEvents(event, logger, false, finalTypes);
			});
		}else{
			new PagedResult<>(List.of("Category", "Type"))
				.onSelect(selectEvent -> {
					if(selectEvent.entry.equals("Category")) {
						new PagedResult<>(LoggerType.Category.values())
							.onSelect(categorySelectEvent -> {
								this.getLoggerPerform(event, channel, logger -> {
									this.enableEvents(event, logger, false, LoggerType.getByCategory(categorySelectEvent.entry));
								});
							})
							.send(event);
					}else{
						new PagedResult<>(LoggerType.values())
							.onSelect(typeSelectEvent -> {
								this.getLoggerPerform(event, channel, logger -> {
									this.enableEvents(event, logger, false, EnumSet.of(typeSelectEvent.entry));
								});
							})
							.send(event);
					}
				})
				.send(event);
		}
	}
	
	@AuthorPermissions(Permission.MANAGE_SERVER)
	@Command("enable event")
	public void enableEvent(CommandEvent event, @Argument(value="channel", nullDefault=true) TextChannel channel, @Argument(value="event", nullDefault=true) String type) {
		if(type != null) {
			EnumSet<LoggerType> types;
			try {
				types = EnumSet.of(LoggerType.valueOf(type.toUpperCase()));
			}catch(IllegalArgumentException e) {
				try {
					types = LoggerType.getByCategory(LoggerType.Category.valueOf(type.toUpperCase()));
				}catch(IllegalArgumentException e2) {
					event.reply("`" + type.toUpperCase() + "` is not a valid type, check the `logger types` command to see all the events").queue();
					
					return;
				}
			}
			
			EnumSet<LoggerType> finalTypes = types;
			this.getLoggerPerform(event, channel, logger -> {
				this.enableEvents(event, logger, false, finalTypes);
			});
		}else{
			new PagedResult<>(List.of("Category", "Type"))
				.onSelect(selectEvent -> {
					if(selectEvent.entry.equals("Category")) {
						new PagedResult<>(LoggerType.Category.values())
							.onSelect(categorySelectEvent -> {
								this.getLoggerPerform(event, channel, logger -> {
									this.enableEvents(event, logger, true, LoggerType.getByCategory(categorySelectEvent.entry));
								});
							})
							.send(event);
					}else{
						new PagedResult<>(LoggerType.values())
							.onSelect(typeSelectEvent -> {
								this.getLoggerPerform(event, channel, logger -> {
									this.enableEvents(event, logger, true, EnumSet.of(typeSelectEvent.entry));
								});
							})
							.send(event);
					}
				})
				.send(event);
		}
	}
	
	private void reset(CommandEvent event, Document logger, boolean enable) {
		UpdateOptions options = new UpdateOptions().arrayFilters(List.of(Filters.eq("logger.channelId", logger.getLong("channelId"))));
		
		Database.get().updateGuildById(event.getGuild().getIdLong(), Updates.combine(Updates.set("logger.loggers.$[logger].mode", !enable), Updates.set("logger.loggers.$[logger].events", Collections.emptyList())), options, (result, exception) -> {
			if(exception != null) {
				exception.printStackTrace();
				
				event.reply("Something went wrong :no_entry:").queue();
				
				return;
			}
			
			event.reply((enable ? "Enabled" : "Disabled") + " all events").queue();
		});
	}
	
	@AuthorPermissions(Permission.MANAGE_SERVER)
	@Command(value="enable all events")
	public void enabledAllEvents(CommandEvent event, @Argument(value="channel", nullDefault=true) TextChannel channel) {
		this.getLoggerPerform(event, channel, logger -> {
			this.reset(event, logger, true);
		});
	}
	
	@AuthorPermissions(Permission.MANAGE_SERVER)
	@Command(value="disable all events")
	public void disableAllEvents(CommandEvent event, @Argument(value="channel", nullDefault=true) TextChannel channel) {
		this.getLoggerPerform(event, channel, logger -> {
			this.reset(event, logger, false);
		});
	}
}