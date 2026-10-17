package com.aionemu.gameserver.model.templates.luna;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * Luna 消费奖励模板（静态数据/XML）。
 * XML template.
 * @author Ranastic
 */
@Getter
@XmlType(name = "luna_consume_reward")
@XmlAccessorType(XmlAccessType.NONE)
public class LunaConsumeRewardsTemplate {
	/** 返回 ID / Returns the id */
	@XmlAttribute(name = "id")
	protected int id;
	/** 获取名称。 / Returns the name. */
	@XmlAttribute(name = "name")
	protected String name;
	@XmlAttribute(name = "luna_sum_count")
	protected int luna_sum_count;
	@XmlAttribute(name = "gacha_cost")
	protected int gacha_cost;
	@XmlAttribute(name = "create_1")
	protected int create_1;
	@XmlAttribute(name = "num_1")
	protected int num_1;

	/** 返回累计消费次数 / Returns the sum count */
	public int getSumCount() {
		return luna_sum_count;
	}

	/** 返回抽奖消耗 / Returns the gacha cost */
	public int getGachaCost() {
		return gacha_cost;
	}

	/** 返回创建物品 ID / Returns the create item id */
	public int getCreateItemId() {
		return create_1;
	}

	/** 返回创建物品数量 / Returns the create item count */
	public int getCreateItemCount() {
		return num_1;
	}
}
