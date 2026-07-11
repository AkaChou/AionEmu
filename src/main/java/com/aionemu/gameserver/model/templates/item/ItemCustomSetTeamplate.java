package com.aionemu.gameserver.model.templates.item;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * 物品自定义 SetTeamplate 模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "ItemCustomSetTeamplate")
public class ItemCustomSetTeamplate {
	@XmlAttribute(name = "id")
	private int id;

	@XmlAttribute(name = "name")
	private String name;

	@XmlAttribute(name = "custom_enchant_value")
	private int custom_enchant_value;

	/** 返回 ID / Returns the id */
	public int getId() {
		return this.id;
	}

	/** 获取名称。 / Returns the name. */
	public String getName() {
		return this.name;
	}

	/** 返回 custom enchant value / Returns the custom enchant value */
	public int getCustomEnchantValue() {
		return this.custom_enchant_value;
	}
}
