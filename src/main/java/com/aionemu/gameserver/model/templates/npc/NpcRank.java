package com.aionemu.gameserver.model.templates.npc;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * NPC 军阶枚举。
 * Npc Rank enumeration.
 */

@XmlType(name = "rank")
@XmlEnum
public enum NpcRank {
	/** 新手 / Novice */
	NOVICE,
	/** 训练有素 / Disciplined */
	DISCIPLINED,
	/** 老练 / Seasoned */
	SEASONED,
	/** 专家 / Expert */
	EXPERT,
	/** 老兵 / Veteran */
	VETERAN,
	/** 大师 / Master */
	MASTER;
}
