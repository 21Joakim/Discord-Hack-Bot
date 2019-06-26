package com.jsb.bot.mute;

public enum MuteEvasionType {

	BAN_ON_LEAVE("If a user is muted and they leave they will be banned."),
	BAN_ON_JOIN("If a user leaves and then joins the server while being muted they will be banned, providing the mute hasn't ran out in that time."),
	WARN_ON_JOIN("If a user leaves and then joins the server while being muted they will be warned a set value, providing the mute hasn't ran out in that time."),
	REMUTE_ON_JOIN("If a user leaves and then joins the server while being muted they will be muted again, providing the mute hasn't ran out in that time."),
	KICK_ON_JOIN("If a user leaves and then joins the server while being muted they will be kicked, providing the mute hasn't ran out in that time.");
	
	private String description;
	
	private MuteEvasionType(String description) {
		this.description = description;
	}
	
	public String getDescription() {
		return this.description;
	}
	
}
