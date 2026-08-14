package com.aionemu.gameserver.model.summons;

/**
 * 召唤物解散类型：登出/距离/命令/未指定。
 * Unsummon type: logout/distance/command/unspecified.
 */

public enum UnsummonType {
	/** 登出 / Logout */
	LOGOUT,
	/** 距离过远 / Distance */
	DISTANCE,
	/** 玩家命令 / Command */
	COMMAND,
	/** 未指定 / Unspecified */
	UNSPECIFIED;
}
