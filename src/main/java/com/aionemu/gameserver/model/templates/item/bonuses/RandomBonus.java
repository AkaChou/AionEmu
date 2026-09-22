package com.aionemu.gameserver.model.templates.item.bonuses;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.stats.ModifiersTemplate;
import lombok.Getter;

/**
 * 随机加成模板（静态数据/XML）。
 * Random bonus template (static data / XML).
 */

@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RandomBonus", propOrder = { "modifiers" })
public class RandomBonus {
	@XmlElement(required = true)
	protected List<ModifiersTemplate> modifiers;

	/**
	 * 获取加成 ID。
	 * Gets the bonus id.
	 */
	@XmlAttribute(required = true)
	protected int id;

	/** 获取加成类型。 / Returns the bonus type. */
	@XmlAttribute(name = "type", required = true)
	private StatBonusType bonusType;

	/** 获取修正器。 / Returns the modifiers. */
	public List<ModifiersTemplate> getModifiers() {
		if (modifiers == null) {
			modifiers = new ArrayList<>();
		}
		return this.modifiers;
	}
}
