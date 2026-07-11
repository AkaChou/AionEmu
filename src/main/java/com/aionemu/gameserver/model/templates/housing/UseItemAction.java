package com.aionemu.gameserver.model.templates.housing;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Use 物品动作模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author Rolandas
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "UseItemAction")
public class UseItemAction {

	@XmlAttribute(name = "final_reward_id")
	protected Integer finalRewardId;

	@XmlAttribute(name = "reward_id")
	protected Integer rewardId;

	@XmlAttribute(name = "remove_count")
	protected Integer removeCount;

	@XmlAttribute(name = "check_type")
	protected Integer checkType;

	/** 返回 final reward id / Returns the final reward id */
	public Integer getFinalRewardId() {
		return finalRewardId;
	}

	/** 返回 reward id / Returns the reward id */
	public Integer getRewardId() {
		return rewardId;
	}

	/** 返回移除数量 / Returns the remove count*/
	public Integer getRemoveCount() {
		return removeCount;
	}

	/** 返回检查类型 / Returns the check type*/
	public Integer getCheckType() {
		return checkType;
	}
}
