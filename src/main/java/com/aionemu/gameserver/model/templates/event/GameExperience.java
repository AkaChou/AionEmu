package com.aionemu.gameserver.model.templates.event;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * 游戏经验模板（静态数据/XML）。
 * Game Experience Template (static data/XML).
 *
 * @author Rinzler (Encom)
 */

@XmlRootElement(name = "game_experience_item")
@XmlAccessorType(XmlAccessType.NONE)
public class GameExperience {
	@XmlAttribute(name = "id", required = true)
	private int id;

	@XmlAttribute(name = "account_type", required = true)
	private AccountType accountType;

	@XmlAttribute(name = "reward_item", required = true)
	private int rewardItem;

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 获取账号类型。 / Returns the account type. */
	public AccountType getAccountType() {
		return accountType;
	}

	/** 获取奖励物品。 / Returns the reward item. */
	public int getRewardItem() {
		return rewardItem;
	}
}
