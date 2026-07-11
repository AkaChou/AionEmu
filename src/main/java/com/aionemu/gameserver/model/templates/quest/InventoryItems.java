package com.aionemu.gameserver.model.templates.quest;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 背包物品模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "InventoryItems", propOrder = { "inventoryItem" })
public class InventoryItems {

	@XmlElement(name = "inventory_item")
	protected List<InventoryItem> inventoryItem;

	/** 获取背包物品。 / Returns the inventory item. */
	public List<InventoryItem> getInventoryItem() {
		if (inventoryItem == null) {
			inventoryItem = new ArrayList<InventoryItem>();
		}
		return inventoryItem;
	}
}
