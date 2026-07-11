package com.aionemu.gameserver.model.templates.npc;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * MassiveLooting 模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MassiveLooting")
public class MassiveLooting {
	@XmlAttribute
	protected int itemid;

	@XmlAttribute(name = "looting_num")
	protected int lootingNum;

	@XmlAttribute(name = "min_level")
	protected int minLevel;

	@XmlAttribute(name = "max_level")
	protected int maxLevel;

	/** 返回 looting num / Returns the looting num */
	public int getLootingNum() {
		return lootingNum;
	}

	/** 返回物品 ID / Returns the itemid */
	public int getItemid() {
		return itemid;
	}

	/** 获取最小等级。 / Returns the min level. */
	public int getMinLevel() {
		return minLevel;
	}

	/** 获取最大等级。 / Returns the max level. */
	public int getMaxLevel() {
		return maxLevel;
	}
}
