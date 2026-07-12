package com.aionemu.gameserver.model.templates.recipe;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * ComboProduct 模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ComboProduct")
public class ComboProduct {
	@XmlAttribute
	protected int itemid;

	/** 返回物品 ID / Returns the itemid */
	public int getItemid() {
		return itemid;
	}
}
