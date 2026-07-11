package com.aionemu.gameserver.model.ingameshop;

/**
 * 游戏内商城，用于 ingameshop 相关逻辑。
 * In Game Shop for ingameshop logic.
 */

public class InGameShop {

	private byte subCategory;
	private byte category = 2;

	/** 返回 sub category / Returns the sub category */
	public byte getSubCategory() {
		return subCategory;
	}

	/** 设置 sub category / Sets the sub category */
	public void setSubCategory(byte subCategory) {
		this.subCategory = subCategory;
	}

	/** 获取分类。 / Returns the category. */
	public byte getCategory() {
		return category;
	}

	/** 设置分类。 / Sets the category. */
	public void setCategory(byte category) {
		this.category = category;
	}
}
