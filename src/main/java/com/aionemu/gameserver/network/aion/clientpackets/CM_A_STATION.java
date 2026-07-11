package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.services.AStationService;

/**
 * 跨服 A-Station 传送（前往或返回）的客户端包。
 * Client packet for A-Station transfer (move there or back).
 */
public class CM_A_STATION extends AionClientPacket {
	private int action;

	public CM_A_STATION(int opcode, State state, State... states) {
		super(opcode, state, states);
	}

	@Override
	protected void readImpl() {
		action = readH();
		readH();
		readD();
		readD();
		readD();
	}

	@Override
	protected void runImpl() {
		Player requested = getConnection().getActivePlayer();
		switch (action) {
		case 1:
			GameFeatureServices.aStationService().handleMoveThere(requested);
			break;
		case 2:
			GameFeatureServices.aStationService().handleMoveBack(requested);
			break;
		}
	}
}
