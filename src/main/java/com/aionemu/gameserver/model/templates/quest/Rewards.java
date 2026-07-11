package com.aionemu.gameserver.model.templates.quest;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 奖励模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Rewards", propOrder = { "selectableRewardItem", "rewardItem" })
public class Rewards {
	@XmlElement(name = "selectable_reward_item")
	protected List<QuestItems> selectableRewardItem;

	@XmlElement(name = "reward_item")
	protected List<QuestItems> rewardItem;

	@XmlAttribute
	protected Integer gold;

	@XmlAttribute
	protected Integer exp;

	@XmlAttribute
	protected Integer expBoost;

	@XmlAttribute
	protected Integer dp;

	@XmlAttribute
	protected Integer ap;

	@XmlAttribute
	protected Integer gp;

	@XmlAttribute
	protected Integer abyssOp;

	@XmlAttribute
	protected Integer cp;

	@XmlAttribute
	protected Integer title;

	@XmlAttribute(name = "extend_inventory")
	protected Integer extendInventory;

	@XmlAttribute(name = "extend_stigma")
	protected Integer extendStigma;

	/** 返回 selectable reward item / Returns the selectable reward item */
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

	/** 返回基纳 / Returns the gold */
	public Integer getGold() {
		return gold;
	}

	/** 获取经验。 / Returns the exp. */
	public Integer getExp() {
		return exp;
	}

	/** 返回经验加速 / Returns the exp boost*/
	public Integer getExpBoost() {
		return expBoost;
	}

	/** 获取神圣能量。 / Returns the dp. */
	public Integer getDp() {
		return dp;
	}

	/** 获取欧比斯点数。 / Returns the ap. */
	public Integer getAp() {
		return ap;
	}

	/** 返回荣耀点 / Returns the gp */
	public Integer getGp() {
		return gp;
	}

	/** 获取创造点。 / Returns the cp. */
	public Integer getCP() {
		return cp;
	}

	/** 返回 abyss op / Returns the abyss op */
	public Integer getAbyssOp() {
		return abyssOp;
	}

	/** 获取称号。 / Returns the title. */
	public Integer getTitle() {
		return title;
	}

	/** 返回 extend inventory / Returns the extend inventory */
	public Integer getExtendInventory() {
		return extendInventory;
	}

	/** 返回 extend stigma / Returns the extend stigma */
	public Integer getExtendStigma() {
		return extendStigma;
	}
}
