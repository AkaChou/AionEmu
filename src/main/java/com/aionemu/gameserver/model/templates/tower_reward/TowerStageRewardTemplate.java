package com.aionemu.gameserver.model.templates.tower_reward;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 高塔 Stage 奖励模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TowerStageReward")
public class TowerStageRewardTemplate {
	@XmlAttribute(name = "floor")
	protected int floor;

	@XmlAttribute(name = "name")
	protected String name;

	@XmlAttribute(name = "item_id")
	protected int itemId;

	@XmlAttribute(name = "item_count")
	protected int itemCount;

	@XmlAttribute(name = "item_id2")
	protected int itemId2;

	@XmlAttribute(name = "item_count2")
	protected int itemCount2;

	@XmlAttribute(name = "ap_count")
	protected int apCount;

	@XmlAttribute(name = "gp_count")
	protected int gpCount;

	@XmlAttribute(name = "kinah_count")
	protected int kinahCount;

	@XmlAttribute(name = "exp_count")
	protected int expCount;

	/** 返回 floor / Returns the floor */
	public int getFloor() {
		return this.floor;
	}

	/** 获取名称。 / Returns the name. */
	public String getName() {
		return this.name;
	}

	/** 返回物品 ID / Returns the item id */
	public int getItemId() {
		return this.itemId;
	}

	/** 返回物品 ID2 / Returns the item id 2 */
	public int getItemId2() {
		return this.itemId2;
	}

	/** 获取物品计数。 / Returns the item count. */
	public int getItemCount() {
		return this.itemCount;
	}

	/** 返回物品统计2 / Returns the item count 2 */
	public int getItemCount2() {
		return this.itemCount2;
	}

	/** 获取欧比斯点数计数。 / Returns the ap count. */
	public int getApCount() {
		return this.apCount;
	}

	/** 返回荣耀点数量 / Returns the gp count */
	public int getGpCount() {
		return this.gpCount;
	}

	/** 获取基纳计数。 / Returns the kinah count. */
	public int getKinahCount() {
		return this.kinahCount;
	}

	/** 获取经验计数。 / Returns the exp count. */
	public int getExpCount() {
		return this.expCount;
	}
}
