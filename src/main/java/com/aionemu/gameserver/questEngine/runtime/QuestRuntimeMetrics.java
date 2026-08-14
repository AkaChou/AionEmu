package com.aionemu.gameserver.questEngine.runtime;

/** 路由分发与每 owner 结果的类型化指标边界。 / Typed metrics boundary for route dispatch and per-owner outcomes. */
public interface QuestRuntimeMetrics {
	void onDispatch(QuestDispatchContract contract, int ownerCount);

	void onOwnerResult(QuestDispatchContract contract, int questId, QuestRouteResult result);

	/** 记录审计 sink 失败，而不递归发出另一个审计事件。 / Records an audit sink failure without recursively emitting another audit event. */
	default void onAuditFailure(int questId, String failureType) {
		// 对不暴露基础设施健康指标的适配器为可选。 / Optional for adapters that do not expose infrastructure health metrics.
	}
}
