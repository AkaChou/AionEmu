package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 未知 opcode 0x104 的占位服务端包。
 * Placeholder server packet for unknown opcode 0x104.
 *
 * @author wanke
 */
public class SM_UNK_104 extends AionServerPacket {
	@Override
	protected void writeImpl(AionConnection con) {
		writeH(777);
		writeH(0);
		writeD(0);
	}
}
