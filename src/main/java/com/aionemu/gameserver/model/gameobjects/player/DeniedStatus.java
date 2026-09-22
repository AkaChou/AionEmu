package com.aionemu.gameserver.model.gameobjects.player;

import lombok.Getter;

/**
 * Denied 状态枚举。
 * Denied Status enumeration.
 * @author Sweetkr
 */
@Getter
public enum DeniedStatus {
	/** 查看详情 / View Details */
	VIEW_DETAILS(1), TRADE(2), GROUP(4), GUILD(8), FRIEND(16), DUEL(32);

	private final int id;

	DeniedStatus(int id) {
		this.id = id;
	}
}
