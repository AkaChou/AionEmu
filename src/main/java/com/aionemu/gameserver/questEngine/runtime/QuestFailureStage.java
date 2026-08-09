package com.aionemu.gameserver.questEngine.runtime;

/** Stable execution phase used by production audit and recovery decisions. */
public enum QuestFailureStage {
	ROUTING,
	SNAPSHOT,
	PLAN,
	PREFLIGHT,
	APPLY_ACTIONS,
	APPLY_STATE,
	COMMIT,
	PARTICIPANT_AFTER_COMMIT,
	STATE_PUBLISH,
	STATE_RESYNC,
	AFTER_COMMIT
}
