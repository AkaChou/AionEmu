package com.aionemu.gameserver.network.aion.clientpackets;

import lombok.extern.slf4j.Slf4j;

import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EVERGALE_CANYON;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 请求永风峡谷相关信息的客户端包。
 * Client packet requesting Evergale Canyon related information.
 */
@Slf4j
public class CM_EVERGALE_CANYON extends AionClientPacket {

	public int action;

	/**
	 * 构造客户端包实例。
	 * Constructs a new client packet instance.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_EVERGALE_CANYON(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		action = readH();
		switch (action) {
		case 0:
			readD();
			readD();
			readD();
			readD();
			break;
		}
	}

	@Override
	protected void runImpl() {
		switch (action) {
		case 0:
			PacketSendUtility.sendPacket(getConnection().getActivePlayer(), new SM_EVERGALE_CANYON(0));
			break;
		}
	}
}
