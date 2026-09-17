package com.aionemu.gameserver.model.templates.ingameshop;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * IGSub 分类模板（静态数据/XML）。
 * XML template.
 */

@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "IGSubCategory")
public class IGSubCategory {

	/** 返回 ID / Returns the id */
	@XmlAttribute(required = true)
	protected int id;

	/** 获取名称。 / Returns the name. */
	@XmlAttribute(required = true)
	protected String name;
}
