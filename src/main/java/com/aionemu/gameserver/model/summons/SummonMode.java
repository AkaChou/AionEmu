package com.aionemu.gameserver.model.summons;

/**
 * 召唤物 Mode 枚举。
 * Summon Mode enumeration.
 */

public enum SummonMode {
	/** 攻击 / Attack. */
	ATTACK(0), GUARD(1), REST(2), RELEASE(3), UNK(5);

	private int id;

	private SummonMode(int id) {
		this.id = id;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 按 ID 返回 summon mode / Returns the summon mode by id */
	public static SummonMode getSummonModeById(int id) {
		for (SummonMode mode : values()) {
			if (mode.getId() == id) {
				return mode;
			}
		}
		return null;
	}
}
