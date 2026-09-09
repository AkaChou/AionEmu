package com.aionemu.gameserver.model.templates.rewards;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.itemgroups.ItemRaceEntry;
import lombok.Getter;

/**
 * ID 等级奖励模板（静态数据/XML）。
 * XML template.
 *
 * @author Rolandas
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "IdLevelReward")
public class IdLevelReward extends ItemRaceEntry {

	/**
	 * 获取 level 属性值。
	 * Gets the value of the level property
	 */
	@Getter
	@XmlAttribute(name = "level", required = true)
	protected int level;
}
