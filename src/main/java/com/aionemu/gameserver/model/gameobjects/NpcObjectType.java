package com.aionemu.gameserver.model.gameobjects;

/**
 * NPC 对象类型枚举。
 * Npc Object Type enumeration.
 */

public enum NpcObjectType {
	/** 普通 / Normal. */
	NORMAL(1), SUMMON(2), HOMING(16), TRAP(32), SKILLAREA(64), TOTEM(128), GROUPGATE(256), SERVANT(1024), PET(2048);

	private NpcObjectType(int id) {
		this.id = id;
	}

	private int id;

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}
}
