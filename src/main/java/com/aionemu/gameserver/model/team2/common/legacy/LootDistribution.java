package com.aionemu.gameserver.model.team2.common.legacy;

/**
 * LootDistribution 枚举。
 * Loot Distribution enumeration.
 *
 * @author KKnD
 */
public enum LootDistribution {

	/** 普通 / Normal. */
	NORMAL(0), ROLL_DICE(2), BID(3);

	private int id;

	LootDistribution(int id) {
		this.id = id;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return this.id;
	}
}
