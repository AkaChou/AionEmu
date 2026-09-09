package com.aionemu.gameserver.model.templates.ingameshop;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * IG 分类模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "IGCategory")
public class IGCategory {

	@XmlElement(name = "sub_category")
	protected List<IGSubCategory> subCategories;

	/** 返回 ID / Returns the id */
	@Getter
	@XmlAttribute(required = true)
	protected int id;

	/** 获取名称。 / Returns the name. */
	@Getter
	@XmlAttribute(required = true)
	protected String name;

	/** 返回 sub categories / Returns the sub categories */
	public List<IGSubCategory> getSubCategories() {
		if (subCategories == null) {
			subCategories = new ArrayList<IGSubCategory>();
		}
		return subCategories;
	}
}
