package com.aionemu.gameserver.model.templates.pet;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * 宠物喂食结果模板（静态数据/XML）。
 * Pet feed result template (static data / XML).
 * @author Rolandas
 */
@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PetFeedResult")
public class PetFeedResult {

	/** 获取物品。 / Returns the item. */
	@XmlAttribute(required = true)
	protected int item;

	@XmlAttribute
	protected String name;

	/** 返回字符串表示。 / Returns string representation. */
	@Override
	public String toString() {
		return name + " (" + item + ")";
	}
}
