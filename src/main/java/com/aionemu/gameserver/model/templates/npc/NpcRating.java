package com.aionemu.gameserver.model.templates.npc;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.state.CreatureSeeState;
import lombok.Getter;

/**
 * NpcRating 枚举。
 * Npc Rating enumeration.
 */

@Getter
@XmlType(name = "rating")
@XmlEnum
public enum NpcRating {
	/** 垃圾 / Junk */
	JUNK(CreatureSeeState.NORMAL),
	/** 普通 / Normal */
	NORMAL(CreatureSeeState.NORMAL),
	/** 精英 / Elite */
	ELITE(CreatureSeeState.SEARCH1),
	/** 英雄 / Hero */
	HERO(CreatureSeeState.SEARCH2),
	/** 传颂 / Legendary */
	LEGENDARY(CreatureSeeState.SEARCH2);

	/** 返回 congenital see state / Returns the congenital see state */
	private final CreatureSeeState congenitalSeeState;

	NpcRating(CreatureSeeState congenitalSeeState) {
		this.congenitalSeeState = congenitalSeeState;
	}
}
