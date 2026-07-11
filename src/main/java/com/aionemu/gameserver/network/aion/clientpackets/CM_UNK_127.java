package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_UNK_98;

/**
 * 未知客户端包占位（opcode 127），收到后回复 {@link SM_UNK_98}。
 * Unknown client packet placeholder (opcode 127); responds with {@link SM_UNK_98}.
 *
 * @author Wnkrz
 */
public class CM_UNK_127 extends AionClientPacket {

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_UNK_127(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		// 空 / empty
	}

	@Override
	protected void runImpl() {
		sendPacket(new SM_UNK_98());
	}
}
