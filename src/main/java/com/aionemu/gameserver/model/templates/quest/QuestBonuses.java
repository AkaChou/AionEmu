package com.aionemu.gameserver.model.templates.quest;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.rewards.BonusType;
import lombok.Getter;

/**
 * 任务加成模板（静态数据/XML）。
 * Quest bonuses template (static data / XML).
 *
 * @author Rolandas
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "QuestBonuses")
public class QuestBonuses {

	/**
	 * 返回加成类型。
	 * Returns the bonus type.
	 *
	 * @return 加成类型 / possible object is {@link BonusType}
	 */
	@Getter
	@XmlAttribute(required = true)
	protected BonusType type;
	/**
	 * 返回等级。
	 * Returns the level.
	 *
	 * @return 等级 / possible object is {@link Integer}
	 */
	@Getter
	@XmlAttribute
	protected Integer level;
	/**
	 * 返回技能 ID。
	 * Returns the skill id.
	 *
	 * @return 技能 ID / possible object is {@link Integer}
	 */
	@Getter
	@XmlAttribute
	protected Integer skill;
}
