package com.aionemu.gameserver.model.team2.common.legacy;

/**
 * LootRule 类型枚举。
 * Loot Rule Type enumeration.
 *
 * @author Lyahim
 */
public enum LootRuleType {
	/** 自由拾取 / Freeforall. */
	FREEFORALL(0), ROUNDROBIN(1), LEADER(2);

	private int id;

	private LootRuleType(int id) {
		this.id = id;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return this.id;
	}
}
