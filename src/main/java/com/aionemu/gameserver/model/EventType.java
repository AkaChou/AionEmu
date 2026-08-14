package com.aionemu.gameserver.model;

/**
 * 活动类型枚举。
 * Event Type enumeration.
 */

public enum EventType {
	/** 无 / None. */
	NONE(0, ""),
	/** 圣诞节 / Christmas */
	CHRISTMAS(1 << 0, "christmas"),
	/** 万圣节 / Halloween */
	HALLOWEEN(1 << 1, "halloween"),
	/** 情人节 / Valentine */
	VALENTINE(1 << 2, "valentine"),
	/** 布拉克斯咖啡 / Braxcafe. */
	BRAXCAFE(1 << 3, "braxcafe"),
	/** 测试基础 1 / Test Basic 1 */
	TEST_BASIC_1(1 << 4, "test_basic_1"),
	/** 测试基础 2 / Test Basic 2 */
	TEST_BASIC_2(1 << 5, "test_basic_2"),
	/** 测试基础 3 / Test Basic 3 */
	TEST_BASIC_3(1 << 6, "test_basic_3"),
	/** 测试基础 4 / Test Basic 4 */
	TEST_BASIC_4(1 << 7, "test_basic_4");

	private int id;
	private String theme;

	private EventType(int id, String theme) {
		this.id = id;
		this.theme = theme;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 返回主题 / Returns the theme */
	public String getTheme() {
		return theme;
	}

	/** 获取活动类型。 / Returns the event type. */
	public static EventType getEventType(String theme) {
		for (EventType type : values()) {
			if (theme.equals(type.getTheme())) {
				return type;
			}
		}
		return EventType.NONE;
	}
}
