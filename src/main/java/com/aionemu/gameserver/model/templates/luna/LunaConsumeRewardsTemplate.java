package com.aionemu.gameserver.model.templates.luna;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 月华 Consume 奖励模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author Ranastic
 */
@XmlType(name = "luna_consume_reward")
@XmlAccessorType(XmlAccessType.NONE)
public class LunaConsumeRewardsTemplate {
	@XmlAttribute
	protected int id;
	@XmlAttribute
	protected String name;
	@XmlAttribute
	protected int luna_sum_count;
	@XmlAttribute
	protected int gacha_cost;
	@XmlAttribute
	protected int create_1;
	@XmlAttribute
	protected int num_1;

	/** 返回 ID / Returns the id */
	public int getId() {
		return this.id;
	}

	/** 获取名称。 / Returns the name. */
	public String getName() {
		return name;
	}

	/** 返回 sum count / Returns the sum count */
	public int getSumCount() {
		return luna_sum_count;
	}

	/** 返回 gacha cost / Returns the gacha cost */
	public int getGachaCost() {
		return gacha_cost;
	}

	/** 返回创建物品 ID / Returns the create item id */
	public int getCreateItemId() {
		return create_1;
	}

	/** 返回创建物品数量 / Returns the create item count*/
	public int getCreateItemCount() {
		return num_1;
	}
}
