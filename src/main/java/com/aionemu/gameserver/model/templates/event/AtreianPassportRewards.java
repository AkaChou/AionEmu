package com.aionemu.gameserver.model.templates.event;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * 艾特里亚 Passport 奖励模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author Falke_34
 */
@XmlRootElement(name = "atreian_passport_reward")
@XmlAccessorType(XmlAccessType.FIELD)
public class AtreianPassportRewards {

	@XmlAttribute(name = "name")
	private String name = "";
	@XmlAttribute(name = "reward_item", required = true)
	private int rewardItem;
	@XmlAttribute(name = "reward_item_count", required = true)
	private int rewardItemCount;
	@XmlAttribute(name = "reward_item_num", required = true)
	private int rewardItemNum;
	@XmlAttribute(name = "reward_permit_level")
	private int rewardPermitLevel;

	/** 获取名称。 / Returns the name. */
	public String getName() {
		return name;
	}

	/** 获取奖励物品。 / Returns the reward item. */
	public int getRewardItem() {
		return rewardItem;
	}

	/** 获取奖励物品计数。 / Returns the reward item count. */
	public int getRewardItemCount() {
		return rewardItemCount;
	}

	/** 返回 reward item num / Returns the reward item num */
	public int getRewardItemNum() {
		return rewardItemNum;
	}

	/** 返回 reward permit level / Returns the reward permit level */
	public int getRewardPermitLevel() {
		return rewardPermitLevel;
	}
}
