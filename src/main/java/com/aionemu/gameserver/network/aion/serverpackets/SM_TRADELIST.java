package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.lifecycle.GameRuntimeServices;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.limiteditems.LimitedItem;
import com.aionemu.gameserver.model.limiteditems.LimitedTradeNpc;
import com.aionemu.gameserver.model.templates.tradelist.TradeListTemplate;
import com.aionemu.gameserver.model.templates.tradelist.TradeListTemplate.TradeTab;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.services.LimitedItemTradeService;

/**
 * 打开 NPC 普通商店交易列表的服务端包（含限购信息）。
 * Server packet that opens an NPC's normal trade list (including limited-item info).
 *
 * @author Dr.Nism
 */
public class SM_TRADELIST extends AionServerPacket {

	private Integer playerObj;
	private int npcObj;
	private int npcId;
	private TradeListTemplate tlist;
	private int buyPriceModifier;

	/**
	 * 玩家 / player
	 * trade NPC
	 * @param tlist            交易列表模板 / trade list template
	 * @param buyPriceModifier 买入价格修正 / buy price modifier
	 */
	public SM_TRADELIST(Player player, Npc npc, TradeListTemplate tlist, int buyPriceModifier) {
		playerObj = player.getObjectId();
		this.npcObj = npc.getObjectId();
		npcId = npc.getNpcId();
		this.tlist = tlist;
		this.buyPriceModifier = buyPriceModifier;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		if ((tlist != null) && (tlist.getNpcId() != 0) && (tlist.getCount() != 0)) {
			writeD(npcObj);
			writeC(tlist.getTradeNpcType().index());
			writeD(buyPriceModifier);
			writeD(0x64); // 4.7
			writeC(1); // 4.7
			writeC(1); // 4.7
			writeH(tlist.getCount());
			for (TradeTab tradeTabl : tlist.getTradeTablist()) {
				writeD(tradeTabl.getId());
				Player activePlayer = con.getActivePlayer();
				if (activePlayer.isGM()) {
					// PacketSendUtility.sendMessage(activePlayer, "<Tradelist Id> + " +
					// tradeTabl.getId());
				}
			}
			int i = 0;
			LimitedTradeNpc limitedTradeNpc = null;
			if (GameRuntimeServices.limitedItemTradeService().isLimitedTradeNpc(npcId)) {
				limitedTradeNpc = GameRuntimeServices.limitedItemTradeService().getLimitedTradeNpc(npcId);
				i = limitedTradeNpc.getLimitedItems().size();
			}
			writeH(i);
			if (limitedTradeNpc != null) {
				for (LimitedItem limitedItem : limitedTradeNpc.getLimitedItems()) {
					writeD(limitedItem.getItemId());
					writeH(limitedItem.getBuyCount().get(playerObj) == null ? 0
							: limitedItem.getBuyCount().get(playerObj));
					writeH(limitedItem.getSellLimit());
				}
			}
		}
	}
}
