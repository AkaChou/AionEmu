package com.aionemu.gameserver.model.templates.npcskill;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * NPC 技能 Templates 模板（静态数据/XML）。
 * XML template.
 *
 * @author AionChs Master
 */
@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "npcskills")
@NoArgsConstructor
@AllArgsConstructor
public class NpcSkillTemplates {

	/** 返回 NPC ID / Returns the npc id */
	@XmlAttribute(name = "npcid")
	protected int npcId;
	/** 返回 npc skills / Returns the npc skills */
	@XmlElement(name = "npcskill")
	protected List<NpcSkillTemplate> npcSkills;
}
