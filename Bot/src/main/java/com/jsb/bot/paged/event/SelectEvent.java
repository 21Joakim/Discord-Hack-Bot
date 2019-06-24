package com.jsb.bot.paged.event;

import com.jsb.bot.paged.PagedResult;

public class SelectEvent<Type> {
	
	public SelectEvent(PagedResult<Type> pagedResult, int page, int pageIndex, int index, Type entry) {
		this.pagedResult = pagedResult;
		this.page = page;
		this.pageIndex = pageIndex;
		this.index = index;
		this.entry = entry;
	}
	
	public final PagedResult<Type> pagedResult;
	
	public final int page, pageIndex, index;
	
	public final Type entry;
	
}