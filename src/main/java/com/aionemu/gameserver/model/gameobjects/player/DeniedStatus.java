package com.aionemu.gameserver.model.gameobjects.player;

/**
 * Denied 状态枚举。
 * Denied Status enumeration.
 *
 * @author Sweetkr
 */
public enum DeniedStatus {
	/** 查看详情 / View Details */
	VIEW_DETAILS(1), TRADE(2), GROUP(4), GUILD(8), FRIEND(16), DUEL(32);

	private int id;

	private DeniedStatus(int id) {
		this.id = id;
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}
}
