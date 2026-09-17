package com.aionemu.gameserver.model.templates.item;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;

/**
 * 物品自定义套装模板：自定义强化值。
 * Custom item set template: custom enchant value.
 */

@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "ItemCustomSetTeamplate")
public class ItemCustomSetTeamplate {
	/** 返回 ID / Returns the id */
	@XmlAttribute(name = "id")
	private int id;

	/** 获取名称。 / Returns the name. */
	@XmlAttribute(name = "name")
	private String name;

	@XmlAttribute(name = "custom_enchant_value")
	private int custom_enchant_value;

	/** 返回自定义强化值 / Returns the custom enchant value */
	public int getCustomEnchantValue() {
		return this.custom_enchant_value;
	}
}
