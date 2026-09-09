package com.aionemu.gameserver.model.dorinerk_wardrobe;

import lombok.Getter;
import lombok.AllArgsConstructor;

/**
 * 衣橱条目，用于多里纳克衣橱相关逻辑。
 * Wardrobe Entry for dorinerk wardrobe logic.
 *
 * @author Ranastic
 */
@AllArgsConstructor
public class WardrobeEntry {

	@Getter
	private final int itemId;
	@Getter
	private final int slot;
	private final int reskin_count;

	/** 返回重塑次数。 / Returns the reskin count. */
	public int getReskinCount() {
		return reskin_count;
	}
}
