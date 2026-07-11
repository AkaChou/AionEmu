package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;

/**
 * 客户端举报聊天刷屏/禁言请求包。
 * Client packet for reporting a player for chat spam (silence report).
 *
 * @author Ranastic (Encom)
 */
public class CM_SILENCE_REPORT extends AionClientPacket {
	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_SILENCE_REPORT(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		readS();
	}

	@Override
	protected void runImpl() {
	}
}
