package com.aionemu.gameserver.model.templates.shugosweep;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * 术古清扫奖励模板（静态数据/XML）。
 * XML template.
 */

@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ShugoSweepReward")
public class ShugoSweepReward {
	/** 返回 board id / Returns the board id */
	@XmlAttribute(name = "board_id")
	protected int boardId;

	/** 返回 reward num / Returns the reward num */
	@XmlAttribute(name = "reward_num")
	protected int rewardNum;

	/** 返回物品 ID / Returns the item id */
	@XmlAttribute(name = "item_id")
	protected int itemId;

	/** 获取计数。 / Returns the count. */
	@XmlAttribute(name = "count")
	protected int count;
}
