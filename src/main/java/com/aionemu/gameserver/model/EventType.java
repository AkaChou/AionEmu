package com.aionemu.gameserver.model;

/**
 * 活动类型枚举。
 * Event Type enumeration.
 */

public enum EventType {
	/** 无 / None. */
	NONE(0, ""), CHRISTMAS(1 << 0, "christmas"), HALLOWEEN(1 << 1, "halloween"), VALENTINE(1 << 2, "valentine"),
	/** 布拉克斯咖啡 / Braxcafe. */
	BRAXCAFE(1 << 3, "braxcafe"), TEST_BASIC_1(1 << 4, "test_basic_1"), TEST_BASIC_2(1 << 5, "test_basic_2"),
	/** 测试基础 3 / Test Basic 3 */
	TEST_BASIC_3(1 << 6, "test_basic_3"), TEST_BASIC_4(1 << 7, "test_basic_4");

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
