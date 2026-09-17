package com.aionemu.gameserver.model.ingameshop;

import lombok.Data;

/**
 * 游戏内商城分类信息（子分类与分类 ID）。
 * In-game shop category info (sub-category and category id).
 */

@Data
public class InGameShop {

	private byte subCategory;
	private byte category = 2;

}
