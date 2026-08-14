package com.aionemu.gameserver.model.templates.item;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 物品获取模板：按类型（欧比斯/奖励/兑换券）与数量定义获取条件。
 * Item acquisition template: defines acquisition by type (AP/reward/coupon) and quantity.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Acquisition")
public class Acquisition {

	@XmlAttribute(name = "ap", required = false)
	private int ap = 0;

	@XmlAttribute(name = "count", required = false)
	private int itemCount;

	@XmlAttribute(name = "item", required = false)
	private int itemId;

	@XmlAttribute(name = "type", required = true)
	private AcquisitionType type;

	/** 获取类型。 / Returns the type. */
	public AcquisitionType getType() {
		return type;
	}

	/** 返回物品 ID / Returns the item id */
	public int getItemId() {
		return itemId;
	}

	/** 获取物品计数。 / Returns the item count. */
	public int getItemCount() {
		return itemCount;
	}

	/** 返回所需欧比斯点数 / Returns the required AP */
	public int getRequiredAp() {
		return ap;
	}
}
