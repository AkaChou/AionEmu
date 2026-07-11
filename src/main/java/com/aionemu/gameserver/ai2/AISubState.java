package com.aionemu.gameserver.ai2;

/**
 * AI 子状态枚举，用于在主状态之下表达更细粒度的行为。
 * AI sub-state enumeration for finer-grained behavior under a main state.
 *
 * @author ATracer
 */
public enum AISubState {

	/** 无子状态 / No sub-state */
	NONE,
	/** 对话中 / Talking/dialog */
	TALK,
	/** 施法中 / Casting a skill */
	CAST,
	/** 按路径行走 / Walking along a path */
	WALK_PATH,
	/** 随机行走 / Walking randomly */
	WALK_RANDOM,
	/** 等待队伍/组行走 / Waiting for group walk */
	WALK_WAIT_GROUP,
	/** 冻结（行为暂停） / Frozen (behavior paused) */
	FREEZE,
	/** 目标丢失 / Target lost */
	TARGET_LOST
}
