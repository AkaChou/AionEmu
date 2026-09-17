package com.aionemu.gameserver.model.templates.rewards;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * 制作物品奖励模板（静态数据/XML）。
 * XML template.
 *
 * @author Rolandas
 */
@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CraftItem")
public class CraftItem extends CraftReward {

	/**
	 * 获取 minLevel 属性值。
	 * Gets the value of the minLevel property
	 */
	@XmlAttribute(name = "minLevel", required = true)
	protected int minLevel;

	/**
	 * 获取 maxLevel 属性值。
	 * Gets the value of the maxLevel property
	 */
	@XmlAttribute(name = "maxLevel", required = true)
	protected int maxLevel;
}
