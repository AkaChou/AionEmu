package com.aionemu.gameserver.model.templates.item;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Disposition 模板（静态数据/XML）。
 * XML template. / XML template.
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
