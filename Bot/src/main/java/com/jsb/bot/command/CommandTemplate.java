package com.jsb.bot.command;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bson.Document;

import com.jockie.bot.core.argument.Argument;
import com.jockie.bot.core.command.Command;
import com.jockie.bot.core.command.ICommand;
import com.jockie.bot.core.command.Command.AuthorPermissions;
import com.jockie.bot.core.command.impl.CommandEvent;
import com.jockie.bot.core.command.impl.CommandImpl;
import com.jsb.bot.category.Category;
import com.jsb.bot.database.Database;
import com.jsb.bot.paged.PagedResult;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Updates;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;

public class CommandTemplate extends CommandImpl {
	
	public static final Pattern TEMPLATE_NAME_PATTERN = Pattern.compile("[a-z0-9_-]+", Pattern.CASE_INSENSITIVE);
	
	public CommandTemplate() {
		super("template");
		
		this.initialize(this);
	}
	
	@AuthorPermissions(Permission.MANAGE_SERVER)
	@Command(value="add", description="Create a new reason template, templates are used to quickly specify a reason for moderator actions, they are specified like `t:name` or `template:name` when using a command which takes a reason")
	public void add(CommandEvent event, @Argument("template name") String name, @Argument(value="template", endless=true) String template) {
		Matcher matcher = CommandTemplate.TEMPLATE_NAME_PATTERN.matcher(name);
		if(!matcher.matches()) {
			event.reply("Invalid template name, names may only contain: a-z, 0-9, _ and -").queue();
			
			return;
		}
		
		Database.get().getGuildById(event.getGuild().getIdLong(), null, Projections.include("template.reasons"), (document, exception) -> {
			if(exception != null) {
				exception.printStackTrace();
				
				event.reply("Something went wrong :no_entry:").queue();
				
				return;
			}
			
			List<Document> templates = document.getEmbedded(List.of("template", "reasons"), Collections.emptyList());
			
			if(templates.stream().anyMatch(t -> t.getString("name").equalsIgnoreCase(name))) {
				event.reply("A template with the name of `" + name + "` already exists").queue();
				
				return;
			}
			
			Document template0 = new Document()
				.append("name", name)
				.append("template", template)
				.append("createdAt", Clock.systemUTC().instant().getEpochSecond());
			
			Database.get().updateGuildById(event.getGuild().getIdLong(), Updates.push("template.reasons", template0), (result, exception2) -> {
				if(exception2 != null) {
					exception2.printStackTrace();
					
					event.reply("Something went wrong :no_entry:").queue();
					
					return;
				}
				
				event.reply("Created template with the name of `" + name + "`").queue();
			});
		});
	}
	
	@AuthorPermissions(Permission.MANAGE_SERVER)
	@Command(value="remove", description="Remove a template by its name")
	public void remove(CommandEvent event, @Argument(value="template name", endless=true) String name) {
		Database.get().updateGuildById(event.getGuild().getIdLong(), Updates.pull("template.reasons", new Document("name", name)), (result, exception) -> {
			if(exception != null) {
				exception.printStackTrace();
				
				event.reply("Something went wrong :no_entry:").queue();
				
				return;
			}
			
			if(result.getModifiedCount() > 0) {
				event.reply("Removed template by the name of `" + name + "`").queue();
			}else{
				event.reply("There is no template by the name of `" + name + "`").queue();
			}
		});
	}
	
	@Command(value="list", description="List all available templates")
	public void list(CommandEvent event) {
		Database.get().getGuildById(event.getGuild().getIdLong(), null, Projections.include("template.reasons"), (document, exception) -> {
			if(exception != null) {
				exception.printStackTrace();
				
				event.reply("Something went wrong :no_entry:").queue();
				
				return;
			}
			
			List<Document> templates = document.getEmbedded(List.of("template", "reasons"), Collections.emptyList());
			if(templates.size() == 0) {
				event.reply("This server has no templates").queue();
				
				return;
			}
			
			new PagedResult<>(templates)
				.setDisplayFunction(template -> template.getString("name"))
				.onSelect(selectEvent -> {
					Document entry = selectEvent.entry;
					
					EmbedBuilder embed = new EmbedBuilder();
					embed.addField("Name", entry.getString("name"), false);
					embed.addField("Template", entry.getString("template"), false);
					
					embed.setFooter("Created");
					embed.setTimestamp(Instant.ofEpochSecond(entry.getLong("createdAt")));
					
					event.reply(embed.build()).queue();
				})
				.send(event);
		});
	}
	
	public void initialize(CommandImpl command) {
		command.setCategory(Category.TEMPLATE);
		
		for(ICommand subCommand : command.getSubCommands()) {
			this.initialize((CommandImpl) subCommand);
		}
	}
}