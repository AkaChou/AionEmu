package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.services.ProtectorConquerorService;

/**
 * 客户端打开地图时更新保护者/征服者图标请求包。
 * Client packet sent when opening the map to refresh protector/conqueror icons.
 */
public class CM_SHOW_MAP extends AionClientPacket {
	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_SHOW_MAP(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		GameFeatureServices.protectorConquerorService().updateIcons(player);
	}
}
