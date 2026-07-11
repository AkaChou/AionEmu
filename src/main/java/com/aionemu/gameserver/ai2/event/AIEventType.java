package com.aionemu.gameserver.ai2.event;

/**
 * AI2 系统中的事件类型枚举，覆盖激活、战斗、移动、感知、生命周期与对话等场景。
 * Enumeration of AI2 event types covering activation, combat, movement, perception, lifecycle, and dialogue.
 *
 * @author ATracer
 */
public enum AIEventType {

	/** 激活 AI / Activate AI */
	ACTIVATE,
	/** 停用 AI / Deactivate AI */
	DEACTIVATE,
	/** 冻结 AI（暂停处理） / Freeze AI (suspend processing) */
	FREEZE,
	/** 解冻 AI / Unfreeze AI */
	UNFREEZE,
	/** 生物正在被攻击（内部） / Creature is being attacked (internal) */
	ATTACK,
	/** 生物攻击动作完成（内部） / Creature's attack part is complete (internal) */
	ATTACK_COMPLETE,
	/** 生物停止攻击（内部） / Creature is stopping attack (internal) */
	ATTACK_FINISH,
	/** 邻近生物需要支援（广播） / Neighbor creature needs support (broadcast) */
	CREATURE_NEEDS_SUPPORT,
	/** 校验移动路径 / Validate move path */
	MOVE_VALIDATE,
	/** 已到达移动目标点 / Arrived at move destination */
	MOVE_ARRIVED,
	/** 看见生物 / Creature became visible */
	CREATURE_SEE,
	/** 看不见生物 / Creature became invisible */
	CREATURE_NOT_SEE,
	/** 生物发生移动 / Creature moved */
	CREATURE_MOVED,
	/** 生物触发仇恨 / Creature aggro triggered */
	CREATURE_AGGRO,
	/** 已刷出 / Spawned */
	SPAWNED,
	/** 已重生 / Respawned */
	RESPAWNED,
	/** 已消失 / Despawned */
	DESPAWNED,
	/** 已死亡 / Died */
	DIED,
	/** 已到达目标 / Target reached */
	TARGET_REACHED,
	/** 目标过远 / Target too far */
	TARGET_TOOFAR,
	/** 放弃目标 / Give up target */
	TARGET_GIVEUP,
	/** 目标已变更 / Target changed */
	TARGET_CHANGED,
	/** 请求跟随 / Follow me request */
	FOLLOW_ME,
	/** 停止跟随 / Stop follow me */
	STOP_FOLLOW_ME,
	/** 不在出生点 / Not at home */
	NOT_AT_HOME,
	/** 已返回出生点 / Back home */
	BACK_HOME,
	/** 对话开始 / Dialog started */
	DIALOG_START,
	/** 对话结束 / Dialog finished */
	DIALOG_FINISH,
	/** 掉落已注册 / Drop registered */
	DROP_REGISTERED
}
