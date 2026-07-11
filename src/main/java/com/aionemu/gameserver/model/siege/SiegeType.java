package com.aionemu.gameserver.model.siege;

/**
 * 要塞类型枚举。
 * Siege Type enumeration.
 */

public enum SiegeType {
	/** 要塞 / Fortress. */
	FORTRESS(0), ARTIFACT(1), BOSSRAID_LIGHT(2), BOSSRAID_DARK(3), INDUN(4), UNDERPASS(5), TOWER(6);

	private int typeId;

	private SiegeType(int id) {
		this.typeId = id;
	}

	/** 返回类型 ID / Returns the type id */
	public int getTypeId() {
		return this.typeId;
	}
}
