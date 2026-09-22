package com.aionemu.gameserver.model.templates.stats;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * Absolute 属性模板（静态数据/XML）。
 * XML template.
 * @author Rolandas
 */
@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "StatsSet", propOrder = { "modifiers" })
public class AbsoluteStatsTemplate {

	/** 获取修正器。 / Returns the modifiers. */
	@XmlElement(required = true)
	protected ModifiersTemplate modifiers;

	/**
	 * 获取 id 属性值。
	 * Gets the value of the id property
	 */
	@XmlAttribute(required = true)
	protected int id;
}
