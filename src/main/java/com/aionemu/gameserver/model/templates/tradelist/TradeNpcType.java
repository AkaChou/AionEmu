package com.aionemu.gameserver.model.templates.tradelist;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 交易 NPC 类型枚举。
 * Trade Npc Type enumeration.
 *
 * @author namedrisk
 */
@XmlType(name = "npc_type")
@XmlEnum
public enum TradeNpcType {
	/** 普通 / Normal. */
	NORMAL(1), ABYSS(2), REWARD(4);

	private final int index;

	private TradeNpcType(int index) {
		this.index = index;
	}

	/** 索引 / index. */
	public int index() {
		return index;
	}
}
