package com.aionemu.gameserver.skillengine.model;

/**
 * 驱散分类：按增益/减益等类别筛选可驱散效果。
 * Dispel category: filters dispellable effects by buff/debuff class.
 *
 * @author kecimis
 */
public enum DispelCategoryType {

	/** 无 / None */
	NONE,
	/** 全部 / All */
	ALL,
	/** 增益 / Buff */
	BUFF,
	/** 减益 / Debuff */
	DEBUFF,
	/** 精神减益 / Mental debuff */
	DEBUFF_MENTAL,
	/** 物理减益 / Physical debuff */
	DEBUFF_PHYSICAL,
	/** 额外类别 / Extra */
	EXTRA,
	/** 永不驱散 / Never dispel */
	NEVER,
	/** NPC 增益 / NPC buff */
	NPC_BUFF,
	/** NPC 物理减益 / NPC physical debuff */
	NPC_DEBUFF_PHYSICAL,
	/** 眩晕类 / Stun category */
	STUN;
}
