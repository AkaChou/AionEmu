package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.lifecycle.GameServerNetworkServices;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.services.TradeService;

/**
 * 客户端出售过期/终止物品请求包。
 * Client packet for selling terminated (expired) items.
 */
public class CM_SELL_TERMINATED_ITEMS extends AionClientPacket {
	private int[] itemObjectIds;

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_SELL_TERMINATED_ITEMS(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		GameServerNetworkServices.packetLoggerService().logPacketCM(this.getPacketName());
		int size = readH();
		itemObjectIds = new int[size];
		for (int i = 0; i < size; i++) {
			itemObjectIds[i] = readD();
		}
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();

		if (player == null) {
			return;
		}
		for (int itemObjectId : itemObjectIds) {
			TradeService.terminatedItemToShop(player, itemObjectId);
		}
	}
}
