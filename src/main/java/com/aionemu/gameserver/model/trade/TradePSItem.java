package com.aionemu.gameserver.model.trade;

import lombok.Getter;
import lombok.Setter;

/**
 * 交易 PS 物品模型。
 * Trade PS Item model.
 * @author Simple
 */
@Getter
@Setter
public class TradePSItem extends TradeItem {

	private int itemObjId;
	private long price;

	public TradePSItem(int itemObjId, int itemId, long count, long price) {
		super(itemId, count);
		this.setPrice(price);
		this.setItemObjId(itemObjId);
	}

}
