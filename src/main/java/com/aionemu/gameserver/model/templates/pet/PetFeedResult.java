package com.aionemu.gameserver.model.templates.pet;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 宠物 Feed 结果模板（静态数据/XML）。
 * XML template.
 *
 * @author Rolandas
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PetFeedResult")
public class PetFeedResult {

	@XmlAttribute(required = true)
	protected int item;

	@XmlAttribute
	protected String name;

	/** 获取物品。 / Returns the item. */
	public int getItem() {
		return item;
	}

	/** 返回字符串表示。 / Returns string representation. */
	@Override
	public String toString() {
		return name + " (" + item + ")";
	}
}
