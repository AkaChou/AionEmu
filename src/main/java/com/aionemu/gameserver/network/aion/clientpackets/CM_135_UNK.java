package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection;

/**
 * 未知客户端包（操作码 0x135），当前无处理逻辑。
 * Unknown client packet (opcode 0x135); currently a no-op.
 *
 * @author wanke
 */
public class CM_135_UNK extends AionClientPacket {
	public CM_135_UNK(int opcode, AionConnection.State state, AionConnection.State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
	}

	@Override
	protected void runImpl() {
	}
}
