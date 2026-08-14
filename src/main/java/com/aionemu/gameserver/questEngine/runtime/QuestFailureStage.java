package com.aionemu.gameserver.questEngine.runtime;

/** 生产审计与恢复决策使用的稳定执行阶段。 / Stable execution phase used by production audit and recovery decisions. */
public enum QuestFailureStage {
	/** 路由 / Routing. */
	ROUTING,
	/** 快照 / Snapshot. */
	SNAPSHOT,
	/** 计划 / Plan. */
	PLAN,
	/** 预检 / Preflight. */
	PREFLIGHT,
	/** 应用动作 / Apply actions. */
	APPLY_ACTIONS,
	/** 应用状态 / Apply state. */
	APPLY_STATE,
	/** 提交 / Commit. */
	COMMIT,
	/** 参与者提交后 / Participant after-commit. */
	PARTICIPANT_AFTER_COMMIT,
	/** 状态发布 / State publish. */
	STATE_PUBLISH,
	/** 状态重同步 / State resync. */
	STATE_RESYNC,
	/** 提交后 / After commit. */
	AFTER_COMMIT
}
