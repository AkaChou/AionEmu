package com.aionemu.gameserver.model.templates.quest;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * 奖励模板（静态数据/XML）。
 * XML template.
 */

@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Rewards", propOrder = { "selectableRewardItem", "rewardItem" })
public class Rewards {
	@XmlElement(name = "selectable_reward_item")
	protected List<QuestItems> selectableRewardItem;

	@XmlElement(name = "reward_item")
	protected List<QuestItems> rewardItem;

	/** 返回基纳 / Returns the gold */
	@XmlAttribute
	protected Integer gold;

	/** 获取经验。 / Returns the exp. */
	@XmlAttribute
	protected Integer exp;

	/** 返回经验加成 / Returns the exp boost. */
	@XmlAttribute
	protected Integer expBoost;

	/** 获取神圣能量。 / Returns the dp. */
	@XmlAttribute
	protected Integer dp;

	/** 获取欧比斯点数。 / Returns the ap. */
	@XmlAttribute
	protected Integer ap;

	/** 返回荣耀点数 / Returns the gp */
	@XmlAttribute
	protected Integer gp;

	/** 返回 Abyss Op / Returns the abyss op */
	@XmlAttribute
	protected Integer abyssOp;

	@XmlAttribute
	protected Integer cp;

	/** 获取称号。 / Returns the title. */
	@XmlAttribute
	protected Integer title;

	/** 返回扩展背包数量 / Returns the extend inventory */
	@XmlAttribute(name = "extend_inventory")
	protected Integer extendInventory;

	/** 返回扩展烙印之石槽数量 / Returns the extend stigma */
	@XmlAttribute(name = "extend_stigma")
	protected Integer extendStigma;

	/** 返回可选奖励物品列表 / Returns the selectable reward items */
	public List<QuestItems> getSelectableRewardItem() {
		if (selectableRewardItem == null) {
			selectableRewardItem = new ArrayList<QuestItems>();
		}
		return this.selectableRewardItem;
	}

	/** 获取奖励物品。 / Returns the reward item. */
	public List<QuestItems> getRewardItem() {
		if (rewardItem == null) {
			rewardItem = new ArrayList<QuestItems>();
		}
		return this.rewardItem;
	}

	/** 获取创造点。 / Returns the cp. */
	public Integer getCP() {
		return cp;
	}
}
