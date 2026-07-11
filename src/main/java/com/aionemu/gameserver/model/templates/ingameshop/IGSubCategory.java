package com.aionemu.gameserver.model.templates.ingameshop;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * IGSub 分类模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "IGSubCategory")
public class IGSubCategory {

	@XmlAttribute(required = true)
	protected int id;

	@XmlAttribute(required = true)
	protected String name;

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 获取名称。 / Returns the name. */
	public String getName() {
		return name;
	}
}
