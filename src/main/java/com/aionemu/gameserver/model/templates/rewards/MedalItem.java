package com.aionemu.gameserver.model.templates.rewards;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Medal 物品模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MedalReward")
public class MedalItem extends IdLevelReward {

	@XmlAttribute(name = "count")
	protected int count;

	@XmlAttribute(name = "chance")
	protected float chance;

	/** 获取计数。 / Returns the count. */
	public int getCount() {
		return count;
	}

	/** 返回概率 / Returns the chance*/
	public float getChance() {
		return chance;
	}
}
