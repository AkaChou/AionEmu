package com.aionemu.gameserver.model.templates.rewards;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.itemgroups.ItemRaceEntry;
import lombok.Getter;

/**
 * 制作奖励模板（静态数据/XML）。
 * XML template.
 *
 * @author Rolandas
 */
@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CraftReward")
@XmlSeeAlso({ CraftRecipe.class, CraftItem.class })
public abstract class CraftReward extends ItemRaceEntry {

	/**
	 * 获取 skill 属性值。
	 * Gets the value of the skill property
	 * @return 可能的对象类型 / possible object is {@link Integer }
	 */
	@XmlAttribute(name = "skill")
	protected Integer skill;
}
