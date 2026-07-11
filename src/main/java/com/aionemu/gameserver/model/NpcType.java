package com.aionemu.gameserver.model;

/**
 * NPC 类型枚举。
 * Npc Type enumeration.
 */

public enum NpcType {
	/** 可攻击 / Attackable. */
	ATTACKABLE(0), PEACE(2), AGGRESSIVE(8), INVULNERABLE(10), NON_ATTACKABLE(38), UNKNOWN(54);

	private int someClientSideId;

	private NpcType(int id) {
		this.someClientSideId = id;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return someClientSideId;
	}
}
