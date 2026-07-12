package com.aionemu.gameserver.model.templates.pet;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 宠物 Merchand 条目模板（静态数据/XML）。
 * XML template.
 *
 * @author Rinzler
 */

@XmlType(name = "merch")
@XmlAccessorType(XmlAccessType.NONE)
public class PetMerchandEntry {
	@XmlAttribute(name = "id", required = true)
	private int id;

	@XmlAttribute(name = "rate_price")
	private int ratePrice;

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 获取比率价格。 / Returns the rate price. */
	public int getRatePrice() {
		return ratePrice;
	}
}
