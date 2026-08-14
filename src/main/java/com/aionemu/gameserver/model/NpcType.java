package com.aionemu.gameserver.model;

/**
 * NPC 类型枚举。
 * Npc Type enumeration.
 */

public enum NpcType {
	/** 可攻击 / Attackable. */
	ATTACKABLE(0),
	/** 和平 / Peace */
	PEACE(2),
	/** 好战 / Aggressive */
	AGGRESSIVE(8),
	/** 无敌 / Invulnerable */
	INVULNERABLE(10),
	/** 不可攻击 / Non Attackable */
	NON_ATTACKABLE(38),
	/** 未知 / Unknown */
	UNKNOWN(54);

	private int someClientSideId;

	private NpcType(int id) {
		this.someClientSideId = id;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return someClientSideId;
	}
}
