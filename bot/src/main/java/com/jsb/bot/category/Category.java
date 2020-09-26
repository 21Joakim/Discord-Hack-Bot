package com.jsb.bot.category;

import com.jockie.bot.core.category.impl.CategoryImpl;

public class Category {
	
	public static final CategoryImpl BASIC = new CategoryImpl("Basic", null);
	public static final CategoryImpl MODLOG = new CategoryImpl("Modlog", null);
	public static final CategoryImpl BOT_PERMISSIONS = new CategoryImpl("Bot Permissions", null);
	public static final CategoryImpl PRUNE = new CategoryImpl("Prune", null);
	public static final CategoryImpl TEMPLATE = new CategoryImpl("Template", null);
	public static final CategoryImpl LOGGER = new CategoryImpl("Logger", null);
	public static final CategoryImpl WARN = new CategoryImpl("Warn", null);
	
	public static final CategoryImpl[] ALL = {BASIC, MODLOG, BOT_PERMISSIONS, PRUNE, TEMPLATE, LOGGER, WARN};
	
}