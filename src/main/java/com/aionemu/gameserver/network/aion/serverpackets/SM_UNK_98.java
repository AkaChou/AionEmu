package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 未知 opcode 0x98 的占位服务端包。
 * Placeholder server packet for unknown opcode 0x98.
 *
 * @author wanke
 */
public class SM_UNK_98 extends AionServerPacket {
	@Override
	protected void writeImpl(AionConnection con) {
		writeC(1);
		writeD(0);
		writeD(0);
		writeD(0);
		writeD(0);
	}
}
