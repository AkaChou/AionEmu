package com.aionemu.gameserver.model.templates.event;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;

/**
 * 艾特里亚 Passport 奖励模板（静态数据/XML）。
 * Atreian Passport Rewards Template (static data/XML).
 * @author Falke_34
 */
@Getter
@XmlRootElement(name = "atreian_passport_reward")
@XmlAccessorType(XmlAccessType.FIELD)
public class AtreianPassportRewards {

	/** 获取名称。 / Returns the name. */
	@XmlAttribute(name = "name")
	private String name = "";
	/** 获取奖励物品。 / Returns the reward item. */
	@XmlAttribute(name = "reward_item", required = true)
	private int rewardItem;
	/** 获取奖励物品计数。 / Returns the reward item count. */
	@XmlAttribute(name = "reward_item_count", required = true)
	private int rewardItemCount;
	/** 返回奖励物品序号 / Returns the reward item num */
	@XmlAttribute(name = "reward_item_num", required = true)
	private int rewardItemNum;
	/** 返回奖励发放等级 / Returns the reward permit level */
	@XmlAttribute(name = "reward_permit_level")
	private int rewardPermitLevel;
}
