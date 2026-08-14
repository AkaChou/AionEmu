package com.aionemu.gameserver.model.templates.item;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 物品获取类型：欧比斯点数/深渊/奖励/兑换券。
 * Item acquisition type: AP/abyss/reward/coupon.
 */

@XmlType(name = "acquisitionType")
@XmlEnum
public enum AcquisitionType {
	/** 欧比斯点数 / AP */
	AP(0),
	/** 深渊（欧比斯兑换）/ Abyss */
	ABYSS(1),
	/** 奖励 / Reward */
	REWARD(2),
	/** 兑换券 / Coupon */
	COUPON(2);

	private int id;

	private AcquisitionType(int id) {
		this.id = id;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}
}
