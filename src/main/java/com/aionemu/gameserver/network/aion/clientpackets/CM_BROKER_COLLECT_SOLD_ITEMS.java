package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.lifecycle.GameRuntimeServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.services.BrokerService;

/**
 * 领取交易行已售出物品收益的客户端包。
 * Client packet to collect sold broker item proceeds.
 */
public class CM_BROKER_COLLECT_SOLD_ITEMS extends AionClientPacket {
	@SuppressWarnings("unused")
	private int npcId;

	public CM_BROKER_COLLECT_SOLD_ITEMS(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		npcId = readD();
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		GameRuntimeServices.brokerService().settleAccount(player);
	}
}
