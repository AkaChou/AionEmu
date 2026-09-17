package com.aionemu.gameserver.model.templates.chest;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * 钥匙物品模板（静态数据/XML）。
 * Key item template (static data / XML).
 *
 * @author Wakizashi
 */
@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "KeyItem")
public class KeyItem {

	/**
	 * @return 钥匙 ID / the id
	 */
	@XmlAttribute(name = "id")
	protected int id;
	@XmlAttribute(name = "itemid")
	protected int itemid;
	/**
	 * @return 数量 / the quantity
	 */
	@XmlAttribute(name = "quantity")
	protected int quantity;

	/**
	 * @return 物品 ID / the itemid
	 */
	public int getItemId() {
		return itemid;
	}
}
