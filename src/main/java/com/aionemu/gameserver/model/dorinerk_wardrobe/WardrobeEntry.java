package com.aionemu.gameserver.model.dorinerk_wardrobe;

import lombok.Getter;

/**
 * 衣橱条目，用于多里纳克衣橱相关逻辑。
 * Wardrobe Entry for dorinerk wardrobe logic.
 *
 * @author Ranastic
 */
public class WardrobeEntry {

	@Getter
	private int itemId;
	@Getter
	private int slot;
	private int reskin_count;

	public WardrobeEntry(int itemId, int slot, int reskin_count) {
		this.itemId = itemId;
		this.slot = slot;
		this.reskin_count = reskin_count;
	}

	/** 返回重塑次数。 / Returns the reskin count. */
	public int getReskinCount() {
		return reskin_count;
	}

}
