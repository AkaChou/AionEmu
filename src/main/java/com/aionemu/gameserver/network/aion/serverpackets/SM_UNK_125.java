package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 未知 opcode 0x125 的占位服务端包。
 * Placeholder server packet for unknown opcode 0x125.
 *
 * @author wanke
 */
public class SM_UNK_125 extends AionServerPacket {
	@Override
	protected void writeImpl(AionConnection con) {
		writeH(1131);
		writeC(1);
	}
}
