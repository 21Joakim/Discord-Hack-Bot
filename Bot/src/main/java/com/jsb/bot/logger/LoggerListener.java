package com.jsb.bot.logger;

import java.awt.Color;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.bson.Document;

import com.jsb.bot.embed.WebhookEmbedBuilder;

import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class LoggerListener extends ListenerAdapter {
	
	private static final int LOG_DELAY = 500;
	
	private static final Color COLOR_GREEN = Color.GREEN;
	private static final Color COLOR_RED = Color.RED;
	
	/* 
	 * Used to delay all logs so we can get the correct action as well as the correct audit logs,
	 * the reason all logs are delayed and not just the ones that use audit logs is
	 * to keep the order of the logs, this can be done in other ways as well but this is the 
	 * easiest. 
	 */
	private ScheduledExecutorService delayer = Executors.newSingleThreadScheduledExecutor();
	
	private void delay(Runnable runnable) {
		this.delayer.schedule(runnable, LoggerListener.LOG_DELAY, TimeUnit.MILLISECONDS);
	}
	
	public void onGuildMemberJoin(GuildMemberJoinEvent event) {
		Document logger = LoggerClient.get().getLogger(event.getGuild(), LoggerType.MEMBER_JOIN);
		if(logger == null) {
			return;
		}
		
		this.delay(() -> {
			WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
			embed.setDescription(String.format("`%s` just joined the server", event.getMember().getEffectiveName()));
			embed.setAuthor(event.getUser().getAsTag(), event.getUser().getEffectiveAvatarUrl());
			embed.setTimestamp(ZonedDateTime.now());
			embed.setFooter(String.format("User ID: %s", event.getUser().getId()));
			embed.setColor(LoggerListener.COLOR_GREEN);
			
			LoggerClient.get().queue(event.getGuild(), LoggerType.MEMBER_JOIN, embed.build());
		});
	}
	
	/* TODO: Check for kick */
	/* TODO: Make stayed for use more than just days */
	public void onGuildMemberLeave(GuildMemberLeaveEvent event) {
		Document logger = LoggerClient.get().getLogger(event.getGuild(), LoggerType.MEMBER_LEAVE);
		if(logger == null) {
			return;
		}
		
		this.delay(() -> {
			WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
			embed.setDescription(String.format("**%s** left the server", event.getUser().getAsTag()));
			embed.setAuthor(event.getUser().getAsTag(), event.getUser().getEffectiveAvatarUrl());
			embed.setTimestamp(ZonedDateTime.now());
			embed.setFooter(String.format("User ID: %s", event.getUser().getId()));
			embed.setColor(LoggerListener.COLOR_RED);
			
			LoggerClient.get().queue(event.getGuild(), LoggerType.MEMBER_LEAVE, embed.build());
		});
	}
	
	/* TODO: Add support for multiple roles */
	/* TODO: Add who it was removed by */
	public void onGuildMemberRoleAdd(GuildMemberRoleAddEvent event) {
		Document logger = LoggerClient.get().getLogger(event.getGuild(), LoggerType.MEMBER_ROLE_ADD);
		if(logger == null) {
			return;
		}
		
		this.delay(() -> {
			WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
			embed.setDescription(String.format("The role %s was added to `%s`", event.getRoles().get(0).getAsMention(), event.getMember().getEffectiveName()));
			embed.setAuthor(event.getMember().getEffectiveName(), event.getUser().getEffectiveAvatarUrl());
			embed.setTimestamp(ZonedDateTime.now());
			embed.setFooter(String.format("Role ID: %s", event.getRoles().get(0).getId()));
			embed.setColor(LoggerListener.COLOR_GREEN);
			
			LoggerClient.get().queue(event.getGuild(), LoggerType.MEMBER_ROLE_ADD, embed.build());
		});
	}
	
	/* TODO: Add support for multiple roles */
	/* TODO: Add who it was removed by */
	public void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event) {
		Document logger = LoggerClient.get().getLogger(event.getGuild(), LoggerType.MEMBER_ROLE_REMOVE);
		if(logger == null) {
			return;
		}
		
		this.delay(() -> {
			WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
			embed.setDescription(String.format("The role %s was removed from `%s`", event.getRoles().get(0).getAsMention()));
			embed.setAuthor(event.getMember().getEffectiveName(), event.getUser().getEffectiveAvatarUrl());
			embed.setTimestamp(ZonedDateTime.now());
			embed.setFooter(String.format("Role ID: %s", event.getRoles().get(0).getId()));
			embed.setColor(LoggerListener.COLOR_RED);
			
			LoggerClient.get().queue(event.getGuild(), LoggerType.MEMBER_ROLE_REMOVE, embed.build());	
		});
	}
}