package com.aionemu.gameserver.questEngine.runtime;

/** 保留现有调用方契约作为显式路由策略。 / Existing caller contracts preserved as explicit routing policies. */
public enum QuestDispatchContract {
	/** 独占 / Exclusive. */
	EXCLUSIVE,
	/** 第一个非未知 / First non-unknown. */
	FIRST_NON_UNKNOWN,
	/** 第一个已注册 / First registered. */
	FIRST_REGISTERED,
	/** 广播 / Broadcast. */
	BROADCAST
}
