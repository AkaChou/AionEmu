package com.aionemu.gameserver.network.aion.clientpackets;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;

/**
 * 客户端 Web 会话密钥上报包（调试用）。
 * Client packet reporting a web session key (debug logging only).
 */
@Slf4j
public class CM_S_REP_WEB_SESSIONKEY extends AionClientPacket {


	private int unk;
	private String text;

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_S_REP_WEB_SESSIONKEY(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		unk = readC();
		text = readS();
	}

	@Override
	protected void runImpl() {
		log.debug(text);
	}
}
