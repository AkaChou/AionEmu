package com.aionemu.gameserver.model.templates.item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.model.stats.calc.functions.StatFunction;
import com.aionemu.gameserver.model.templates.stats.ModifiersTemplate;
import lombok.Getter;

/**
 * 物品强化/觉醒模板：表内按等级查表，超出表内最高级后交由 {@link EnchantGrowth} 继续成长。
 * Item enchant/authorize template: per-level stats inside the table, further levels grown by {@link EnchantGrowth}.
 */

@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "ItemEnchantTemplate")
public class ItemEnchantTemplate {
	/** 返回 ID / Returns the id */
	@XmlAttribute(name = "id")
	private int id;

	@XmlAttribute(name = "type")
	private EnchantType type;

	@XmlElement(name = "item_enchant", required = false)
	private List<ItemEnchantBonus> item_enchant;

	@XmlTransient
	private final Map<Integer, List<StatFunction>> enchants = new HashMap<>();

	@XmlTransient
	private int maxLevel;

	/**
	 * JAXB 反序列化完成后按等级建立属性索引。
	 * Builds the level index after JAXB unmarshalling.
	 */
	void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
		if (item_enchant == null) {
			return;
		}
		for (ItemEnchantBonus bonus : item_enchant) {
			ModifiersTemplate modifiers = bonus.getModifiers();
			if (modifiers != null && modifiers.getModifiers() != null && !modifiers.getModifiers().isEmpty()) {
				enchants.put(bonus.getLevel(), modifiers.getModifiers());
				maxLevel = Math.max(maxLevel, bonus.getLevel());
			}
		}
	}

	/**
	 * 获取属性。 / Returns the stats.
	 * 表内等级直接查表；超出表内最高级时按真端 limitless 数据继续成长。
	 * Levels inside the table are looked up directly, higher levels grow with the retail limitless data.
	 *
	 * @param level 强化/觉醒等级 / enchant or authorize level
	 * @return 属性列表，表内无数据时为 null / stat list, or null when the table carries none
	 */
	public List<StatFunction> getStats(int level) {
		List<StatFunction> stats = this.enchants.get(level);
		if (stats == null) {
			stats = this.enchants.get(this.maxLevel);
		}
		return EnchantGrowth.grow(this.id, level, stats, this.maxLevel);
	}

	/** 返回物品强化列表 / Returns the item enchant list */
	public List<ItemEnchantBonus> getItemEnchant() {
		return this.item_enchant;
	}

	/** 返回强化类型 / Returns the enchant type */
	public EnchantType getEnchantType() {
		return this.type;
	}
}
