package com.aionemu.gameserver.model.house;

/**
 * 房屋 Permissions 枚举。
 * House Permissions enumeration.
 */

public enum HousePermissions {
	/** 未设置 / Not set */
	NOT_SET(0),
	/** 显示房主 / Show owner */
	SHOW_OWNER(1 << 0),
	/** 门对所有人开放 / Door opened to all */
	DOOR_OPENED_ALL(1 << 8),
	/** 门对好友开放 / Door opened to friends */
	DOOR_OPENED_FRIENDS(2 << 8),
	/** 门关闭 / Door closed */
	DOOR_CLOSED(3 << 8);

	private int value;

	private HousePermissions(int value) {
		this.value = value;
	}

	/** 获取数据包值。 / Returns the packet value. */
	public byte getPacketValue() {
		int result = value;
		if (value > 1) {
			result >>= 8;
		}
		return (byte) result;
	}

	/** 是否门打开 / Whether door open*/
	public boolean isDoorOpen() {
		return this == DOOR_OPENED_ALL || this == DOOR_OPENED_FRIENDS;
	}

	/** 返回数据包门状态 / Returns the packet door state*/
	public static HousePermissions getPacketDoorState(int value) {
		value <<= 8;
		for (HousePermissions perm : HousePermissions.values()) {
			if (value == perm.value) {
				return perm;
			}
		}
		return NOT_SET;
	}

	/** 返回门状态 / Returns the door state*/
	public static HousePermissions getDoorState(int value) {
		value &= 0xFF00;
		for (HousePermissions perm : HousePermissions.values()) {
			if (value == perm.value) {
				return perm;
			}
		}
		return NOT_SET;
	}

	/** 设置 door state / Sets the door state */
	public static int setDoorState(int value, HousePermissions doorState) {
		int state = doorState.value & 0xFF00;
		return (value & 0x00FF) | state;
	}

	/** 返回 notice state / Returns the notice state */
	public static HousePermissions getNoticeState(int value) {
		if ((value & SHOW_OWNER.value) == SHOW_OWNER.value) {
			return SHOW_OWNER;
		}
		return NOT_SET;
	}

	/** 设置 notice state / Sets the notice state */
	public static int setNoticeState(int value, HousePermissions noticeState) {
		if (noticeState == NOT_SET) {
			return value & 0xFF00;
		}
		int state = noticeState.value & 0xFF;
		return state | value;
	}
}
