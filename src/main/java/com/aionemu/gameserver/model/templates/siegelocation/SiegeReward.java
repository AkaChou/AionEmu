package com.aionemu.gameserver.model.templates.siegelocation;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 要塞奖励模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SiegeReward")
public class SiegeReward {
	@XmlAttribute(name = "top")
	protected int top;

	@XmlAttribute(name = "itemid")
	protected int itemId;

	@XmlAttribute(name = "m_count")
	protected int mCount;

	/** 返回 top / Returns the top */
	public int getTop() {
		return top;
	}

	/** 返回物品 ID / Returns the item id */
	public int getItemId() {
		return itemId;
	}

	/** 获取计数。 / Returns the count. */
	public int getCount() {
		return mCount;
	}
}
