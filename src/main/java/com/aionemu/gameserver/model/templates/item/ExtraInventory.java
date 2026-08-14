package com.aionemu.gameserver.model.templates.item;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 额外背包模板：扩展背包 ID。
 * Extra inventory template: extended cube id.
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
