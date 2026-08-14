package com.aionemu.gameserver.model.templates.globaldrops;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 全局掉落 NPC 模板（静态数据/XML）。
 * Global drop NPC template (static data/XML).
 *
 * @author Wnkrz
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GlobalDropNpc")
public class GlobalDropNpc {
	@XmlAttribute(name = "npc_id", required = true)
	protected int npcId;

	/** 返回 NPC ID。 / Returns the npc id. */
	public int getNpcId() {
		return npcId;
	}
}
