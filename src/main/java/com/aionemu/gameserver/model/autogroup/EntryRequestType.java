package com.aionemu.gameserver.model.autogroup;

/**
 * 条目请求类型枚举。
 * Entry Request Type enumeration.
 */

public enum EntryRequestType {
	/**
	 * 入场类型：新小队 / 快速小队 / 队伍 / 特殊用途。
	 * Entry types: new group / fast group / group / special purpose.
	 */
	NEW_GROUP_ENTRY((byte) 0), FAST_GROUP_ENTRY((byte) 1), GROUP_ENTRY((byte) 2), SPECIAL_PURPOSE((byte) 3);

	private byte id;

	private EntryRequestType(byte id) {
		this.id = id;
	}

	/** 返回 ID / Returns the id */
	public byte getId() {
		return id;
	}

	/** 是否新小队入场 / Whether new group entry */
	public boolean isNewGroupEntry() {
		return id == 0;
	}

	/** 是否快速小队入场 / Whether fast group entry */
	public boolean isFastGroupEntry() {
		return id == 1;
	}

	/** 是否为队伍条目。 / Whether group entry. */
	public boolean isGroupEntry() {
		return id == 2;
	}

	/**
	 * @return 是否特殊用途 / Whether special purpose
	 */
	public boolean isSpecialPurpose() {
		return id == 3;
	}

	/** 返回按 ID 的类型 / Returns the type by id */
	public static EntryRequestType getTypeById(byte id) {
		for (EntryRequestType ert : values()) {
			if (ert.getId() == id) {
				return ert;
			}
		}
		return null;
	}
}
