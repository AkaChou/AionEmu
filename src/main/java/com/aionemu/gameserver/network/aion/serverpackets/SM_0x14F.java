package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 未命名操作码 0x14F 的服务端占位包，向客户端写入固定字节标记。
 * Placeholder server packet for opcode 0x14F, writing a fixed byte marker to the client.
 */
public class SM_0x14F extends AionServerPacket {

	/**
	 * 构造默认占位包实例。
	 * Creates a default placeholder packet instance.
	 */
	public SM_0x14F() {
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeC(2);
	}
}
