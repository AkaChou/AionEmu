package com.aionemu.gameserver.model.templates.item;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 处理方式模板：物品处置计数。
 * Disposition template: item disposal count.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Disposition")
public class Disposition {
	@XmlAttribute
	protected int count;

	@XmlAttribute
	protected int id;

	/** 获取计数。 / Returns the count. */
	public int getCount() {
		return count;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}
}
