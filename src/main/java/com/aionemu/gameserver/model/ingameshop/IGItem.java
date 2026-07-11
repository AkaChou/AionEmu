package com.aionemu.gameserver.model.ingameshop;

/**
 * 商城物品，用于 ingameshop 相关逻辑。
 * IG Item for ingameshop logic.
 *
 * @author xTz
 */
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

	/** 返回对象 ID / Returns the object id */
	public int getObjectId() {
		return objectId;
	}

	/** 返回物品 ID / Returns the item id */
	public int getItemId() {
		return itemId;
	}

	/** 获取物品计数。 / Returns the item count. */
	public long getItemCount() {
		return itemCount;
	}

	/** 获取物品价格。 / Returns the item price. */
	public long getItemPrice() {
		return itemPrice;
	}

	/** 获取分类。 / Returns the category. */
	public byte getCategory() {
		return category;
	}

	/** 返回 sub category / Returns the sub category */
	public byte getSubCategory() {
		return subCategory;
	}

	/** 获取列表。 / Returns the list. */
	public int getList() {
		return list;
	}

	/** 返回销量排名 / Returns the sales ranking*/
	public int getSalesRanking() {
		return salesRanking;
	}

	/** 获取物品类型。 / Returns the item type. */
	public byte getItemType() {
		return itemType;
	}

	/** 返回 gift / Returns the gift */
	public byte getGift() {
		return gift;
	}

	/** 获取物品描述。 / Returns the item description. */
	public String getItemDescription() {
		return itemDescription;
	}

	/** 获取称号描述。 / Returns the title description. */
	public String getTitleDescription() {
		return titleDescription;
	}

	/** Increase sales / Increase sales */
	public void increaseSales() {
		salesRanking++;
	}
}
