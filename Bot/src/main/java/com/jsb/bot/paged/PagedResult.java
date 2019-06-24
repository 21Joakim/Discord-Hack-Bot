package com.jsb.bot.paged;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import com.jockie.bot.core.command.impl.CommandEvent;
import com.jsb.bot.paged.event.SelectEvent;
import com.jsb.bot.paged.event.UpdateEvent;
import com.jsb.bot.paged.event.UpdateEvent.UpdateType;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

public class PagedResult<Type> {
	
	private long timeoutTime = 2;
	private TimeUnit timeoutUnit = TimeUnit.MINUTES;
	
	private int selectedPage = 1;
	private int entriesPerPage = 10;
	
	private int pages = -1;
	
	private boolean listIndexes = true;
	private boolean listIndexesContinuously = false;
	
	private List<? extends Type> entries;
	
	private Function<? super Type, String> displayFunction;
	
	private String seperator = " - ";
	
	private Consumer<SelectEvent<Type>> onSelect;
	private Consumer<UpdateEvent<Type>> onUpdate;
	private Consumer<PagedResult<Type>> onCancel;
	private Consumer<PagedResult<Type>> onTimeout;
	
	private EmbedBuilder embed = new EmbedBuilder();
	
	private Message message;
	
	public PagedResult(List<? extends Type> entries) {
		this.entries = entries;
		
		this.pages = this.getPages();
	}
	
	public PagedResult(List<? extends Type> entries, Function<? super Type, String> displayFunction, Consumer<SelectEvent<Type>> onSelect) {
		this(entries);
		
		this.displayFunction = displayFunction;
		this.onSelect = onSelect;
	}
	
	public PagedResult<Type> onSelect(Consumer<SelectEvent<Type>> onSelect) {
		this.onSelect = onSelect;
		
		return this;
	}
	
	public PagedResult<Type> onUpdate(Consumer<UpdateEvent<Type>> onUpdate) {
		this.onUpdate = onUpdate;
		
		return this;
	}
	
	public PagedResult<Type> onCancel(Consumer<PagedResult<Type>> onCancel) {
		this.onCancel = onCancel;
		
		return this;
	}
	
	public PagedResult<Type> onTimeout(Consumer<PagedResult<Type>> onTimeout) {
		this.onTimeout = onTimeout;
		
		return this;
	}
	
	public boolean select(int index) {
		int[] bounds = this.getPageBounds();
		
		if(index > 0 && index <= bounds[1]) {
			/* Do nothing */
		}else if(this.isListIndexesContinuously()) {
			if(index <= bounds[0] || index > bounds[1]) {
				return false;
			}
			
			/* Do nothing */
		}else{
			return false;
		}
		
		if(this.onSelect != null) {
			int actualIndex = (this.selectedPage - 1) * this.entriesPerPage + (index - 1);
			
			SelectEvent<Type> event = new SelectEvent<>(
				this, this.selectedPage, 
				index, actualIndex,
				this.entries.get(actualIndex)
			);
			
			this.onSelect.accept(event);
		}
		
		return true;
	}
	
	public void cancel() {
		if(this.onCancel != null) {
			this.onCancel.accept(this);
		}
	}
	
	public void timeout() {
		if(this.onTimeout != null) {
			this.onTimeout.accept(this);
		}
	}
	
	public PagedResult<Type> setEntriesPerPage(int entriesPerPage) {
		this.entriesPerPage = entriesPerPage;
		
		return this;
	}
	
	public PagedResult<Type> setListIndexes(boolean listIndexes) {
		this.listIndexes = listIndexes;
		
		return this;
	}
	
	public PagedResult<Type> setListIndexesContinuously(boolean listIndexes) {
		this.listIndexes = listIndexes;
		
		return this;
	}
	
	public PagedResult<Type> setDisplayFunction(Function<? super Type, String> displayFunction) {
		this.displayFunction = displayFunction;
		
		return this;
	}
	
	public PagedResult<Type> setSeperator(String seperator) {
		this.seperator = seperator;
		
		return this;
	}
	
	public PagedResult<Type> setTimeout(long time, TimeUnit unit) {
		this.timeoutTime = time;
		this.timeoutUnit = unit;
		
		return this;
	}
	
	public PagedResult<Type> setMessage(Message message) {
		this.message = message;
		
		return this;
	}
	
