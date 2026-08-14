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

	/** 技能列表。 / Skill list. */
	@XmlElement(name = "skill", required = true)
	protected List<MaterialSkill> skills;

	/** 技能障碍。 / Skill obstacle. */
	@XmlAttribute(name = "skill_obstacle")
	protected Integer skillObstacle;

	/** 模板 ID。 / Template id. */
	@XmlAttribute(required = true)
	protected int id;

	/** 返回技能 / Returns the skills */
	public List<MaterialSkill> getSkills() {
		return skills;
	}

	/** 返回技能障碍 / Returns the skill obstacle */
	public Integer getSkillObstacle() {
		return skillObstacle;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}
}
