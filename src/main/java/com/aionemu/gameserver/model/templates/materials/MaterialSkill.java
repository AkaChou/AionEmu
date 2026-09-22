package com.aionemu.gameserver.model.templates.materials;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * 材料技能模板（静态数据/XML）。
 * XML template.
 * @author Rolandas
 */
@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MaterialSkill")
public class MaterialSkill {

	/** 返回时间 / Returns the time */
	@XmlAttribute
	protected MaterialActTime time;

	/** 返回频率 / Returns the frequency */
	@XmlAttribute(required = true)
	protected float frequency;

	@XmlAttribute
	protected MaterialTarget target;

	@XmlAttribute(required = true)
	protected int level;

	/** 返回 ID / Returns the id */
	@XmlAttribute(required = true)
	protected int id;

	/** 返回目标 / Returns the target */
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
}
