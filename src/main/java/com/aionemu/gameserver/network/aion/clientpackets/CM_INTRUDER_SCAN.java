package com.aionemu.gameserver.network.aion.clientpackets;

import lombok.extern.slf4j.Slf4j;

import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;

/**
 * 入侵者扫描的客户端包。
 * Client packet for intruder scan.
 *
 * @author Ranastic (Encom)
 */
@Slf4j
public class CM_INTRUDER_SCAN extends AionClientPacket {
	private int value;
	/**
	 * 构造该客户端包。
	 * Constructs this client packet.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余合法状态 / additional valid states
	 */
	public CM_INTRUDER_SCAN(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		this.value = readC();
	}

	@Override
	protected void runImpl() {
	}
}
