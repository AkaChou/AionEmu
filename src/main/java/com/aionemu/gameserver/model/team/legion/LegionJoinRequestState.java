package com.aionemu.gameserver.model.team.legion;

/**
 * 军团加入请求状态枚举。
 * Legion Join Request State enumeration.
 */

public enum LegionJoinRequestState {
	/** 已接受 / Accepted. */
	ACCEPTED,
	/** 已拒绝 / Denied. */
	DENIED,
	/** 无状态 / None. */
	NONE,
	/** 已取消 / Cancel. */
	CANCEL;
}
