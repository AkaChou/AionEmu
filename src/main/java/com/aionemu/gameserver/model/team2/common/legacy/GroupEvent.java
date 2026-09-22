package com.aionemu.gameserver.model.team2.common.legacy;

import lombok.Getter;

/**
 * 队伍活动枚举。
 * Group Event enumeration.
 * @author Lyahim
 */
@Getter
public enum GroupEvent {

	/** 离开 / Leave. */
	LEAVE(0), MOVEMENT(1), DISCONNECTED(3), JOIN(5), ENTER_OFFLINE(7), ENTER(13), UPDATE(13), UNK(9), // 待办 / To do
	/** Unk 53 / Unk 53 */
	UNK_53(65);

	/** 返回 ID / Returns the id */
	private final int id;

	GroupEvent(int id) {
		this.id = id;
	}
}
