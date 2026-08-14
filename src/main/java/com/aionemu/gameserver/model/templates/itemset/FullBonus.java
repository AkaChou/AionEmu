package com.aionemu.gameserver.model.templates.itemset;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.stats.calc.functions.StatFunction;
import com.aionemu.gameserver.model.templates.stats.ModifiersTemplate;

/**
 * 完整套装加成模板（静态数据/XML）。
 * Full set bonus template (static data/XML).
 *
 * @author ATracer
 */
@XmlRootElement(name = "FullBonus")
@XmlAccessorType(XmlAccessType.FIELD)
public class FullBonus {

	@XmlElement(name = "modifiers", required = false)
	protected ModifiersTemplate modifiers;

	private int totalnumberofitems;

	/** 获取修正器。 / Returns the modifiers. */
	public List<StatFunction> getModifiers() {
		return modifiers != null ? modifiers.getModifiers() : null;
	}

	/**
	 * @return Value of the number of items in the set
	 */
	public int getCount() {
		return totalnumberofitems;
	}

	/**
	 * 设置 numberitemswhenbonusapplies。
	 * Sets number of items in the set (when this bonus applies)
	 *
	 * @param number
	 */
	public void setNumberOfItems(int number) {
		this.totalnumberofitems = number;
	}
}
