package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MAY_LOGIN_INTO_GAME;

/**
 * 询问是否允许进入游戏世界的客户端包。
 * Client packet asking whether login into the game world is allowed.
 *
 * @author -Nemesiss-
 */
public class CM_MAY_LOGIN_INTO_GAME extends AionClientPacket {
	/**
	 * 构造该客户端包。
	 * Constructs this client packet.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余合法状态 / additional valid states
	 */
	public CM_MAY_LOGIN_INTO_GAME(int opcode, State state, State... restStates) {
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
		getConnection().sendPacket(new SM_MAY_LOGIN_INTO_GAME());
	}
}
