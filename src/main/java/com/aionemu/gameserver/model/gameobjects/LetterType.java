package com.aionemu.gameserver.model.gameobjects;

/**
 * 信件类型枚举。
 * Letter Type enumeration.
 */

public enum LetterType {
	/** 普通 / Normal. */
	NORMAL(0), EXPRESS(1), BLACKCLOUD(2);

	private int id;

	private LetterType(int id) {
		this.id = id;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 按 ID 返回信件类型 / Returns the letter type by id */
	public static LetterType getLetterTypeById(int id) {
		for (LetterType lt : values()) {
			if (lt.id == id) {
				return lt;
			}
		}
		throw new IllegalArgumentException("Unsupported revive type: " + id);
	}
}
