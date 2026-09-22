package com.aionemu.gameserver.model.ingameshop;

import lombok.Getter;
import lombok.AllArgsConstructor;

/**
 * 游戏商城物品。
 * In-game shop item.
 * @author xTz
 */
@Getter
@AllArgsConstructor
public class IGItem {

	private final int objectId;
	private final int itemId;
	private final long itemCount;
	private final long itemPrice;
	private final byte category;
	private final byte subCategory;
	private final int list;
	private int salesRanking;
	private final byte itemType;
	private final byte gift;
	private final String titleDescription;
	private final String itemDescription;

	/** 销量加一。 / Increase sales by one. */
	public void increaseSales() {
		salesRanking++;
	}
}
