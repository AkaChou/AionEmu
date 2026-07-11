package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.lifecycle.GameRuntimeServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.services.BrokerService;

/**
 * 开始交易行上架前查询物品均价区间的客户端包。
 * Client packet to start broker registration and query item average price range.
 */
public class CM_BROKER_START_REGISTER extends AionClientPacket {
	private int itemUniqueId;

	public CM_BROKER_START_REGISTER(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		itemUniqueId = readD();
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		GameRuntimeServices.brokerService().CalcItemAveLowHigh(player, itemUniqueId);
	}
}
