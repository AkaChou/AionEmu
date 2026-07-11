package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 未知 opcode 0xA5 的服务端包，写入单字节值。
 * Server packet for unknown opcode 0xA5 that writes a single byte value.
 *
 * @author wanke
 */
public class SM_UNK_A5 extends AionServerPacket {
	private int value;

	/**
	 * @param value 写入客户端的值 / value written to the client
	 */
	public SM_UNK_A5(int value) {
		this.value = value;
	}

	protected void writeImpl(AionConnection con) {
		writeC(value);
		writeH(0);
	}
}
