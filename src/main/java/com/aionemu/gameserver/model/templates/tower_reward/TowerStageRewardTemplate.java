package com.aionemu.gameserver.model.templates.tower_reward;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * 高塔 Stage 奖励模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TowerStageReward")
public class TowerStageRewardTemplate {
	/** 返回 floor / Returns the floor */
	@Getter
	@XmlAttribute(name = "floor")
	protected int floor;

	/** 获取名称。 / Returns the name. */
	@Getter
	@XmlAttribute(name = "name")
	protected String name;

	/** 返回物品 ID / Returns the item id */
	@Getter
	@XmlAttribute(name = "item_id")
	protected int itemId;

	/** 获取物品计数。 / Returns the item count. */
	@Getter
	@XmlAttribute(name = "item_count")
	protected int itemCount;

	/** 返回物品 ID2 / Returns the item id 2 */
	@Getter
	@XmlAttribute(name = "item_id2")
	protected int itemId2;

	/** 返回物品统计2 / Returns the item count 2 */
	@Getter
	@XmlAttribute(name = "item_count2")
	protected int itemCount2;

	/** 获取欧比斯点数计数。 / Returns the ap count. */
	@Getter
	@XmlAttribute(name = "ap_count")
	protected int apCount;

	/** 返回荣耀点数量 / Returns the gp count */
	@Getter
	@XmlAttribute(name = "gp_count")
	protected int gpCount;

	/** 获取基纳计数。 / Returns the kinah count. */
	@Getter
	@XmlAttribute(name = "kinah_count")
	protected int kinahCount;

	/** 获取经验计数。 / Returns the exp count. */
	@Getter
	@XmlAttribute(name = "exp_count")
	protected int expCount;
}
