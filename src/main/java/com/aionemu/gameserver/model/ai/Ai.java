package com.aionemu.gameserver.model.ai;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * AI，用于 ai 相关逻辑。
 * Ai for ai logic.
 *
 * @author xTz
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Ai")
public class Ai {

	@XmlElement(name = "summons")
	private Summons summons;

	@XmlElement(name = "bombs")
	private Bombs bombs;

	@XmlAttribute(name = "npcId")
	private int npcId;

	/** 返回 summons / Returns the summons */
	public Summons getSummons() {
		return this.summons;
	}

	/** 返回 bombs / Returns the bombs */
	public Bombs getBombs() {
		return this.bombs;
	}

	/** 返回 NPC ID / Returns the npc id */
	public int getNpcId() {
		return this.npcId;
	}
}
