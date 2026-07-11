package com.aionemu.gameserver.model.templates.housing;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 限制类型枚举。
 * Limit Type enumeration.
 */

@XmlType(name = "LimitType")
@XmlEnum
public enum LimitType {
	/** 无 / None. */
	NONE(0, new int[] { 0, 0, 0, 0, 0 }, new int[] { 0, 0, 0, 0, 0 }),
	/** 所有者 Pot / Owner Pot */
	OWNER_POT(1, new int[] { 8, 6, 4, 3, 8 }, new int[] { 0, 0, 0, 0, 4 }),
	/** Visitor Pot / Visitor Pot */
	VISITOR_POT(2, new int[] { 9, 7, 5, 2, 8 }, new int[] { 0, 0, 0, 0, 4 }),
	/** 仓库。 / Storage. */
	STORAGE(3, new int[] { 7, 6, 5, 4, 8 }, new int[] { 0, 0, 0, 0, 4 }),
	/** 花盆 / Pot. */
	POT(4, new int[] { 7, 6, 5, 4, 3 }, new int[] { 7, 6, 5, 4, 1 }),
	/** 烹饪 / Cooking. */
	COOKING(5, new int[] { 2, 2, 2, 2, 2 }, new int[] { 2, 2, 2, 2, 2 }),
	/** 画作 / Picture. */
	PICTURE(6, new int[] { 1, 1, 1, 1, 1 }, new int[] { 1, 1, 1, 1, 0 }),
	/** 点唱机 / Jukebox. */
	JUKEBOX(7, new int[] { 1, 1, 1, 1, 1 }, new int[] { 1, 1, 1, 1, 0 });

	int id;
	int[] personalLimits;
	int[] trialLimits;

	private LimitType(int id, int[] maxPersonalLimits, int[] maxTrialLimits) {
		this.id = id;
		this.personalLimits = maxPersonalLimits;
		this.trialLimits = maxTrialLimits;
	}

	/** 值。 / Value. */
	public String value() {
		return name();
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 返回 object place limit / Returns the object place limit */
	public int getObjectPlaceLimit(HouseType houseType) {
		return personalLimits[houseType.getLimitTypeIndex()];
	}

	/** 返回 trial object place limit / Returns the trial object place limit */
	public int getTrialObjectPlaceLimit(HouseType houseType) {
		return trialLimits[houseType.getLimitTypeIndex()];
	}

	/** 值 / From Value*/
	public static LimitType fromValue(String value) {
		return valueOf(value);
	}
}
