package com.aionemu.gameserver.model.templates.item;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Acquisition 模板（静态数据/XML）。
 * XML template. / XML template.
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

	/** 返回 required ap / Returns the required ap */
	public int getRequiredAp() {
		return ap;
	}
}
