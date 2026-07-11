package com.aionemu.gameserver.model.templates.shugosweep;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 术古清扫奖励模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ShugoSweepReward")
public class ShugoSweepReward {
	@XmlAttribute(name = "board_id")
	protected int boardId;

	@XmlAttribute(name = "reward_num")
	protected int rewardNum;

	@XmlAttribute(name = "item_id")
	protected int itemId;

	@XmlAttribute(name = "count")
	protected int count;

	/** 返回 board id / Returns the board id */
	public int getBoardId() {
		return boardId;
	}

	/** 返回 reward num / Returns the reward num */
	public int getRewardNum() {
		return rewardNum;
	}

	/** 返回物品 ID / Returns the item id */
	public int getItemId() {
		return itemId;
	}

	/** 获取计数。 / Returns the count. */
	public int getCount() {
		return count;
	}
}
