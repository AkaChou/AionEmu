package com.aionemu.gameserver.model.templates.npcskill;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * NPC 技能 Templates 模板（静态数据/XML）。
 * XML template.
 *
 * @author AionChs Master
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "npcskills")
public class NpcSkillTemplates {

	@XmlAttribute(name = "npcid")
	protected int npcId;
	@XmlElement(name = "npcskill")
	protected List<NpcSkillTemplate> npcSkills;

	public NpcSkillTemplates() {
	}

	public NpcSkillTemplates(int npcId, List<NpcSkillTemplate> npcSkills) {
		this.npcId = npcId;
		this.npcSkills = npcSkills;
	}

	/** 返回 NPC ID / Returns the npc id */
	public int getNpcId() {
		return npcId;
	}

	/** 返回 npc skills / Returns the npc skills */
	public List<NpcSkillTemplate> getNpcSkills() {
		return npcSkills;
	}
}
