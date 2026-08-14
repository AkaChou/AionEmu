package com.aionemu.gameserver.model.templates.quest;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.rewards.BonusType;

/**
 * 任务加成模板（静态数据/XML）。
 * Quest bonuses template (static data / XML).
 *
 * @author Rolandas
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "QuestBonuses")
public class QuestBonuses {

	@XmlAttribute(required = true)
	protected BonusType type;
	@XmlAttribute
	protected Integer level;
	@XmlAttribute
	protected Integer skill;

	 /**
	  * 返回加成类型。
	  * Returns the bonus type.
	  *
	  * @return 加成类型 / possible object is {@link BonusType}
	  */
	public BonusType getType() {
		return type;
	}

	 /**
	  * 返回等级。
	  * Returns the level.
	  *
	  * @return 等级 / possible object is {@link Integer}
	  */
	public Integer getLevel() {
		return level;
	}

	 /**
	  * 返回技能 ID。
	  * Returns the skill id.
	  *
	  * @return 技能 ID / possible object is {@link Integer}
	  */
	public Integer getSkill() {
		return skill;
	}
}
