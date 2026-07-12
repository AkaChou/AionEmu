package com.aionemu.gameserver.model.templates.expand;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Expand 模板（静态数据/XML）。
 * XML template.
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
	 * @return the level
	 */
	public int getLevel() {
		return level;
	}

	/**
	 * @return the price
	 */
	public int getPrice() {
		return price;
	}
}
