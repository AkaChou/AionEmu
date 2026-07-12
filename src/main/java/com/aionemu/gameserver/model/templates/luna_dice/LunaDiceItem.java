package com.aionemu.gameserver.model.templates.luna_dice;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 月华 Dice 物品模板（静态数据/XML）。
 * XML template.
 */

@XmlType(name = "LunaDiceItem")
public class LunaDiceItem {
	@XmlAttribute(name = "item_id")
	protected int item_id;

	@XmlAttribute(name = "count")
	protected int count;

	/** 返回物品 ID / Returns the item id */
	public final int getItemId() {
		return item_id;
	}

	/** 获取计数。 / Returns the count. */
	public final int getCount() {
		return count;
	}
}
