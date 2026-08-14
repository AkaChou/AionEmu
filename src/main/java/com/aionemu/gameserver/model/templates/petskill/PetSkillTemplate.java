package com.aionemu.gameserver.model.templates.petskill;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 宠物技能模板：将技能绑定到指定宠物并定义施放顺序。
 * Pet skill template: binds a skill to a pet and defines cast order.
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
	 * @return 技能 ID / the skill id
	 */
	public int getSkillId() {
		return skillId;
	}

	/**
	 * @return 宠物 ID / the pet id
	 */
	public int getPetId() {
		return petId;
	}

	/**
	 * @return 施放顺序 / the cast order
	 */
	public int getOrderSkill() {
		return orderSkill;
	}
}