	public boolean setPage(int page) {
		if(page > this.pages) {
			return false;
		}
		
		if(page < 1) {
			return false;
		}
		
		if(page == this.pages) {
			return false;
		}
		
		int before = this.selectedPage;
		this.selectedPage = page;
		
		if(this.onUpdate != null) {
			UpdateEvent<Type> event = new UpdateEvent<>(this, UpdateType.PAGE_JUMP, before, this.selectedPage);
			
			this.onUpdate.accept(event);
		}
		
		return true;
	}
	
	public boolean nextPage() {
		if(this.selectedPage + 1 > this.pages) {
			return false;
		}
		
		int before = this.selectedPage;
		this.selectedPage = this.selectedPage + 1;
		
		if(this.onUpdate != null) {
			UpdateEvent<Type> event = new UpdateEvent<>(this, UpdateType.PAGE_NEXT, before, this.selectedPage);
			
			this.onUpdate.accept(event);
		}
		
		return true;
	}
	
	public boolean previousPage() {
		if(this.selectedPage - 1 < 1) {
			return false;
		}
		
		int before = this.selectedPage;
		this.selectedPage = this.selectedPage - 1;
		
		if(this.onUpdate != null) {
			UpdateEvent<Type> event = new UpdateEvent<>(this, UpdateType.PAGE_PREVIOUS, before, this.selectedPage);
			
			this.onUpdate.accept(event);
		}
		
		return true;
	}
	
	public boolean isListIndexes() {
		return this.listIndexes;
	}
	
	public boolean isListIndexesContinuously() {
		return this.listIndexesContinuously;
	}
	
	public String getSeperator() {
		return this.seperator;
	}
	
	public int getEntriesPerPage() {
		return this.entriesPerPage;
	}
	
	public long getTimeoutTime() {
		return this.timeoutTime;
	}
	
	public TimeUnit getTimeoutUnit() {
		return this.timeoutUnit;
	}
	
	public Message getMessage() {
		return this.message;
	}
	
	public boolean isSelectable() {
		return this.onSelect != null;
	}
	
	public int getSelectedPage() {
		return this.selectedPage;
	}
	
	public int getPages() {
		if(this.pages == -1) {
			this.pages = (int) Math.ceil(this.entries.size()/(double) this.entriesPerPage);
		}
		
		return this.pages;
	}
	
	public int[] getPageBounds() {
		int from = (this.selectedPage - 1) * this.entriesPerPage;
		int to = this.selectedPage == this.pages ? this.entries.size() - from : this.entriesPerPage;
		
		return new int[] { from, to };
	}
	
	public List<? extends Type> getCurrentPageEntries() {
		int[] bounds = this.getPageBounds();
		
		return this.entries.subList(bounds[0], bounds[0] + bounds[1]);
	}
	
	public EmbedBuilder getCurrentPageAsEmbed() {
		List<? extends Type> entries = this.getCurrentPageEntries();
		
		/* Clear description */
		this.embed.setDescription("");
		
		/* Page header */
		this.embed.appendDescription("Page **" + this.selectedPage + "**/**" + this.pages + "**\n");
		
		/* Build page */
		for(int i = 0; i < entries.size(); i++) {
			this.embed.appendDescription("\n");
			
			if(this.listIndexes) {
				if(this.listIndexesContinuously) {
					this.embed.appendDescription((this.selectedPage - 1) * this.entriesPerPage + (i + 1) + this.seperator);
				}else{
					this.embed.appendDescription((i + 1) + this.seperator);
				}
			}
			
			this.embed.appendDescription(this.displayFunction.apply(entries.get(i)));
		}
		
		StringBuilder footer = new StringBuilder();
		if(this.selectedPage + 1 <= this.pages) {
			footer.append("next page | ");
		}

		if(this.selectedPage - 1 > 0) {
			footer.append("previous page | ");
		}

		if(this.pages > 2) {
			footer.append("go to page <page> | ");
		}
		
		footer.append("cancel");
		
		return this.embed.setFooter(footer.toString());
	}
	
	public EmbedBuilder getEmbed() {
		return this.embed;
	}
	
	public void send(CommandEvent event) {
		this.send(event.getMessage());
	}
	
	public void send(Message message) {
		PagedManager.get().send(message, this);
	}
}