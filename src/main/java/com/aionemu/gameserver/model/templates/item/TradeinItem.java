package com.aionemu.gameserver.model.templates.item;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Tradein 物品模板（静态数据/XML）。
 * XML template.
 *
 * @author MrPoke
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TradeinItem")
public class TradeinItem {

	@XmlAttribute
	protected int id;
	@XmlAttribute
	protected int price;

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 获取价格。 / Returns the price. */
	public int getPrice() {
		return price;
	}
}
