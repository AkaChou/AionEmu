package com.aionemu.gameserver.questEngine.runtime;

/** 中央任务路由的结论合同。 Result contract for central quest routing. */
public enum QuestRouteResult {
	HANDLED,
	NOT_HANDLED,
	UNKNOWN,
	FAILED
}
