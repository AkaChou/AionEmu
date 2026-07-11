package com.aionemu.gameserver.ai2;

/**
 * AI 主状态枚举，描述 AI 生命周期与行为阶段。
 * Main AI state enumeration describing lifecycle and behavioral phases.
 *
 * @author ATracer
 */
public enum AIState {

	/** 已创建，尚未完成初始化 / Created, not fully initialized yet */
	CREATED,
	/** 已死亡 / Died */
	DIED,
	/** 已消失/卸载 / Despawned */
	DESPAWNED,
	/** 空闲 / Idle */
	IDLE,
	/** 巡逻/行走中 / Walking/patrolling */
	WALKING,
	/** 跟随目标中 / Following a target */
	FOLLOWING,
	/** 返回出生点中 / Returning home */
	RETURNING,
	/** 战斗中 / Fighting */
	FIGHT,
	/** 恐惧逃跑中 / Fleeing in fear */
	FEAR
}
