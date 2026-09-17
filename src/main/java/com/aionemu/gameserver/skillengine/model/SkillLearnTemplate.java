package com.aionemu.gameserver.skillengine.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import lombok.Getter;

/**
 * 技能学习模板：职业、种族、等级与是否自动/烙印学习。
 * Skill learn template: class, race, level and auto/stigma learn flags.
 */
@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "skill")
public class SkillLearnTemplate {

	/**
	 * 获取职业。
	 * Gets player class.
	 *
	 */
	@XmlAttribute(name = "classId", required = true)
	private PlayerClass classId = PlayerClass.ALL;

	/**
	 * 获取技能 ID。
	 * Gets skill id.
	 *
	 */
	@XmlAttribute(name = "skillId", required = true)
	private int skillId;

	/**
	 * 获取技能等级。
	 * Gets skill level.
	 *
	 */
	@XmlAttribute(name = "skillLevel", required = true)
	private int skillLevel;

	/**
	 * 获取技能名称。
	 * Gets skill name.
	 *
	 */
	@XmlAttribute(name = "name", required = true)
	private String name;

	/**
	 * 获取种族限制。
	 * Gets race restriction.
	 *
	 * @return 阵营 / race
	 */
	@XmlAttribute(name = "race", required = true)
	private Race race;

	/**
	 * 获取最低学习等级。
	 * Gets minimum learn level.
	 *
	 */
	@XmlAttribute(name = "minLevel", required = true)
	private int minLevel;

	@XmlAttribute(name = "skill_group")
	private String skill_group;

	/**
	 * 是否自动学习。
	 * Whether auto-learned.
	 *
	 */
	@XmlAttribute
	private boolean autoLearn;

	/**
	 * 是否烙印技能。
	 * Whether this is a stigma skill.
	 *
	 */
	@XmlAttribute
	private boolean stigma = false;

	/**
	 * 获取技能分组。
	 * Gets skill group.
	 *
	 */
	public String getSkillGroup() {
		return skill_group;
	}
}
