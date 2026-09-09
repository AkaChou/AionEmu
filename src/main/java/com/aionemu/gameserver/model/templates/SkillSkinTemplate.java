package com.aionemu.gameserver.model.templates;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * 技能外观模板（静态数据/XML）。
 * Skill skin template (static data / XML).
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "skill_skin")
public class SkillSkinTemplate {

	/** 返回 ID / Returns the id */
	@Getter
	@XmlAttribute(name = "id", required = true)
	private int id;
	/** 获取名称。 / Returns the name. */
	@Getter
	@XmlAttribute(name = "name", required = true)
	private String name;
	@XmlAttribute(name = "skill_group", required = true)
	private String skillgroup;
	/** 返回动作名称 / Returns the motion name */
	@Getter
	@XmlAttribute(name = "motion_name", required = true)
	private String motionName;
	/** 返回 ammo speed / Returns the ammo speed */
	@Getter
	@XmlAttribute(name = "ammo_speed", required = true)
	private int ammoSpeed;

	/** 获取技能队伍。 / Returns the skill group. */
	public String getSkillGroup() {
		return skillgroup;
	}
}
