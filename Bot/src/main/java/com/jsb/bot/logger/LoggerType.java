package com.jsb.bot.logger;

import java.util.EnumSet;

public enum LoggerType {
	/** {@link LoggerListener#onGuildMemberJoin(net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent)} */
	MEMBER_JOIN(Category.MEMBER),
	/** {@link LoggerListener#onGuildMemberLeave(net.dv8tion.jda.api.events.guild.member.GuildMemberLeaveEvent)} */
	MEMBER_LEAVE(Category.MEMBER),
	/** {@link LoggerListener#onGuildBan(net.dv8tion.jda.api.events.guild.GuildBanEvent)} */
	MEMBER_BAN(Category.MEMBER),
	/** {@link LoggerListener#onGuildUnban(net.dv8tion.jda.api.events.guild.GuildUnbanEvent)} */
	MEMBER_UNBAN(Category.MEMBER),
	/** {@link LoggerListener#onGuildMemberUpdateNickname(net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent)} */
	MEMBER_NICKNAME_CHANGE(Category.MEMBER),
	/** {@link LoggerListener#onGuildMemberRoleAdd(net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent)} */
	MEMBER_ROLE_ADD(Category.MEMBER),
	/** {@link LoggerListener#onGuildMemberRoleRemove(net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent)} */
	MEMBER_ROLE_REMOVE(Category.MEMBER),
	
	/** {@link LoggerListener#onRoleCreate(net.dv8tion.jda.api.events.role.RoleCreateEvent)} */
	ROLE_CREATE(Category.ROLE),
	/** {@link LoggerListener#onRoleDelete(net.dv8tion.jda.api.events.role.RoleDeleteEvent)} */
	ROLE_DELETE(Category.ROLE),
	/** {@link LoggerListener#onRoleUpdateColor(net.dv8tion.jda.api.events.role.update.RoleUpdateColorEvent)} */
	ROLE_UPDATE_COLOR(Category.ROLE),
	/** {@link LoggerListener#onRoleUpdateHoisted(net.dv8tion.jda.api.events.role.update.RoleUpdateHoistedEvent)} */
	ROLE_UPDATE_HOISTED(Category.ROLE),
	/** {@link LoggerListener#onRoleUpdateMentionable(net.dv8tion.jda.api.events.role.update.RoleUpdateMentionableEvent)} */
	ROLE_UPDATE_MENTIONABLE(Category.ROLE),
	/** {@link LoggerListener#onRoleUpdateName(net.dv8tion.jda.api.events.role.update.RoleUpdateNameEvent)} */
	ROLE_UPDATE_NAME(Category.ROLE),
	/** {@link LoggerListener#onRoleUpdatePermissions(net.dv8tion.jda.api.events.role.update.RoleUpdatePermissionsEvent)} */
	ROLE_UPDATE_PERMISSIONS(Category.ROLE),
	// ROLE_UPDATE_POSITION(Category.ROLE)
	
	/** {@link LoggerListener#onEmoteAdded(net.dv8tion.jda.api.events.emote.EmoteAddedEvent)} */
	EMOTE_CREATE(Category.EMOTE),
	/** {@link LoggerListener#onEmoteRemoved(net.dv8tion.jda.api.events.emote.EmoteRemovedEvent)} */
	EMOTE_DELETE(Category.EMOTE),
	/** {@link LoggerListener#onEmoteUpdateName(net.dv8tion.jda.api.events.emote.update.EmoteUpdateNameEvent)} */
	EMOTE_UPDATE_NAME(Category.EMOTE),
	EMOTE_UPDATE_ROLES(Category.EMOTE),
	
	/** {@link LoggerListener#onGuildUpdateAfkChannel(net.dv8tion.jda.api.events.guild.update.GuildUpdateAfkChannelEvent)} */
	GUILD_UPDATE_AFK_CHANNEL(Category.GUILD),
	/** {@link LoggerListener#onGuildUpdateAfkTimeout(net.dv8tion.jda.api.events.guild.update.GuildUpdateAfkTimeoutEvent)} */
	GUILD_UPDATE_AFK_TIMEOUT(Category.GUILD),
	/** {@link LoggerListener#onGuildUpdateExplicitContentLevel(net.dv8tion.jda.api.events.guild.update.GuildUpdateExplicitContentLevelEvent)} */
	GUILD_UPDATE_EXPLICIT_CONTENT_LEVEL(Category.GUILD),
	/** {@link LoggerListener#onGuildUpdateIcon(net.dv8tion.jda.api.events.guild.update.GuildUpdateIconEvent)} */
	GUILD_UPDATE_ICON(Category.GUILD),
	/** {@link LoggerListener#onGuildUpdateMFALevel(net.dv8tion.jda.api.events.guild.update.GuildUpdateMFALevelEvent)} */
	GUILD_UPDATE_MFA_LEVEL(Category.GUILD), // Not tested
	/** {@link LoggerListener#onGuildUpdateName(net.dv8tion.jda.api.events.guild.update.GuildUpdateNameEvent)} */
	GUILD_UPDATE_NAME(Category.GUILD),
	/** {@link LoggerListener#onGuildUpdateNotificationLevel(net.dv8tion.jda.api.events.guild.update.GuildUpdateNotificationLevelEvent)} */
	GUILD_UPDATE_NOTIFICATION_LEVEL(Category.GUILD),
	/** {@link LoggerListener#onGuildUpdateOwner(net.dv8tion.jda.api.events.guild.update.GuildUpdateOwnerEvent)} */
	GUILD_UPDATE_OWNER(Category.GUILD), // Not tested
	/** {@link LoggerListener#onGuildUpdateRegion(net.dv8tion.jda.api.events.guild.update.GuildUpdateRegionEvent)} */
	GUILD_UPDATE_REGION(Category.GUILD),
	/** {@link LoggerListener#onGuildUpdateSplash(net.dv8tion.jda.api.events.guild.update.GuildUpdateSplashEvent)} */
	GUILD_UPDATE_SPLASH(Category.GUILD),
	/** {@link LoggerListener#onGuildUpdateSystemChannel(net.dv8tion.jda.api.events.guild.update.GuildUpdateSystemChannelEvent)} */
	GUILD_UPDATE_SYSTEM_CHANNEL(Category.GUILD),
	/** {@link LoggerListener#onGuildUpdateVerificationLevel(net.dv8tion.jda.api.events.guild.update.GuildUpdateVerificationLevelEvent)} */
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
	
	/** {@link LoggerListener#onGuildMessageReceived(net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent)} */
	MESSAGE_RECEIVE(Category.MESSAGE), // Used for testing
	MESSAGE_EDIT(Category.MESSAGE),
	MESSAGE_DELETE(Category.MESSAGE)
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
	
	private EnumSet<Category> categories;
	
	private LoggerType(Category... categories) {
		this.categories = categories.length > 0
			? EnumSet.of(categories[0], categories)
			: EnumSet.noneOf(Category.class);
	}
	
	public EnumSet<Category> getCategories() {
		return this.categories;
	}
	
	public static EnumSet<LoggerType> getByCategory(Category category) {
		EnumSet<LoggerType> types = EnumSet.noneOf(LoggerType.class);
		
		for(LoggerType type : LoggerType.values()) {
			if(type.categories.contains(category)) {
				types.add(type);
			}
		}
		
		return types;
	}
}