package com.aionemu.gameserver.model.ingameshop;

import lombok.Getter;
import lombok.Setter;

/**
 * 游戏内商城，用于 ingameshop 相关逻辑。
 * In Game Shop for ingameshop logic.
 */

@Getter
@Setter
public class InGameShop {

	private byte subCategory;
	private byte category = 2;

}
