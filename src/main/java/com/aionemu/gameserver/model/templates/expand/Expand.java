package com.aionemu.gameserver.model.templates.expand;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 扩展模板（静态数据/XML）。
 * Expand template (static data / XML).
 *
 * @author Simple
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Expand")
public class Expand {

	@XmlAttribute(name = "level", required = true)
	protected int level;
	@XmlAttribute(name = "price", required = true)
	protected int price;

	/**
	 * @return 扩展等级 / The level
	 */
	public int getLevel() {
		return level;
	}

	/**
	 * @return 扩展价格（基纳） / The price
	 */
	public int getPrice() {
		return price;
	}
}
