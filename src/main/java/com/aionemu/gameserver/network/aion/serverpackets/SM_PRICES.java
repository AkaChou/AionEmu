package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.services.trade.PricesService;

/**
 * 向客户端同步当前税率与物价比率（受势力影响）。
 * Server packet synchronizing current tax and price rates (influence-based) to the client.
 *
 * @author xavier, Sarynth, Wakizashi
 */
public class SM_PRICES extends AionServerPacket {

	@Override
	protected void writeImpl(AionConnection con) {
		writeC(PricesService.getGlobalPrices(con.getActivePlayer().getRace())); // Display Buying Price
		writeC(PricesService.getGlobalPricesModifier()); // Buying Modified Price %
		writeC(PricesService.getTaxes(con.getActivePlayer().getRace())); // Tax = -100 + C %
	}
}
