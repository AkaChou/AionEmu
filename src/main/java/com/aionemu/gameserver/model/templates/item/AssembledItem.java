package com.aionemu.gameserver.model.templates.item;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 组装物品模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AssembledItem")
public class AssembledItem {

	@XmlAttribute(name = "id", required = true)
	private int id;

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}
}
