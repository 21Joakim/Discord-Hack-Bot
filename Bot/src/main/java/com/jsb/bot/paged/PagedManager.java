package com.jsb.bot.paged;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;

public class PagedManager implements EventListener {
	
	public static PagedManager INSTANCE = new PagedManager();
	
	public static PagedManager get() {
		return PagedManager.INSTANCE;
	}
	
	private PagedManager() {}
	
	private final ScheduledExecutorService timeoutScheduler = Executors.newSingleThreadScheduledExecutor();
	private final Map<PagedResult<?>, ScheduledFuture<?>> timeoutFutures = new HashMap<>();
	
	private final Map<Long, Map<Long, Map<Long, PagedResult<?>>>> pagedResults = new HashMap<>();
	
	private PagedResult<?> putPagedResult(Message message, PagedResult<?> pagedResult) {
		return this.pagedResults.computeIfAbsent(message.getGuild().getIdLong(), id -> new HashMap<>())
			.computeIfAbsent(message.getTextChannel().getIdLong(), id -> new HashMap<>())
			.put(message.getAuthor().getIdLong(), pagedResult);
	}
	
	private PagedResult<?> getPagedResult(Message message) {
		Map<Long, Map<Long, PagedResult<?>>> channelMap = this.pagedResults.get(message.getGuild().getIdLong());
		if(channelMap != null) {
			Map<Long, PagedResult<?>> authorMap = channelMap.get(message.getChannel().getIdLong());
			if(authorMap != null) {
				return authorMap.get(message.getAuthor().getIdLong());
			}
		}
		
		return null;
	}
	
	private void removePagedResult(Message message, PagedResult<?> pagedResult) {
		Map<Long, PagedResult<?>> authorMap = this.pagedResults.get(message.getGuild().getIdLong())
			.get(message.getTextChannel().getIdLong());
		
		if(pagedResult.equals(authorMap.get(message.getAuthor().getIdLong()))) {
			authorMap.remove(message.getAuthor().getIdLong());
		}
	}
	
	public void send(Message message, PagedResult<?> pagedResult) {
		PagedResult<?> previousPagedResult = putPagedResult(message, pagedResult);
		if(previousPagedResult != null) {
			this.timeoutFutures.get(previousPagedResult).cancel(false);
		}
		
		message.getChannel().sendMessage(pagedResult.getCurrentPageAsEmbed().build()).queue(message0 -> {
			pagedResult.setMessage(message0);
		});
		
		this.timeoutFutures.put(pagedResult, this.timeoutScheduler.schedule(() -> {
			pagedResult.timeout();
			
			this.removePagedResult(message, pagedResult);
		}, pagedResult.getTimeoutTime(), pagedResult.getTimeoutUnit()));
	}
	
	public boolean onNext(Message message) {
		PagedResult<?> pagedResult = this.getPagedResult(message);
		if(pagedResult != null) {
			if(pagedResult.nextPage()) {
				if(pagedResult.getMessage() != null) {
					pagedResult.getMessage().editMessage(pagedResult.getCurrentPageAsEmbed().build()).queue(null, exception -> {
						this.removePagedResult(message, pagedResult);
						this.timeoutFutures.get(pagedResult).cancel(false);
					});
				}
				
				if(message.getGuild().getSelfMember().hasPermission(message.getTextChannel(), Permission.MESSAGE_MANAGE)) {
					message.delete().queue($ -> {}, $ -> {});
				}
				
				return true;
			}
		}
		
		return false;
	}
	
	public boolean onPrevious(Message message) {
		PagedResult<?> pagedResult = this.getPagedResult(message);
		if(pagedResult != null) {
			if(pagedResult.previousPage()) {
				if(pagedResult.getMessage() != null) {
					pagedResult.getMessage().editMessage(pagedResult.getCurrentPageAsEmbed().build()).queue(null, exception -> {
						this.removePagedResult(message, pagedResult);
						this.timeoutFutures.get(pagedResult).cancel(false);
					});
				}
				
				if(message.getGuild().getSelfMember().hasPermission(message.getTextChannel(), Permission.MESSAGE_MANAGE)) {
					message.delete().queue($ -> {}, $ -> {});
				}
				
				return true;
			}
		}
		
		return false;
	}
	
	public boolean onCancel(Message message) {
		PagedResult<?> pagedResult = this.getPagedResult(message);
		if(pagedResult != null) {
			pagedResult.cancel();
			
			if(pagedResult.getMessage() != null) {
				pagedResult.getMessage().delete().queue();
			}
			
			if(message.getGuild().getSelfMember().hasPermission(message.getTextChannel(), Permission.MESSAGE_MANAGE)) {
				message.delete().queue();
			}
			
			this.removePagedResult(message, pagedResult);
			this.timeoutFutures.get(pagedResult).cancel(false);
			
			return true;
		}
		
		return false;
	}
	
	public boolean onGoto(Message message, int page) {
		PagedResult<?> pagedResult = this.getPagedResult(message);
		if(pagedResult != null) {
			if(pagedResult.setPage(page)) {
				if(pagedResult.getMessage() != null) {
					pagedResult.getMessage().editMessage(pagedResult.getCurrentPageAsEmbed().build()).queue(null, exception -> {
						this.removePagedResult(message, pagedResult);
						this.timeoutFutures.get(pagedResult).cancel(false);
					});
				}
				
				if(message.getGuild().getSelfMember().hasPermission(message.getTextChannel(), Permission.MESSAGE_MANAGE)) {
					message.delete().queue();
				}
				
				return true;
			}
		}
		
		return false;
	}
	
	public boolean onSelect(Message message, int index) {
		PagedResult<?> pagedResult = this.getPagedResult(message);
		if(pagedResult != null) {
			if(pagedResult.isSelectable()) {
				if(pagedResult.select(index)) {
					if(pagedResult.getMessage() != null) {
						pagedResult.getMessage().delete().queue();
					}
					
					if(message.getGuild().getSelfMember().hasPermission(message.getTextChannel(), Permission.MESSAGE_MANAGE)) {
						message.delete().queue();
					}
					
					this.removePagedResult(message, pagedResult);
					this.timeoutFutures.get(pagedResult).cancel(false);
				}
				
				return true;
			}
		}
		
		return false;
	}
	
	public List<String> nextTriggers = List.of("n", "next page", "nextpage", "next");
	public List<String> previousTriggers = List.of("p", "previous page", "previouspage", "previous", "prev");
	public List<String> cancelTriggers = List.of("cancel", "c");
	public List<String> gotoTriggers = List.of("go to page", "goto page", "gotopage", "go to", "goto", "page");
	
	public void onEvent(GenericEvent event) {
		if(event instanceof GuildMessageReceivedEvent) {
			Message message = ((GuildMessageReceivedEvent) event).getMessage();
			String content = message.getContentRaw().toLowerCase();
			
			if(this.nextTriggers.contains(content)) {
				if(this.onNext(message)) {
					return;
				}
			}
			
			if(this.previousTriggers.contains(content)) {
				if(this.onPrevious(message)) {
					return;
				}
			}
			
			if(this.cancelTriggers.contains(content)) {
				if(this.onCancel(message)) {
					return;
				}
			}
			
			for(String trigger : this.gotoTriggers) {
				if(!trigger.equals(content)) {
					continue;
				}
				
				content = content.substring(trigger.length());
				
				try {
					if(this.onGoto(message, Integer.parseInt(content))) {
						return;
					}
				}catch(NumberFormatException e) {}
			}
			
			try {
				if(this.onSelect(message, Integer.parseInt(content))) {
					return;
				}
			}catch(NumberFormatException e) {}
		}
	}
}