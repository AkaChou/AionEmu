package com.aionemu.gameserver.model.templates.housing;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * Sale 模板（静态数据/XML）。
 * XML template.
 *
 * @author Rolandas
 */
@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
@XmlRootElement(name = "sale")
public class Sale {

	/** 获取点价格。 / Returns the point price. */
	@XmlAttribute(name = "point_price", required = true)
	protected int pointPrice;

	/** 返回 gold price / Returns the gold price */
	@XmlAttribute(name = "gold_price", required = true)
	protected long goldPrice;

	@XmlAttribute(required = true)
	protected int level;

	/** 获取最小等级。 / Returns the min level. */
	public int getMinLevel() {
		return level;
	}
}
