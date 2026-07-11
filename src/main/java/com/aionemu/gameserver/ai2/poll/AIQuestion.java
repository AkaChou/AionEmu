package com.aionemu.gameserver.ai2.poll;

/**
 * AI 决策时可向 AI 实现发起的问题类型枚举。
 * Enumeration of question types that can be polled against an AI implementation.
 *
 * @author ATracer
 */
public enum AIQuestion {

	/** 是否已到达目的地 / Whether the destination has been reached */
	DESTINATION_REACHED,
	/** 昼夜切换时是否允许刷出 / Whether spawning is allowed on daytime change */
	CAN_SPAWN_ON_DAYTIME_CHANGE,
	/** 是否应当腐朽消失 / Whether the corpse should decay */
	SHOULD_DECAY,
	/** 是否应当重生 / Whether the creature should respawn */
	SHOULD_RESPAWN,
	/** 是否应当发放击杀奖励 / Whether kill rewards should be granted */
	SHOULD_REWARD,
	/** 是否应当发放 AP 奖励 / Whether AP rewards should be granted */
	SHOULD_REWARD_AP,
	/** 是否应当发放 GP 奖励 / Whether GP rewards should be granted */
	SHOULD_REWARD_GP,
	/** 是否可抵抗异常状态 / Whether abnormal states can be resisted */
	CAN_RESIST_ABNORMAL,
	/** 是否可攻击玩家 / Whether the player may be attacked */
	CAN_ATTACK_PLAYER,
	/** 是否可喊话 / Whether shouting is allowed */
	CAN_SHOUT,
	/** 被攻击时可见性判定是否考虑碰撞边界 / Consider bounds in can-see check when attacked */
	CONSIDER_BOUNDS_IN_CAN_SEE_CHECK_WHEN_ATTACKED,
	/** 攻击时可见性判定是否考虑碰撞边界 / Consider bounds in can-see check when attacking */
	CONSIDER_BOUNDS_IN_CAN_SEE_CHECK_WHEN_ATTACKING;
}
