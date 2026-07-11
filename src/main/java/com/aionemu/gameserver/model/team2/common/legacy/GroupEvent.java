package com.aionemu.gameserver.model.team2.common.legacy;

/**
 * 队伍活动枚举。
 * Group Event enumeration.
 *
 * @author Lyahim
 */
public enum GroupEvent {

	/** 离开 / Leave. */
	LEAVE(0), MOVEMENT(1), DISCONNECTED(3), JOIN(5), ENTER_OFFLINE(7), ENTER(13), UPDATE(13), UNK(9), // to do
	/** Unk 53 / Unk 53 */
	UNK_53(65);

	private int id;

	private GroupEvent(int id) {
		this.id = id;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return this.id;
	}
}
