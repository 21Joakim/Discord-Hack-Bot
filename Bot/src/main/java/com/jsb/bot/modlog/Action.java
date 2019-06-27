package com.jsb.bot.modlog;

public enum Action {
	BAN("Ban", true),
	UNBAN("Unban", false),
	KICK("Kick", true),
	MUTE("Mute", true),
	UNMUTE("Unmute", false),
	WARN("Warn", false),
	VOICE_KICK("Voice Kick", false);
	
	private String name;
	private boolean warnAction;
	
	private Action(String name, boolean warnAction) {
		this.name = name;
		this.warnAction = warnAction; 
	}
	
	public String getName() {
		return this.name;
	}
	
	/**
	 * @return whether or not the action can be used as an action when someone has been warned x amount of times
	 */
	public boolean isWarnAction() {
		return this.warnAction;
	}
}
