package com.aionemu.gameserver.model.trade;

import com.aionemu.gameserver.model.gameobjects.Item;
import lombok.Getter;
import lombok.Setter;

/**
 * 交换物品，用于交易相关逻辑。
 * Exchange Item for trade logic.
 *
 * @author ATracer
 */
public class ExchangeItem {

	@Getter
	private int itemObjId;
	@Getter
	private long itemCount;
	@Getter
	private int itemDesc;
	@Getter
	@Setter
	private Item item;

	/**
	 * 交易物品与原物品不同时使用。 / Used when exchange item != original item.
	 */
	public ExchangeItem(int itemObjId, long itemCount, Item item) {
		this.itemObjId = itemObjId;
		this.itemCount = itemCount;
		this.item = item;
		this.itemDesc = item.getItemTemplate().getNameId();
	}

	/**
	 * @param countToAdd
	 */
	public void addCount(long countToAdd) {
		this.itemCount += countToAdd;
		this.item.setItemCount(itemCount);
	}

}
