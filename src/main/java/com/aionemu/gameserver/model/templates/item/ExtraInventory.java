package com.aionemu.gameserver.model.templates.item;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Extra 背包模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ExtraInventory")
public class ExtraInventory {
	@XmlAttribute(required = true)
	protected int id;

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}
}
