package com.aionemu.gameserver.model.templates.item;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Acquisition 类型枚举。
 * Acquisition Type enumeration.
 */

@XmlType(name = "acquisitionType")
@XmlEnum
public enum AcquisitionType {
	/** 欧比斯点数。 / Ap. */
	AP(0), ABYSS(1), REWARD(2), COUPON(2);

	private int id;

	private AcquisitionType(int id) {
		this.id = id;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}
}
