package com.aionemu.gameserver.model.templates.siegelocation;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * 要塞军团奖励模板（静态数据/XML）。
 * XML template.
 */

@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SiegeLegionReward")
public class SiegeLegionReward {
	/** 返回物品 ID / Returns the item id */
	@XmlAttribute(name = "itemid")
	protected int itemId;

	@XmlAttribute(name = "m_count")
	protected int mCount;

	/** 获取计数。 / Returns the count. */
	public int getCount() {
		return mCount;
	}
}
