package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.lifecycle.GameHousingServices;

import com.aionemu.gameserver.configs.main.HousingConfig;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.services.HousingBidService;

/**
 * 客户端房屋拍卖出价请求包。
 * Client packet to place a bid on a housing auction listing.
 */
public class CM_PLACE_BID extends AionClientPacket {
	int listIndex = 0;
	long bidOffer = 0;

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_PLACE_BID(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		listIndex = readD();
		bidOffer = readQ();
	}

	@Override
	protected void runImpl() {
		if (HousingConfig.ENABLE_HOUSE_AUCTIONS) {
			Player player = getConnection().getActivePlayer();
			GameHousingServices.housingBidService().placeBid(player, listIndex, bidOffer);
		}
	}
}
