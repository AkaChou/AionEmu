package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;

/**
 * 未知客户端包占位：读取变长载荷，暂无业务处理。
 * Unknown/unimplemented client packet placeholder; reads a variable payload with no handling.
 */
public class CM_UNK extends AionClientPacket {

	int size;

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_UNK(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		readB(size = readD());
	}

	@Override
	protected void runImpl() {
	}
}
