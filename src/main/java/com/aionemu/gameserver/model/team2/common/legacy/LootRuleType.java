package com.aionemu.gameserver.model.team2.common.legacy;

import lombok.Getter;

/**
 * LootRule 类型枚举。
 * Loot Rule Type enumeration.
 * @author Lyahim
 */
@Getter
public enum LootRuleType {
	/** 自由拾取 / Freeforall. */
	FREEFORALL(0), ROUNDROBIN(1), LEADER(2);

	/** 返回 ID / Returns the id */
	private final int id;

	LootRuleType(int id) {
		this.id = id;
	}
}
