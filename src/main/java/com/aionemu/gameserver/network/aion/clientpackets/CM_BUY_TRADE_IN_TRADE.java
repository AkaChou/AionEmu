package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.services.TradeService;

/**
 * 以物易物（Trade-in）购买的客户端包。
 * Client packet to buy via trade-in exchange.
 */
public class CM_BUY_TRADE_IN_TRADE extends AionClientPacket {
	private int sellerObjId;
	private int BuyMask;
	private int itemId;
	private int count;
	private int TradeinListCount;
	private int TradeinItemObjectId1;
	private int TradeinItemObjectId2;
	private int TradeinItemObjectId3;

	public CM_BUY_TRADE_IN_TRADE(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		sellerObjId = readD();
		BuyMask = readC();
		itemId = readD();
		count = readD();
		TradeinListCount = readH();
		switch (TradeinListCount) {
		case 1:
			TradeinItemObjectId1 = readD();
			break;
		case 2:
			TradeinItemObjectId1 = readD();
			TradeinItemObjectId2 = readD();
			break;
		case 3:
			TradeinItemObjectId1 = readD();
			TradeinItemObjectId2 = readD();
			TradeinItemObjectId3 = readD();
			break;
		}
	}

	@Override
	protected void runImpl() {
		Player player = this.getConnection().getActivePlayer();
		if (count < 1) {
			return;
		}
		TradeService.performBuyFromTradeInTrade(player, sellerObjId, itemId, count, TradeinListCount,
				TradeinItemObjectId1, TradeinItemObjectId2, TradeinItemObjectId3);
	}
}
