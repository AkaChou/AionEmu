package com.aionemu.gameserver.model.limiteditems;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

/**
 * 限定交易 NPC，用于 limiteditems 相关逻辑。
 * Limited Trade Npc for limiteditems logic.
 *
 * @author xTz
 */
public class LimitedTradeNpc {

	/** 获取限定物品。 / Returns the limited items. */
	@Getter
	private final List<LimitedItem> limitedItems;

	public LimitedTradeNpc(List<LimitedItem> limitedItems) {
		this.limitedItems = new ArrayList<>(limitedItems);
	}

	/** 放入限定物品。 / Put limited items. */
	public void putLimitedItems(List<LimitedItem> limitedItems) {
		this.limitedItems.addAll(limitedItems);
	}
}
