package com.aionemu.gameserver.model.limiteditems;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 * 限定物品，用于 limiteditems 相关逻辑。
 * Limited Item for limiteditems logic.
 *
 * @author xTz
 */
public class LimitedItem {

	@Getter
	private int itemId;
	@Getter
	@Setter
	private int sellLimit;
	@Getter
	private int buyLimit;
	@Getter
	private int defaultSellLimit;
	@Getter
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

	/** 设置默认 / Sets the to default*/
	public void setToDefault() {
		sellLimit = defaultSellLimit;
		buyCounts.clear();
	}

}
