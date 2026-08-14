package com.aionemu.gameserver.model.templates.siegelocation;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 月华奖励模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "LunaReward")
public class LunaReward {
	@XmlAttribute(name = "itemid")
	protected int itemId;

	@XmlAttribute(name = "l_count")
	protected int lCount;

	/** 返回物品 ID / Returns the item id */
	public int getItemId() {
		return itemId;
	}

	/** 返回 l_count 计数 / Returns the l_count */
	public int getLount() {
		return lCount;
	}
}
