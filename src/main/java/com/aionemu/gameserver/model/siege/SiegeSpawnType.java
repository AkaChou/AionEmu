package com.aionemu.gameserver.model.siege;

/**
 * 要塞刷新点类型枚举。
 * Siege Spawn Type enumeration.
 */

public enum SiegeSpawnType {
	/** 和平。 / Peace. */
	PEACE(0),
	/** 守卫。 / Guard. */
	GUARD(1),
	/** 神器。 / Artifact. */
	ARTIFACT(2),
	/** 保护者。 / Protector. */
	PROTECTOR(3),
	/** 地雷。 / Mine. */
	MINE(4),
	/** 传送门。 / Portal. */
	PORTAL(5),
	/** 发电机。 / Generator. */
	GENERATOR(6),
	/** 复活泉。 / Spring. */
	SPRING(7),
	/** 种族保护者。 / Race Protector. */
	RACEPROTECTOR(8);

	private int id;

	private SiegeSpawnType(int id) {
		this.id = id;
	}

	/**
	 * @return 枚举 ID / the id
	 */
	public int getId() {
		return id;
	}
}
