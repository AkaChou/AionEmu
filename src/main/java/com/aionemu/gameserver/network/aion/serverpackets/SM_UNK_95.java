package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 未知 opcode 0x95 的占位服务端包。
 * Placeholder server packet for unknown opcode 0x95.
 *
 * @author wanke
 */
public class SM_UNK_95 extends AionServerPacket {
	@Override
	protected void writeImpl(AionConnection con) {
		writeD(0);
	}
}
