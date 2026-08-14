package com.aionemu.gameserver.questEngine.runtime;

/** 中央任务路由的结论合同。 Result contract for central quest routing. */
public enum QuestRouteResult {
	/** 已处理 / Handled. */
	HANDLED,
	/** 任务已占用物品使用并阻止普通物品动作。 / The quest claimed the item use and intentionally blocked the normal item action. */
	BLOCKED,
	/** 未处理 / Not handled. */
	NOT_HANDLED,
	/** 未知 / Unknown. */
	UNKNOWN,
	/** 失败 / Failed. */
	FAILED
}
