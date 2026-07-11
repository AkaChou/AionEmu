package com.aionemu.gameserver.utils.stats;

/**
 * 属性/伤害计算类型标记，用于区分技能、能量碎片、双手武器等计算路径
 * Calculation type flags that distinguish skill, power-shard, dual-wield and related stat calc paths
 */
public enum CalculationType {
	/** 技能伤害计算 / Skill damage calculation */
	SKILL,
	/** 应用能量碎片伤害加成 / Apply power-shard damage bonus */
	APPLY_POWER_SHARD_DAMAGE,
	/** 消耗能量碎片 / Remove/consume power shard */
	REMOVE_POWER_SHARD,
	/** 仅用于展示的计算 / Display-only calculation */
	DISPLAY,
	/** 双持计算 / Dual-wield calculation */
	DUAL_WIELD,
	/** 主手计算 / Main-hand calculation */
	MAIN_HAND,
	/** 副手计算 / Off-hand calculation */
	OFF_HAND
}
