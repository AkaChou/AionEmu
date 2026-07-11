package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.lifecycle.GameServerNetworkServices;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionConnection.State;

/**
 * 客户端向登录服快速重连鉴权请求包。
 * Client packet requesting fast reconnection authentication to the LoginServer.
 *
 * @author -Nemesiss-
 */
public class CM_RECONNECT_AUTH extends AionClientPacket {

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_RECONNECT_AUTH(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void readImpl() {
		// 空 / empty
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void runImpl() {
		AionConnection client = getConnection();
		GameServerNetworkServices.loginServer().requestAuthReconnection(client);
	}
}
