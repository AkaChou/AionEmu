package com.aionemu.gameserver.services.events.thievesguildservice;

/**
 * 盗贼类型枚举，区分盗贼公会相关业务类型。
 * Thieves type enum classifying thieves-guild related business types.
 *
 * @author Rinzler (Encom)
 */

public enum ThievesType {

	NONE(0), // 无 / None
	BRONZE(1), // 青铜 / Bronze
	SILVER(2), // 白银 / Silver
	GOLD(3), // 黄金 / Gold
	PLATINUM(4), // 白金 / Platinum
	MITHRIL(5), // 秘银 / Mithril
	SERAMIUM(6); // 塞拉镁 / Seramium

	private int id;

	private ThievesType(int id) {
		this.id = id;
	}

	/**
	 * 返回类型 ID。
	 * Returns the type id.
	 *
	 * @return 类型 ID / type id
	 */
	public int getId() {
		return id;
	}

	/**
	 * 按 ID 返回盗贼类型，未匹配返回 {@link #NONE}。
	 * Returns the thieves type by id, {@link #NONE} if unmatched.
	 *
	 * @param id ID / id
	 * @return 匹配的盗贼类型 / matching thieves type
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