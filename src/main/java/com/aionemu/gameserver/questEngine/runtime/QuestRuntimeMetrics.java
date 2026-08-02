package com.aionemu.gameserver.questEngine.runtime;

/** Typed metrics boundary for route dispatch and per-owner outcomes. */
public interface QuestRuntimeMetrics {
	void onDispatch(QuestDispatchContract contract, int ownerCount);

	void onOwnerResult(QuestDispatchContract contract, int questId, QuestRouteResult result);

	/** Records an audit sink failure without recursively emitting another audit event. */
	default void onAuditFailure(int questId, String failureType) {
		// Optional for adapters that do not expose infrastructure health metrics.
	}
}
