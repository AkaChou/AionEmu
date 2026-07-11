package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.lifecycle.GameRuntimeServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.services.BrokerService;

/**
 * 取消交易行已上架物品的客户端包。
 * Client packet to cancel a registered broker item.
 */
public class CM_BROKER_CANCEL_REGISTERED extends AionClientPacket {
	@SuppressWarnings("unused")
	private int npcId;
	private int brokerItemId;

	public CM_BROKER_CANCEL_REGISTERED(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		npcId = readD();
		brokerItemId = readD();
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		GameRuntimeServices.brokerService().cancelRegisteredItem(player, brokerItemId);
	}
}
