package com.aionemu.gameserver.model.summons;

/**
 * 召唤物模式：攻击/守护/休息/释放。
 * Summon mode: attack/guard/rest/release.
 */

public enum SummonMode {
	/** 攻击 / Attack */
	ATTACK(0),
	/** 守护 / Guard */
	GUARD(1),
	/** 休息 / Rest */
	REST(2),
	/** 释放 / Release */
	RELEASE(3),
	/** 未知 / Unknown */
	UNK(5);

	private int id;

	private SummonMode(int id) {
		this.id = id;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 按 ID 返回召唤物模式 / Returns the summon mode by id */
	public static SummonMode getSummonModeById(int id) {
		for (SummonMode mode : values()) {
			if (mode.getId() == id) {
				return mode;
			}
		}
		return null;
	}
}
