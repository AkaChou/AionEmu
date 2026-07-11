package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 未知 opcode 0x14D 的占位服务端包。
 * Placeholder server packet for unknown opcode 0x14D.
 *
 * @author wanke
 */
public class SM_UNK_14D extends AionServerPacket {
	@Override
	protected void writeImpl(AionConnection con) {
		writeC(2);
	}
}
