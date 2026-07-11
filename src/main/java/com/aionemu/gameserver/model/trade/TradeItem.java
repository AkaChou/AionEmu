package com.aionemu.gameserver.model.trade;

import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import lombok.Getter;
import lombok.Setter;

/**
 * 交易物品模型。
 * Trade Item model.
 *
 * @author ATracer
 */
@Getter
public class TradeItem {

	private int itemId;
	private long count;
	@Setter
	private ItemTemplate itemTemplate;

	public TradeItem(int itemId, long count) {
		super();
		this.itemId = itemId;
		this.count = count;
	}

	/**
	 * 将 decrease 当前数量。
	 * This method will decrease the current count
	 */
	public void decreaseCount(long decreaseCount) {
		if (decreaseCount > 0 && decreaseCount <= count) {
			count -= decreaseCount;
		}
	}
}
