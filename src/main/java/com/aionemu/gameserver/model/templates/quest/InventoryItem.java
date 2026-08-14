package com.aionemu.gameserver.model.templates.quest;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 背包物品模板（静态数据/XML）。
 * Inventory item template (static data / XML).
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "InventoryItem")
public class InventoryItem {

	@XmlAttribute(name = "item_id")
	protected Integer itemId;

	/** 返回物品 ID / Returns the item id */
	public Integer getItemId() {
		return itemId;
	}
}
