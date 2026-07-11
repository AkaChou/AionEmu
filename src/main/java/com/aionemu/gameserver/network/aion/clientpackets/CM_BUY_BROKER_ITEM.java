package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.lifecycle.GameRuntimeServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.services.BrokerService;

/**
 * 购买交易行物品的客户端包。
 * Client packet to buy an item from the broker.
 */
public class CM_BUY_BROKER_ITEM extends AionClientPacket {
	@SuppressWarnings("unused")
	private int brokerId;
	private int itemUniqueId;
	private long itemCount;

	public CM_BUY_BROKER_ITEM(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		this.brokerId = readD();
		this.itemUniqueId = readD();
		this.itemCount = readQ();
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		if (itemCount < 1) {
			return;
		}
		GameRuntimeServices.brokerService().buyBrokerItem(player, itemUniqueId, itemCount);
	}
}
