package com.aionemu.gameserver.model.templates.rewards;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * Medal 物品模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MedalReward")
public class MedalItem extends IdLevelReward {

	/** 获取计数。 / Returns the count. */
	@Getter
	@XmlAttribute(name = "count")
	protected int count;

	/** 返回概率 / Returns the chance*/
	@Getter
	@XmlAttribute(name = "chance")
	protected float chance;
}
