package com.jsb.bot.paged.event;

import com.jsb.bot.paged.PagedResult;

public class UpdateEvent<Type> {
	
	public static enum UpdateType {
		PAGE_NEXT,
		PAGE_PREVIOUS,
		PAGE_JUMP;
	}
	
	public UpdateEvent(PagedResult<Type> pagedResult, UpdateType type, int oldPage, int newPage) {
		this.pagedResult = pagedResult;
		this.type = type;
		this.oldPage = oldPage;
		this.newPage = newPage;
	}
	
	public final PagedResult<Type> pagedResult;
	
	public final UpdateType type;
	
	public final int oldPage;
	public final int newPage;
	
}