package com.aionemu.gameserver.model.templates.item.bonuses;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.stats.ModifiersTemplate;

/**
 * Random 加成模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RandomBonus", propOrder = { "modifiers" })
public class RandomBonus {
	@XmlElement(required = true)
	protected List<ModifiersTemplate> modifiers;

	@XmlAttribute(required = true)
	protected int id;

	@XmlAttribute(name = "type", required = true)
	private StatBonusType bonusType;

	/** 获取修正器。 / Returns the modifiers. */
	public List<ModifiersTemplate> getModifiers() {
		if (modifiers == null) {
			modifiers = new ArrayList<ModifiersTemplate>();
		}
		return this.modifiers;
	}

	 /**
	  * 获取 id 属性值。
	  * Gets the value of the id property
	  */
	public int getId() {
		return id;
	}

	/** 获取加成类型。 / Returns the bonus type. */
	public StatBonusType getBonusType() {
		return bonusType;
	}
}
