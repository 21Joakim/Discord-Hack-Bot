package com.jsb.bot.logger;

public enum LoggerType {
	/** {@link LoggerListener#onGuildMemberJoin(net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent)} */
	MEMBER_JOIN(Category.MEMBER),
	/** {@link LoggerListener#onGuildMemberLeave(net.dv8tion.jda.api.events.guild.member.GuildMemberLeaveEvent)} */
	MEMBER_LEAVE(Category.MEMBER),
	/** {@link LoggerListener#onGuildBan(net.dv8tion.jda.api.events.guild.GuildBanEvent)} */
	MEMBER_BANNED(Category.MEMBER),
	MEMBER_UNBANNED(Category.MEMBER),
	/** {@link LoggerListener#onGuildMemberUpdateNickname(net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent)} */
	MEMBER_NICKNAME_CHANGE(Category.MEMBER),
	/** {@link LoggerListener#onGuildMemberRoleAdd(net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent)} */
	MEMBER_ROLE_ADD(Category.MEMBER),
	/** {@link LoggerListener#onGuildMemberRoleRemove(net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent)} */
	MEMBER_ROLE_REMOVE(Category.MEMBER),
	
	ROLE_CREATE(Category.ROLE),
	ROLE_DELETE(Category.ROLE),
	ROLE_UPDATE_COLOR(Category.ROLE),
	ROLE_UPDATE_HOISTED(Category.ROLE),
	ROLE_UPDATE_MENTIONABLE(Category.ROLE),
	ROLE_UPDATE_NAME(Category.ROLE),
	ROLE_UPDATE_PERMISSIONS(Category.ROLE),
	// ROLE_UPDATE_POSITION(Category.ROLE)
	
	EMOTE_CREATE(Category.EMOTE),
	EMOTE_DELETE(Category.EMOTE),
	EMOTE_UPDATE_NAME(Category.EMOTE),
	EMOTE_UPDATE_ROLES(Category.EMOTE),
	
	GUILD_UPDATE_AFK_CHANNEL(Category.GUILD),
	GUILD_UPDATE_AFK_TIMEOUT(Category.GUILD),
	GUILD_UPDATE_EXPLICIT_CONTENT_LEVEL(Category.GUILD),
	GUILD_UPDATE_ICON(Category.GUILD),
	GUILD_UPDATE_MFA_LEVEL(Category.GUILD),
	GUILD_UPDATE_NAME(Category.GUILD),
	GUILD_UPDATE_NOTIFICATION_LEVEL(Category.GUILD),
	GUILD_UPDATE_OWNER(Category.GUILD),
	GUILD_UPDATE_REGION(Category.GUILD),
	GUILD_UPDATE_SPLASH(Category.GUILD),
	GUILD_UPDATE_SYSTEM_CHANNEL(Category.GUILD),
	GUILD_UPDATE_VERIFICATION_LEVEL(Category.GUILD),
	
	TEXT_CHANNEL_CREATE(Category.TEXT_CHANNEL, Category.CHANNEL),
	TEXT_CHANNEL_DELETE(Category.TEXT_CHANNEL, Category.CHANNEL),
	TEXT_CHANNEL_UPDATE_NAME(Category.TEXT_CHANNEL, Category.CHANNEL),
	TEXT_CHANNEL_UPDATE_NSFW(Category.TEXT_CHANNEL, Category.CHANNEL),
	TEXT_CHANNEL_UPDATE_PARENT(Category.TEXT_CHANNEL, Category.CHANNEL),
	// TEXT_CHANNEL_UPDATE_PERMISSIONS(Category.TEXT_CHANNEL, Category.CHANNEL),
	// TEXT_CHANNEL_UPDATE_POSITION(Category.TEXT_CHANNEL, Category.CHANNEL),
	TEXT_CHANNEL_UPDATE_SLOWMODE(Category.TEXT_CHANNEL, Category.CHANNEL),
	TEXT_CHANNEL_UPDATE_TOPIC(Category.TEXT_CHANNEL, Category.CHANNEL),
	
	VOICE_CHANNEL_CREATE(Category.VOICE_CHANNEL, Category.CHANNEL),
	VOICE_CHANNEL_DELETE(Category.VOICE_CHANNEL, Category.CHANNEL),
	VOICE_CHANNEL_UPDATE_BITRATE(Category.VOICE_CHANNEL, Category.CHANNEL),
	VOICE_CHANNEL_UPDATE_NAME(Category.VOICE_CHANNEL, Category.CHANNEL),
	VOICE_CHANNEL_UPDATE_PARENT(Category.VOICE_CHANNEL, Category.CHANNEL),
	// VOICE_CHANNEL_UPDATE_PERMISSIONS(Category.VOICE_CHANNEL, Category.CHANNEL),
	// VOICE_CHANNEL_UPDATE_POSITION(Category.VOICE_CHANNEL, Category.CHANNEL),
	VOICE_CHANNEL_UPDATE_USER_LIMIT(Category.VOICE_CHANNEL, Category.CHANNEL),
	
	STORE_CHANNEL_CREATE(Category.STORE_CHANNEL, Category.CHANNEL),
	STORE_CHANNEL_DELETE(Category.STORE_CHANNEL, Category.CHANNEL),
	STORE_CHANNEL_UPDATE_NAME(Category.STORE_CHANNEL, Category.CHANNEL),
	// STORE_CHANNEL_UPDATE_PERMISSIONS(Category.STORE_CHANNEL, Category.CHANNEL),
	// STORE_CHANNEL_UPDATE_POSITION(Category.STORE_CHANNEL, Category.CHANNEL),
	
	CATEGORY_CHANNEL_CREATE(Category.CATEGORY_CHANNEL, Category.CHANNEL),
	CATEGORY_CHANNEL_DELETE(Category.CATEGORY_CHANNEL, Category.CHANNEL),
	CATEGORY_CHANNEL_UPDATE_NAME(Category.CATEGORY_CHANNEL, Category.CHANNEL),
	// CATEGORY_CHANNEL_UPDATE_PERMISSIONS(Category.CATEGORY_CHANNEL, Category.CHANNEL),
	// CATEGORY_CHANNEL_UPDATE_POSITION(Category.CATEGORY_CHANNEL, Category.CHANNEL)
	
	VOICE_DEAFEN(Category.VOICE),
	VOICE_MUTE(Category.VOICE),
	VOICE_JOIN(Category.VOICE),
	VOICE_LEAVE(Category.VOICE),
	VOICE_MOVE(Category.VOICE),
	VOICE_SUPRESS(Category.VOICE),
	
	MESSAGE_(Category.MESSAGE),
	;
	
	public static enum Category {
		MEMBER,
		ROLE,
		EMOTE,
		GUILD,
		VOICE,
		MESSAGE,
		CHANNEL,
		TEXT_CHANNEL,
		VOICE_CHANNEL,
		CATEGORY_CHANNEL,
		STORE_CHANNEL;
	}
	
	private Category[] categories;
	
	private LoggerType(Category... categories) {
		this.categories = categories;
	}
	
	public Category[] getCategories() {
		return this.categories;
	}
}