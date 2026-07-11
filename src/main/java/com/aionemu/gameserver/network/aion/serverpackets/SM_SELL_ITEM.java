package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.templates.tradelist.TradeListTemplate;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端打开商店购买/出售信息窗口（S_STORE_PURCHASE_INFO）。
 * Server packet opening a shop purchase/sell info window (S_STORE_PURCHASE_INFO).
 *
 * @author BeckUp.Media
 */
public class SM_SELL_ITEM extends AionServerPacket
{
	private int targetObjectId;
	private TradeListTemplate buylist;
	private int priceModifier;
	private byte action = 1;
	private byte unk1 = 0;
	private byte unk2 = 0;

	/**
	 * 使用给定参数构造 SM_SELL_ITEM 包。
	 * Normal sell
	 *
	 * target object id
	 * price modifier
	 */
	public SM_SELL_ITEM(int targetObjectId, int priceModifier)
	{
		this.action = 1;
		this.priceModifier = priceModifier;
		this.targetObjectId = targetObjectId;
		this.unk1 = 1;
		this.unk2 = 1;
	}

	/**
	 * 使用给定参数构造 SM_SELL_ITEM 包。
	 * sell ap relics
	 *
	 * target object id
	 * price modifier
	 * @param buyList 购买列表模板 / buy list template
	 */
	public SM_SELL_ITEM(int targetObjectId, int priceModifier, TradeListTemplate buyList)
	{
		this.action = 2;
		this.priceModifier = priceModifier;
		this.targetObjectId = targetObjectId;
		this.buylist = buyList;
		this.unk1 = 0;
		this.unk2 = 1;
	}

	@Override
	protected void writeImpl(AionConnection con)
	{
		writeD(targetObjectId); // npc obj id
		writeC(action); // action
		writeD(priceModifier); // modifier
		writeC(unk1);// 未知 / unk
		writeC(unk2);// 未知 / unk
		if (buylist != null) {
			writeH(buylist.getCount());// buyList size
			for (TradeListTemplate.TradeTab tTab : buylist.getTradeTablist()) {
				writeD(tTab.getId()); // id of tab
			}
		} else {
			writeH(0);// buyList size
		}
	}
}
