package com.aionemu.gameserver.model.templates.rewards;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 制作配方奖励模板（静态数据/XML）。
 * XML template.
 *
 * @author Rolandas
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CraftRecipe")
public class CraftRecipe extends CraftReward {

	@XmlAttribute(name = "level", required = true)
	protected int level;

	 /**
	  * 获取 level 属性值。
	  * Gets the value of the level property
	  */
	public int getLevel() {
		return level;
	}
}
