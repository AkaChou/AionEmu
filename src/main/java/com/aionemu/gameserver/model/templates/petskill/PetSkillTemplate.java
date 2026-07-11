package com.aionemu.gameserver.model.templates.petskill;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 宠物技能模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "pet_skill")
public class PetSkillTemplate {

	@XmlAttribute(name = "skill_id")
	protected int skillId;
	@XmlAttribute(name = "pet_id")
	protected int petId;
	@XmlAttribute(name = "order_skill")
	protected int orderSkill;

	/**
	 * @return the skillId
	 */
	public int getSkillId() {
		return skillId;
	}

	/**
	 * @return the petId
	 */
	public int getPetId() {
		return petId;
	}

	/**
	 * @return the orderSkill
	 */
	public int getOrderSkill() {
		return orderSkill;
	}
}
