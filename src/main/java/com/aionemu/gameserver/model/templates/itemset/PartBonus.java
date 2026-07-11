package com.aionemu.gameserver.model.templates.itemset;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.stats.calc.functions.StatFunction;
import com.aionemu.gameserver.model.templates.stats.ModifiersTemplate;

/**
 * Part 加成模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author ATracer
 */
@XmlRootElement(name = "PartBonus")
@XmlAccessorType(XmlAccessType.FIELD)
public class PartBonus {

	@XmlAttribute
	protected int count;
	@XmlElement(name = "modifiers", required = false)
	protected ModifiersTemplate modifiers;

	/** 获取修正器。 / Returns the modifiers. */
	public List<StatFunction> getModifiers() {
		return modifiers != null ? modifiers.getModifiers() : null;
	}

	/**
	 * @return the count
	 */
	public int getCount() {
		return count;
	}
}
