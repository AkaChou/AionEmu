package com.aionemu.gameserver.model.siege;

/**
 * 要塞刷新点类型枚举。
 * Siege Spawn Type enumeration.
 */

public enum SiegeSpawnType {
	/** 和平 / Peace. */
	PEACE(0), GUARD(1), ARTIFACT(2), PROTECTOR(3), MINE(4), PORTAL(5), GENERATOR(6), SPRING(7), RACEPROTECTOR(8);

	private int id;

	private SiegeSpawnType(int id) {
		this.id = id;
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}
}
