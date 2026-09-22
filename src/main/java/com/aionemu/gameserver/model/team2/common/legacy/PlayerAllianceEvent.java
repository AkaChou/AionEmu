package com.aionemu.gameserver.model.team2.common.legacy;

import lombok.Getter;

/**
 * 玩家联盟活动枚举。
 * Player Alliance Event enumeration.
 * @author Sarynth
 */
@Getter
public enum PlayerAllianceEvent {
	/** 离开 / Leave. */
	LEAVE(0), LEAVE_TIMEOUT(0), BANNED(0), MOVEMENT(1), DISCONNECTED(3), JOIN(5), ENTER_OFFLINE(7), UNK(9),
	/** 重连 / Reconnect. */
	RECONNECT(13), ENTER(13), UPDATE(13), MEMBER_GROUP_CHANGE(5), APPOINT_VICE_CAPTAIN(13), DEMOTE_VICE_CAPTAIN(13),
	/** Appoint Captain / Appoint Captain */
	APPOINT_CAPTAIN(13);

	/** 返回 ID / Returns the id */
	private final int id;

	PlayerAllianceEvent(int id) {
		this.id = id;
	}
}
