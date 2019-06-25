package com.jsb.bot.database.callback;

public interface Callback<T> {
	
	public void onResult(T result, Throwable e);
	
}