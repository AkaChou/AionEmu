package com.aionemu.gameserver.model.templates.npc;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.state.CreatureSeeState;

/**
 * NpcRating 枚举。
 * Npc Rating enumeration.
 */

@XmlType(name = "rating")
@XmlEnum
public enum NpcRating {
	/** 垃圾 / Junk. */
	JUNK(CreatureSeeState.NORMAL), NORMAL(CreatureSeeState.NORMAL), ELITE(CreatureSeeState.SEARCH1),
	/** 英雄 / Hero. */
	HERO(CreatureSeeState.SEARCH2), LEGENDARY(CreatureSeeState.SEARCH2);

	private final CreatureSeeState congenitalSeeState;

	private NpcRating(CreatureSeeState congenitalSeeState) {
		this.congenitalSeeState = congenitalSeeState;
	}

	/** 返回 congenital see state / Returns the congenital see state */
	public CreatureSeeState getCongenitalSeeState() {
		return congenitalSeeState;
	}
}
