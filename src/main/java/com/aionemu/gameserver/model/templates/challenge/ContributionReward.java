package com.aionemu.gameserver.model.templates.challenge;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 贡献奖励模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ContributionReward")
public class ContributionReward {
	@XmlAttribute(name = "item_count", required = true)
	protected int itemCount;

	@XmlAttribute(name = "reward_id", required = true)
	protected int rewardId;

	@XmlAttribute(required = true)
	protected int number;

	@XmlAttribute(required = true)
	protected int rank;

	/** 获取物品计数。 / Returns the item count. */
	public int getItemCount() {
		return this.itemCount;
	}

	/** 返回奖励 ID / Returns the reward id */
	public int getRewardId() {
		return this.rewardId;
	}

	/** 返回编号 / Returns the number */
	public int getNumber() {
		return this.number;
	}

	/** 获取军阶。 / Returns the rank. */
	public int getRank() {
		return this.rank;
	}
}
