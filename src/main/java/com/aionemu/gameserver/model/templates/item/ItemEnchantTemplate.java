package com.aionemu.gameserver.model.templates.item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.model.stats.calc.functions.StatFunction;

/**
 * 物品 Enchant 模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "ItemEnchantTemplate")
public class ItemEnchantTemplate {
	@XmlAttribute(name = "id")
	private int id;

	@XmlAttribute(name = "type")
	private EnchantType type;

	@XmlElement(name = "item_enchant", required = false)
	private List<ItemEnchantBonus> item_enchant;

	@XmlTransient
	private Map<Integer, List<StatFunction>> enchants = new HashMap<Integer, List<StatFunction>>();

	/** 获取属性。 / Returns the stats. */
	public List<StatFunction> getStats(int level) {
		if (this.enchants.containsKey(level)) {
			return this.enchants.get(level);
		}
		return null;
	}

	/** 返回物品强化 / Returns the item enchant*/
	public List<ItemEnchantBonus> getItemEnchant() {
		return this.item_enchant;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return this.id;
	}

	/** 返回强化类型 / Returns the enchant type*/
	public EnchantType getEnchantType() {
		return this.type;
	}
}
