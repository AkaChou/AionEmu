package com.aionemu.gameserver.model.templates.staticdoor;

import java.util.EnumSet;

/**
 * 静态 Door 状态枚举。
 * Static Door State enumeration.
 *
 * @author Rolandas
 */
public enum StaticDoorState {
	/** 无 / None. */
	NONE(0), OPENED(1 << 0), CLICKABLE(1 << 1), CLOSEABLE(1 << 2), ONEWAY(1 << 3);

	private StaticDoorState(int flag) {
		this.flag = flag;
	}

	private int flag;

	/** 返回标志 / Returns the flag*/
	public int getFlag() {
		return flag;
	}

	/** 设置 states / Sets the states */
	public static void setStates(int flags, EnumSet<StaticDoorState> state) {
		for (StaticDoorState states : StaticDoorState.values()) {
			if (states == NONE) {
				continue;
			}
			if ((flags & states.flag) == 0) {
				state.remove(states);
			} else {
				state.add(states);
			}
		}
	}

	/** 返回 flags / Returns the flags */
	public static int getFlags(EnumSet<StaticDoorState> doorStates) {
		int result = 0;
		for (StaticDoorState state : StaticDoorState.values()) {
			if (state == NONE) {
				continue;
			}
			if (doorStates.contains(state)) {
				result |= state.flag;
			}
		}
		return result;
	}
}
