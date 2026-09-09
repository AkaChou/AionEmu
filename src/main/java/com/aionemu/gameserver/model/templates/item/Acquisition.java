package com.aionemu.gameserver.model.templates.item;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * 物品获取模板：按类型（欧比斯/奖励/兑换券）与数量定义获取条件。
 * Item acquisition template: defines acquisition by type (AP/reward/coupon) and quantity.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Acquisition")
public class Acquisition {

	@XmlAttribute(name = "ap", required = false)
	private int ap = 0;

	/** 获取物品计数。 / Returns the item count. */
	@Getter
	@XmlAttribute(name = "count", required = false)
	private int itemCount;

	/** 返回物品 ID / Returns the item id */
	@Getter
	@XmlAttribute(name = "item", required = false)
	private int itemId;

	/** 获取类型。 / Returns the type. */
	@Getter
	@XmlAttribute(name = "type", required = true)
	private AcquisitionType type;

	/** 返回所需欧比斯点数 / Returns the required AP */
	public int getRequiredAp() {
		return ap;
	}
}
