package com.aionemu.gameserver.model.limiteditems;

import java.util.HashMap;
import java.util.Map;

/**
 * 限定物品，用于 limiteditems 相关逻辑。
 * Limited Item for limiteditems logic.
 *
 * @author xTz
 */
public class LimitedItem {

	private int itemId;
	private int sellLimit;
	private int buyLimit;
	private int defaultSellLimit;
	private String salesTime;

	private Map<Integer, Integer> buyCounts = new HashMap<>();

	public LimitedItem() {
	}

	public LimitedItem(int itemId, int sellLimit, int buyLimit, String salesTime) {
		this.itemId = itemId;
		this.sellLimit = sellLimit;
		this.buyLimit = buyLimit;
		this.defaultSellLimit = sellLimit;
		this.salesTime = salesTime;
	}

	/**
	 * return itemId
	 */
	public int getItemId() {
		return itemId;
	}

	/**
	 * @param playerObjectId playerObjectId.
	 * @param count count.
	 */
	public void setBuyCount(int playerObjectId, int count) {
		buyCounts.putIfAbsent(playerObjectId, count);
	}

	/**
	 * return playerListByObject
	 */
	public Map<Integer, Integer> getBuyCount() {
		return buyCounts;
	}

	/**
	 * @param itemId itemId.
	 */
	public void setItem(int itemId) {
		this.itemId = itemId;
	}

	/**
	 * return sellLimit
	 */
	public int getSellLimit() {
		return sellLimit;
	}

	/**
	 * return buyLimit
	 */
	public int getBuyLimit() {
		return buyLimit;
	}

	/** 设置默认 / Sets the to default*/
	public void setToDefault() {
		sellLimit = defaultSellLimit;
		buyCounts.clear();
	}

	/**
	 * @param sellLimit sellLimit.
	 */
	public void setSellLimit(int sellLimit) {
		this.sellLimit = sellLimit;
	}

	/**
	 * return defaultSellLimit
	 */
	public int getDefaultSellLimit() {
		return defaultSellLimit;
	}

	/** 返回销量时间 / Returns the sales time*/
	public String getSalesTime() {
		return salesTime;
	}
}
