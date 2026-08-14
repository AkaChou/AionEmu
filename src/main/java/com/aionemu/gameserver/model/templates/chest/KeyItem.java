package com.aionemu.gameserver.model.templates.chest;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 钥匙物品模板（静态数据/XML）。
 * Key item template (static data / XML).
 *
 * @author Wakizashi
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "KeyItem")
public class KeyItem {

	@XmlAttribute(name = "id")
	protected int id;
	@XmlAttribute(name = "itemid")
	protected int itemid;
	@XmlAttribute(name = "quantity")
	protected int quantity;

	/**
	 * @return 钥匙 ID / the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @return 物品 ID / the itemid
	 */
	public int getItemId() {
		return itemid;
	}

	/**
	 * @return 数量 / the quantity
	 */
	public int getQuantity() {
		return quantity;
	}
}
