package com.aionemu.gameserver.questEngine.handlers.models;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 奖励条目列表容器（{@code <rewards>} 根元素）。
 * Container for reward entries ({@code <rewards>} root element).
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "reward" })
@XmlRootElement(name = "rewards")
public class Rewards {

	/**
	 * 奖励条目列表。
	 * List of reward entries.
	 */
	@XmlElement(required = true)
	protected List<Reward> reward;

	/**
	 * 返回奖励列表；若尚未初始化则惰性创建空列表。
	 * Returns the reward list; lazily creates an empty list when null.
	 *
	 * Reward list
	 */
	public List<Reward> getReward() {
		if (reward == null) {
			reward = new ArrayList<Reward>();
		}
		return this.reward;
	}
}
