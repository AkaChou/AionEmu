package com.aionemu.gameserver.model.templates.minion;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 守护灵技能模板（静态数据/XML）。
 * XML template.
 *
 * @author Falke_34
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MinionSkill")
public class MinionSkill {

	@XmlAttribute(name = "skill_id")
	public int skill_id;

	@XmlAttribute(name = "energyCost")
	public int energyCost;

	/** 返回技能 ID / Returns the skill id */
	public int getSkillId() {
		return this.skill_id;
	}

	/** 返回 energy cost / Returns the energy cost */
	public int getEnergyCost() {
		return this.energyCost;
	}
}
