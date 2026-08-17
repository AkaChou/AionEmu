package com.aionemu.gameserver.questEngine.e2e;

/**
 * 端到端场景相对于目标 transition 的实际路由归因；它独立于执行状态，避免把场景合成偏差误报为状态错误。
 * Actual route attribution relative to the transition targeted by an end-to-end scenario; it is independent from
 * execution status so scenario-synthesis drift is not misreported as a state failure.
 */
public enum QuestE2eTransitionMatch {
	EXPECTED_TRANSITION_MATCHED,
	ALTERNATE_TRANSITION_MATCHED,
	NO_TRANSITION_MATCHED,
	UNSUPPORTED_SCENARIO_FACTS
}
