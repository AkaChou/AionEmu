package com.aionemu.gameserver.model.cp;

/**
 * 创造点条目。
 * CP Entry model.
 */
public class CPEntry {

	private int slot;
	private int point;

	public CPEntry(int slot, int point) {
		this.slot = slot;
		this.point = point;
	}

	/** 获取槽位。 / Returns the slot. */
	public int getSlot() {
		return slot;
	}

	/** 获取点。 / Returns the point. */
	public int getPoint() {
		return point;
	}
}
