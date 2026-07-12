package com.aionemu.gameserver.model.templates.item;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.templates.stats.ModifiersTemplate;

/**
 * 物品 Enchant 加成模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "ItemEnchantBouns")
public class ItemEnchantBonus {
	@XmlElement(name = "modifiers", required = false)
	private ModifiersTemplate modifiers;
	@XmlAttribute(name = "level")
	private int level;

	public ItemEnchantBonus() {
	}

	/** 获取修正器。 / Returns the modifiers. */
	public ModifiersTemplate getModifiers() {
		return modifiers;
	}

	/** 获取等级。 / Returns the level. */
	public int getLevel() {
		return level;
	}
}
