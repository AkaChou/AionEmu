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
	/** 新手 / Novice. */
	NOVICE, DISCIPLINED, SEASONED, EXPERT, VETERAN, MASTER;
}
