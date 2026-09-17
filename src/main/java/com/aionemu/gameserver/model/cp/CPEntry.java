package com.aionemu.gameserver.model.cp;

import lombok.Getter;
import lombok.AllArgsConstructor;

/**
 * 创造点条目。
 * CP Entry model.
 */
@Getter
@AllArgsConstructor
public class CPEntry {

	/** 获取槽位。 / Returns the slot. */
	private final int slot;
	/** 获取点。 / Returns the point. */
	private final int point;
}
