package com.aionemu.gameserver.model.templates.recipe;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 月华 Component 元素模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "LunaComponentElement")
public class LunaComponentElement {
	@XmlAttribute
	protected int itemid;

	@XmlAttribute
	protected int quantity;

	/** 返回物品 ID / Returns the itemid */
	public Integer getItemid() {
		return itemid;
	}

	/** 返回 quantity / Returns the quantity */
	public Integer getQuantity() {
		return quantity;
	}
}
