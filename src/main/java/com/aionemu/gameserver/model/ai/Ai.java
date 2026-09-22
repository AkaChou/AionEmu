package com.aionemu.gameserver.model.ai;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * AI 配置模板：按 NPC 定义召唤物与炸弹行为。
 * AI configuration template: defines summon and bomb behavior per NPC.
 * @author xTz
 */
@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Ai")
public class Ai {

	/** 返回 summons / Returns the summons */
	@XmlElement(name = "summons")
	private Summons summons;

	/** 返回 bombs / Returns the bombs */
	@XmlElement(name = "bombs")
	private Bombs bombs;

	/** 返回 NPC ID / Returns the npc id */
	@XmlAttribute(name = "npcId")
	private int npcId;
}
