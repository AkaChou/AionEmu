package com.aionemu.gameserver.questEngine.runtime;

/**
 * 单次串行化玩家事件执行的状态。
 * Status of one serialized player event execution.
 */
public enum QuestExecutionStatus {
	/** 事件与任何任务路由均不匹配，未发生变更。 / no quest route matched the event, nothing changed */
	NO_MATCH,
	/** 匹配的路由已执行并提交变更。 / a matching route executed and committed changes */
	COMMITTED
}
