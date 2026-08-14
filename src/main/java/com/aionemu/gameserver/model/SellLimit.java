package com.aionemu.gameserver.model;

import java.util.NoSuchElementException;

/**
 * 出售上限枚举。
 * Sell Limit enumeration.
 */

public enum SellLimit {
	/** Limit 1 65 / Limit 1 65 */
	LIMIT_1_65(1, 65, 292000047L),
	/** Limit 66 83 / Limit 66 83 */
	LIMIT_66_83(66, 83, 392000047L);

	private int playerMinLevel;
	private int playerMaxLevel;
	private long limit;

	private SellLimit(int playerMinLevel, int playerMaxLevel, long limit) {
		this.playerMinLevel = playerMinLevel;
		this.playerMaxLevel = playerMaxLevel;
		this.limit = limit;
	}

	/** 获取出售上限。 / Returns the sell limit. */
	public static long getSellLimit(int playerLevel) {
		for (SellLimit sellLimit : values()) {
			if (sellLimit.playerMinLevel <= playerLevel && sellLimit.playerMaxLevel >= playerLevel) {
				return sellLimit.limit;
			}
		}
		throw new NoSuchElementException("Sell limit for player level: " + playerLevel + " was not found");
	}
}
