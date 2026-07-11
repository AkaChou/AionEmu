package com.aionemu.gameserver.model;

/**
 * 公告模型。
 * Announcement model.
 *
 * @author Divinity
 */
public class Announcement {

	private int id;
	private String faction;
	private String announce;
	private String chatType;
	private int delay;

	/**
	 * 无公告 ID 的构造。
	 * Constructor without announcement id.
	 *
	 * announcement text
	 * faction
	 * chat type
	 * @param delay 延迟秒数 / delay in seconds
	 */
	public Announcement(String announce, String faction, String chatType, int delay) {
		this.announce = announce;

		// 校验阵营语法 / Validate faction syntax
		if (!faction.equalsIgnoreCase("ELYOS") && !faction.equalsIgnoreCase("ASMODIANS")) {
			faction = "ALL";
		}
		this.faction = faction;
		this.chatType = chatType;
		this.delay = delay;
	}

	/**
	 * 带公告 ID 的构造。
	 * Constructor with announcement id.
	 *
	 * @param id 公告 ID / announcement id
	 * announcement text
	 * faction
	 * chat type
	 * @param delay 延迟秒数 / delay in seconds
	 */
	public Announcement(int id, String announce, String faction, String chatType, int delay) {
		this.id = id;
		this.announce = announce;

		// 校验阵营语法 / Validate faction syntax
		if (!faction.equalsIgnoreCase("ELYOS") && !faction.equalsIgnoreCase("ASMODIANS")) {
			faction = "ALL";
		}
		this.faction = faction;
		this.chatType = chatType;
		this.delay = delay;
	}

	/**
	 * 返回公告 ID；不存在时返回 -1。
	 * Returns the announcement id, or -1 if missing.
	 *
	 * announcement id
	 */
	public int getId() {
		if (id != 0) {
			return id;
		} else {
			return -1;
		}
	}

	/**
	 * 返回公告正文。
	 * Returns the announcement text.
	 *
	 * announcement text
	 */
	public String getAnnounce() {
		return announce;
	}

	/**
	 * 返回公告阵营字符串：ELYOS / ASMODIANS / ALL。
	 * ASMODIANS / ALL. / ASMODIANS / ALL.
	 *
	 * @return 阵营字符串 / faction string
	 */
	public String getFaction() {
		return faction;
	}

	/**
	 * 返回公告阵营枚举：{@link Race#ELYOS} / {@link Race#ASMODIANS}；ALL 时为 null。
	 * {@link Race#ASMODIANS}; null for ALL. / {@link Race#ASMODIANS}; null for ALL.
	 *
	 * faction race
	 */
	public Race getFactionEnum() {
		if (faction.equalsIgnoreCase("ELYOS")) {
			return Race.ELYOS;
		} else if (faction.equalsIgnoreCase("ASMODIANS")) {
			return Race.ASMODIANS;
		}
		return null;
	}

	/**
	 * 返回聊天类型字符串（用于入库）。
	 * Returns the chat type string (for DB insert).
	 *
	 * @return 聊天类型字符串 / chat type string
	 */
	public String getType() {
		return chatType;
	}

	/**
	 * 返回聊天类型枚举。
	 * Returns the chat type enum.
	 *
	 * chat type
	 */
	public ChatType getChatType() {
		if (chatType.equalsIgnoreCase("System")) {
			return ChatType.YELLOW;
		} else if (chatType.equalsIgnoreCase("White")) {
			return ChatType.WHITE_CENTER;
		} else if (chatType.equalsIgnoreCase("Yellow")) {
			return ChatType.YELLOW_CENTER;
		} else if (chatType.equalsIgnoreCase("Shout")) {
			return ChatType.SHOUT;
		} else if (chatType.equalsIgnoreCase("Orange")) {
			return ChatType.GROUP_LEADER;
		} else {
			return ChatType.BRIGHT_YELLOW_CENTER;
		}
	}

	/**
	 * 返回公告延迟（秒）。
	 * Returns the announcement delay in seconds.
	 *
	 * delay in seconds
	 */
	public int getDelay() {
		return delay;
	}
}
