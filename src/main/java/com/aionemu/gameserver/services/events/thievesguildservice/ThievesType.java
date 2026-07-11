package com.aionemu.gameserver.services.events.thievesguildservice;

/**
 * 盗贼类型枚举，区分盗贼公会相关业务类型。
 * Thieves type enum classifying thieves-guild related business types.
 *
 * @author Rinzler (Encom)
 */

public enum ThievesType {

	NONE(0), // Нет
	BRONZE(1), SILVER(2), GOLD(3), PLATINUM(4), MITHRIL(5), SERAMIUM(6);

	private int id;

	private ThievesType(int id) {
		this.id = id;
	}

	/**
	 * getId 方法。
	 * getId method.
	 * result
	 */
	public int getId() {
		return id;
	}

	/**
	 * getThievesType 方法。
	 * getThievesType method.
	 *
	 * @param id ID / id
	 * result
	 */
	public static ThievesType getThievesType(int id) {
		for (ThievesType type : values()) {
			if (id == type.getId()) {
				return type;
			}
		}
		return ThievesType.NONE;
	}
}