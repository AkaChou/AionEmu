package com.aionemu.gameserver.model.templates.housing;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * Use 物品动作模板（静态数据/XML）。
 * XML template.
 * @author Rolandas
 */
@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "UseItemAction")
public class UseItemAction {

	/** 返回 final reward id / Returns the final reward id */
	@XmlAttribute(name = "final_reward_id")
	protected Integer finalRewardId;

	/** 返回 reward id / Returns the reward id */
	@XmlAttribute(name = "reward_id")
	protected Integer rewardId;

	/** 返回移除数量 / Returns the remove count. */
	@XmlAttribute(name = "remove_count")
	protected Integer removeCount;

	/** 返回检查类型 / Returns the check type. */
	@XmlAttribute(name = "check_type")
	protected Integer checkType;
}
