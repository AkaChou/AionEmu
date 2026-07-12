package com.aionemu.gameserver.model.templates.housing;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Sale 模板（静态数据/XML）。
 * XML template.
 *
 * @author Rolandas
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
@XmlRootElement(name = "sale")
public class Sale {

	@XmlAttribute(name = "point_price", required = true)
	protected int pointPrice;

	@XmlAttribute(name = "gold_price", required = true)
	protected long goldPrice;

	@XmlAttribute(required = true)
	protected int level;

	/** 获取点价格。 / Returns the point price. */
	public int getPointPrice() {
		return pointPrice;
	}

	/** 返回 gold price / Returns the gold price */
	public long getGoldPrice() {
		return goldPrice;
	}

	/** 获取最小等级。 / Returns the min level. */
	public int getMinLevel() {
		return level;
	}
}
