package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.lifecycle.GameRuntimeServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.services.BrokerService;

/**
 * 请求本人交易行已上架列表的客户端包。
 * Client packet requesting the player's registered broker items.
 */
public class CM_BROKER_REGISTERED_LIST extends AionClientPacket {
	@SuppressWarnings("unused")
	private int npcId;

	public CM_BROKER_REGISTERED_LIST(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		this.npcId = readD();
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		GameRuntimeServices.brokerService().showRegisteredItems(player);
	}
}
