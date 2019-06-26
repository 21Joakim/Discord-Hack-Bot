package com.jsb.bot.modlog;

public enum Action {
	BAN("Ban"),
	UNBAN("Unban"),
	KICK("Kick"),
	MUTE("Mute"),
	UNMUTE("Unmute"),
	WARN("Warn"),
	VOICE_KICK("Voice Kick");
	
	private String name;
	
	private Action(String name) {
		this.name = name;
	}
	
	public String getName() {
		return this.name;
	}
}
