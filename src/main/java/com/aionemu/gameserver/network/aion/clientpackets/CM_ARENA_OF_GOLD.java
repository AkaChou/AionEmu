package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ARENA_OF_GOLD_RANK;

/**
 * 请求黄金竞技场信息的客户端包。
 * Client packet requesting Arena of Gold information.
 *
 * @author wanke
 */
public class CM_ARENA_OF_GOLD extends AionClientPacket {
	public CM_ARENA_OF_GOLD(int opcode, AionConnection.State state, AionConnection.State... restStates) {
		super(opcode, state, restStates);
	}

	int unkD;

	@Override
	protected void readImpl() {
		this.unkD = readD();
	}

	@Override
	protected void runImpl() {
		sendPacket(new SM_ARENA_OF_GOLD_RANK());
	}
}
