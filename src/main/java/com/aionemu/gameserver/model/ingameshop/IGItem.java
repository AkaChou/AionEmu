package com.aionemu.gameserver.model.ingameshop;

import lombok.Getter;

/**
 * 游戏商城物品。
 * In-game shop item.
 *
 * @author xTz
 */
@Getter
public class IGItem {

	private int objectId;
	private int itemId;
	private long itemCount;
	private long itemPrice;
	private byte category;
	private byte subCategory;
	private int list;
	private int salesRanking;
	private byte itemType;
	private byte gift;
	private String titleDescription;
	private String itemDescription;

	public IGItem(int objectId, int itemId, long itemCount, long itemPrice, byte category, byte subCategory, int list,
			int salesRanking, byte itemType, byte gift, String titleDescription, String itemDescription) {
		this.objectId = objectId;
		this.itemId = itemId;
		this.itemCount = itemCount;
		this.itemPrice = itemPrice;
		this.category = category;
		this.subCategory = subCategory;
		this.list = list;
		this.salesRanking = salesRanking;
		this.itemType = itemType;
		this.gift = gift;
		this.titleDescription = titleDescription;
		this.itemDescription = itemDescription;
	}

	/** 销量加一。 / Increase sales by one. */
	public void increaseSales() {
		salesRanking++;
	}
}
