package com.aionemu.gameserver.model.templates.materials;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 材料模板（静态数据/XML）。
 * XML template.
 *
 * @author Rolandas
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MaterialTemplate", propOrder = { "skills" })
public class MaterialTemplate {

	@XmlElement(name = "skill", required = true)
	protected List<MaterialSkill> skills;

	@XmlAttribute(name = "skill_obstacle")
	protected Integer skillObstacle;

	@XmlAttribute(required = true)
	protected int id;

	/** 返回技能 / Returns the skills */
	public List<MaterialSkill> getSkills() {
		return skills;
	}

	/** 返回 skill obstacle / Returns the skill obstacle */
	public Integer getSkillObstacle() {
		return skillObstacle;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}
}
