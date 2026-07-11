package com.aionemu.gameserver.model.templates.materials;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 材料技能模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author Rolandas
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MaterialSkill")
public class MaterialSkill {

	@XmlAttribute
	protected MaterialActTime time;

	@XmlAttribute(required = true)
	protected float frequency;

	@XmlAttribute
	protected MaterialTarget target;

	@XmlAttribute(required = true)
	protected int level;

	@XmlAttribute(required = true)
	protected int id;

	/** 返回时间 / Returns the time*/
	public MaterialActTime getTime() {
		return time;
	}

	/** 返回 frequency / Returns the frequency */
	public float getFrequency() {
		return frequency;
	}

	/** 返回目标 / Returns the target*/
	public MaterialTarget getTarget() {
		if (target == null) {
			return MaterialTarget.ALL;
		}
		return target;
	}

	/** 获取技能等级。 / Returns the skill level. */
	public int getSkillLevel() {
		return level;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}
}
