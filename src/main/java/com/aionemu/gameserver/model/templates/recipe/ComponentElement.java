package com.aionemu.gameserver.model.templates.recipe;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Component 元素模板（静态数据/XML）。
 * XML template.
 *
 * @author Ranastic
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ComponentElement")
public class ComponentElement {

	@XmlAttribute
	protected int itemid;
	@XmlAttribute
	protected int quantity;

	/** 返回物品 ID / Returns the item id */
	public Integer getItemid() {
		return itemid;
	}

	/** 返回数量 / Returns the quantity */
	public Integer getQuantity() {
		return quantity;
	}
}
