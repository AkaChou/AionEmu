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
	NONE(0),
	/** 打开 / Opened. */
	OPENED(1 << 0),
	/** 可点击 / Clickable. */
	CLICKABLE(1 << 1),
	/** 可关闭 / Closeable. */
	CLOSEABLE(1 << 2),
	/** 单向 / One-way. */
	ONEWAY(1 << 3);

	private StaticDoorState(int flag) {
		this.flag = flag;
	}

	private int flag;

	/** 返回标志 / Returns the flag*/
	public int getFlag() {
		return flag;
	}

	/** 按标志位设置状态 / Sets the states */
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

	/** 返回标志位 / Returns the flags */
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
