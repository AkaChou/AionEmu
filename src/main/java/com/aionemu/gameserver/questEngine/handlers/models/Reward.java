package com.aionemu.gameserver.questEngine.handlers.models;

import java.math.BigInteger;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 工作订单等任务中的单项奖励条目（物品、数量、序号、等级）。
 * Single reward entry for work-order style quests (item, count, index, rank).
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
@XmlRootElement(name = "reward")
public class Reward {

	/**
	 * 奖励数量。
	 * Reward quantity.
	 */
	@XmlAttribute(required = true)
	protected BigInteger count;

	/**
	 * 奖励物品模板 ID。
	 * Reward item template id.
	 */
	@XmlAttribute(name = "item_id", required = true)
	protected BigInteger itemId;

	/**
	 * 奖励条目序号。
	 * serial number.
	 */
	@XmlAttribute(required = true)
	protected BigInteger no;

	/**
	 * 奖励等级 / 档位。
	 * tier.
	 */
	@XmlAttribute(required = true)
	protected BigInteger rank;

	/**
	 * 返回奖励数量。
	 * Returns the reward quantity.
	 *
	 * Count
	 */
	public BigInteger getCount() {
		return count;
	}

	/**
	 * 设置奖励数量。
	 * Sets the reward quantity.
	 *
	 * Count
	 */
	public void setCount(BigInteger value) {
		this.count = value;
	}

	/**
	 * 返回奖励物品 ID。
	 * Returns the reward item id.
	 *
	 * Item id
	 */
	public BigInteger getItemId() {
		return itemId;
	}

	/**
	 * 设置奖励物品 ID。
	 * Sets the reward item id.
	 *
	 * Item id
	 */
	public void setItemId(BigInteger value) {
		this.itemId = value;
	}

	/**
	 * 返回奖励序号。
	 * Returns the reward serial number.
	 *
	 * Serial number
	 */
	public BigInteger getNo() {
		return no;
	}

	/**
	 * 设置奖励序号。
	 * Sets the reward serial number.
	 *
	 * Serial number
	 */
	public void setNo(BigInteger value) {
		this.no = value;
	}

	/**
	 * 返回奖励等级。
	 * Returns the reward rank.
	 *
	 * Rank
	 */
	public BigInteger getRank() {
		return rank;
	}

	/**
	 * 设置奖励等级。
	 * Sets the reward rank.
	 *
	 * Rank
	 */
	public void setRank(BigInteger value) {
		this.rank = value;
	}
}
