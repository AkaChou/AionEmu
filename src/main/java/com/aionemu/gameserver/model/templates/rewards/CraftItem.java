package com.aionemu.gameserver.model.templates.rewards;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 制作物品奖励模板（静态数据/XML）。
 * XML template.
 *
 * @author Rolandas
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CraftItem")
public class CraftItem extends CraftReward {

	@XmlAttribute(name = "minLevel", required = true)
	protected int minLevel;

	@XmlAttribute(name = "maxLevel", required = true)
	protected int maxLevel;

	 /**
	  * 获取 minLevel 属性值。
	  * Gets the value of the minLevel property
	  */
	public int getMinLevel() {
		return minLevel;
	}

	 /**
	  * 获取 maxLevel 属性值。
	  * Gets the value of the maxLevel property
	  */
	public int getMaxLevel() {
		return maxLevel;
	}
}
