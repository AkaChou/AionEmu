package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.templates.tradelist.TradeListTemplate;
import com.aionemu.gameserver.model.templates.tradelist.TradeListTemplate.TradeTab;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 打开 NPC 以物易物（Trade-In）列表的服务端包。
 * Server packet that opens an NPC's trade-in list.
 *
 * @author Rinzler (30.03.2014)
 */
public class SM_TRADE_IN_LIST extends AionServerPacket {
	private Npc npc;
	private TradeListTemplate tlist;
	private int buyPriceModifier;

	/**
	 * trade NPC
	 * @param tlist            以物易物列表模板 / trade-in list template
	 * price modifier
	 */
	public SM_TRADE_IN_LIST(Npc npc, TradeListTemplate tlist, int buyPriceModifier) {
		this.npc = npc;
		this.tlist = tlist;
		this.buyPriceModifier = buyPriceModifier;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		if ((tlist != null) && (tlist.getNpcId() != 0) && (tlist.getCount() != 0)) {
			writeD(npc.getObjectId());
			writeC(tlist.getTradeNpcType().index());
			writeD(buyPriceModifier);
			writeD(0x64); // 4.7
			writeH(tlist.getCount());
			for (TradeTab tradeTabl : tlist.getTradeTablist()) {
				writeD(tradeTabl.getId());
			}
		}
	}
}
