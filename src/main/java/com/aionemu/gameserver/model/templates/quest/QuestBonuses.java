package com.aionemu.gameserver.model.templates.quest;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.rewards.BonusType;

/**
 * 任务加成模板（静态数据/XML）。
 * XML template. / XML template.
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
	  * 获取 type 属性值。
	  * Gets the value of the type property
	  * @return possible object is {@link BonusType }
	  */
	public BonusType getType() {
		return type;
	}

	 /**
	  * 获取 level 属性值。
	  * Gets the value of the level property
	  * @return possible object is {@link Integer }
	  */
	public Integer getLevel() {
		return level;
	}

	 /**
	  * 获取 skill 属性值。
	  * Gets the value of the skill property
	  * @return possible object is {@link Integer }
	  */
	public Integer getSkill() {
		return skill;
	}
}
