package com.aionemu.gameserver.model.team2.common.legacy;

import lombok.Getter;

/**
 * LootDistribution 枚举。
 * Loot Distribution enumeration.
 *
 * @author KKnD
 */
public enum LootDistribution {

	/** 普通 / Normal. */
	NORMAL(0), ROLL_DICE(2), BID(3);

	/** 返回 ID / Returns the id */
	@Getter
	private final int id;

	LootDistribution(int id) {
		this.id = id;
	}
}
