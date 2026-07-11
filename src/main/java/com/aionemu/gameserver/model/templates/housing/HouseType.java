package com.aionemu.gameserver.model.templates.housing;

/**
 * 房屋类型枚举。
 * House Type enumeration.
 *
 * @author Rolandas
 */
public enum HouseType {
	/** 地产 / Estate. */
	ESTATE(0, 3, "a"), MANSION(1, 2, "b"), HOUSE(2, 1, "c"), STUDIO(3, 0, "d"), PALACE(4, 4, "s");

	private HouseType(int index, int id, String abbrev) {
		this.abbrev = abbrev;
		this.limitTypeIndex = index;
		this.id = id;
	}

	private String abbrev;
	private int limitTypeIndex;
	private int id;

	/** 返回 abbreviation / Returns the abbreviation */
	public String getAbbreviation() {
		return abbrev;
	}

	/** 返回 limit type index / Returns the limit type index */
	public int getLimitTypeIndex() {
		return limitTypeIndex;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 值。 / Value. */
	public String value() {
		return name();
	}

	/** 值 / From Value*/
	public static HouseType fromValue(String value) {
		return valueOf(value);
	}
}
