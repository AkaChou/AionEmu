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

	private Map<Integer, Integer> buyCounts = new HashMap<>(); // 玩家对象 ID → 购买数量 / player object id → buy count

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
	 * 记录玩家购买数量（仅在该玩家尚无记录时写入）。
	 * Records a player's buy count (only if no entry exists yet).
	 *
	 * @param playerObjectId 玩家对象 ID / the player object id
	 * @param count 购买数量 / the buy count
	 */
	public void setBuyCount(int playerObjectId, int count) {
		buyCounts.putIfAbsent(playerObjectId, count);
	}

	/**
	 * 返回按玩家对象 ID 统计的购买数量映射。
	 * Returns the buy-count map keyed by player object id.
	 *
	 * @return 购买数量映射 / the buy count map
	 */
	public Map<Integer, Integer> getBuyCount() {
		return buyCounts;
	}

	/**
	 * 设置物品 ID。
	 * Sets the item id.
	 *
	 * @param itemId 物品 ID / the item id
	 */
	public void setItem(int itemId) {
		this.itemId = itemId;
	}

	/** 重置为默认销售限额并清空购买记录。 / Resets to the default sell limit and clears buy counts. */
	public void setToDefault() {
		sellLimit = defaultSellLimit;
		buyCounts.clear();
	}

}
