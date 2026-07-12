package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.lifecycle.GameRuntimeServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.services.territory.TerritoryService;

/**
 * 客户端石矛要塞（领地）攻城信息请求包。
 * territory status.
 */
public class CM_STONESPEAR_SIEGE extends AionClientPacket {
	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_STONESPEAR_SIEGE(int opcode, AionConnection.State state, AionConnection.State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		readD();
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		if (player.getLegion() != null && player.getLegion().getTerritory().getId() > 0) {
			GameRuntimeServices.territoryService().sendStoneSpearPacket(getConnection().getActivePlayer());
		}
	}
}
