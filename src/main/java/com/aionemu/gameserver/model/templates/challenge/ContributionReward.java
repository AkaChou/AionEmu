package com.aionemu.gameserver.model.templates.challenge;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * 贡献奖励模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ContributionReward")
public class ContributionReward {
	/** 获取物品计数。 / Returns the item count. */
	@Getter
	@XmlAttribute(name = "item_count", required = true)
	protected int itemCount;

	/** 返回奖励 ID / Returns the reward id */
	@Getter
	@XmlAttribute(name = "reward_id", required = true)
	protected int rewardId;

	/** 返回编号 / Returns the number */
	@Getter
	@XmlAttribute(required = true)
	protected int number;

	/** 获取军阶。 / Returns the rank. */
	@Getter
	@XmlAttribute(required = true)
	protected int rank;
}
