package com.aionemu.gameserver.model.ingameshop;

import lombok.Getter;
import lombok.Setter;

/**
 * 游戏内商城分类信息（子分类与分类 ID）。
 * In-game shop category info (sub-category and category id).
 */

@Getter
@Setter
public class InGameShop {

	private byte subCategory;
	private byte category = 2;

}
