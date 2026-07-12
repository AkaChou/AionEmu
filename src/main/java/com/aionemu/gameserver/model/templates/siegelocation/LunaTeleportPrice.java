package com.aionemu.gameserver.model.templates.siegelocation;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 月华传送价格模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "LunaTeleportPrice")
public class LunaTeleportPrice {
	@XmlAttribute(name = "itemid")
	protected int itemId;

	/** 返回物品 ID / Returns the item id */
	public int getItemId() {
		return itemId;
	}
}
