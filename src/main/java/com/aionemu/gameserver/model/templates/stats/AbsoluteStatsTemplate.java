package com.aionemu.gameserver.model.templates.stats;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Absolute 属性模板（静态数据/XML）。
 * XML template.
 *
 * @author Rolandas
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "StatsSet", propOrder = { "modifiers" })
public class AbsoluteStatsTemplate {

	@XmlElement(required = true)
	protected ModifiersTemplate modifiers;

	@XmlAttribute(required = true)
	protected int id;

	/** 获取修正器。 / Returns the modifiers. */
	public ModifiersTemplate getModifiers() {
		return this.modifiers;
	}

	 /**
	  * 获取 id 属性值。
	  * Gets the value of the id property
	  */
	public int getId() {
		return id;
	}
}
