package com.aionemu.gameserver.model.templates;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 技能外观模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "skill_skin")
public class SkillSkinTemplate {

	@XmlAttribute(name = "id", required = true)
	private int id;
	@XmlAttribute(name = "name", required = true)
	private String name;
	@XmlAttribute(name = "skill_group", required = true)
	private String skillgroup;
	@XmlAttribute(name = "motion_name", required = true)
	private String motionName;
	@XmlAttribute(name = "ammo_speed", required = true)
	private int ammoSpeed;

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 获取名称。 / Returns the name. */
	public String getName() {
		return name;
	}

	/** 获取技能队伍。 / Returns the skill group. */
	public String getSkillGroup() {
		return skillgroup;
	}

	/** 返回动作名称 / Returns the motion name*/
	public String getMotionName() {
		return motionName;
	}

	/** 返回 ammo speed / Returns the ammo speed */
	public int getAmmoSpeed() {
		return ammoSpeed;
	}
}
