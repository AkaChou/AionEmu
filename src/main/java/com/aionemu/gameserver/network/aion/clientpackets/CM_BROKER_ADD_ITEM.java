package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.lifecycle.GameRuntimeServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.services.BrokerService;

/**
 * 打开交易行上架物品窗口的客户端包。
 * Client packet to open the broker add-item window.
 *
 * @author wanke
 */
public class CM_BROKER_ADD_ITEM extends AionClientPacket {
	private int objectId;

	public CM_BROKER_ADD_ITEM(int opcode, AionConnection.State state, AionConnection.State... restStates) {
		super(opcode, state, restStates);
	}

	protected void readImpl() {
		this.objectId = readD();
	}

	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		GameRuntimeServices.brokerService().showAddItemWindow(player, objectId);
	}
}
