package com.aionemu.gameserver.model.templates.item;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.templates.stats.ModifiersTemplate;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 物品强化加成模板：按等级附加属性修正。
 * Item enchant bonus template: attribute modifiers per level.
 */

@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "ItemEnchantBouns")
@NoArgsConstructor
public class ItemEnchantBonus {
	/** 获取修正器。 / Returns the modifiers. */
	@XmlElement(name = "modifiers", required = false)
	private ModifiersTemplate modifiers;
	/** 获取等级。 / Returns the level. */
	@XmlAttribute(name = "level")
	private int level;
}
