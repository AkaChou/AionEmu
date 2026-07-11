package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 未命名操作码 0xFD 的服务端占位包，向客户端写入一组固定常量字段。
 * Placeholder server packet for opcode 0xFD, writing a set of fixed constant fields to the client.
 *
 * @author wanke
 */
public class SM_0xFD extends AionServerPacket {

	/**
	 * 构造默认占位包实例。
	 * Creates a default placeholder packet instance.
	 */
	public SM_0xFD() {
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeC(2);
		writeH(1);
		writeD(281602);
		writeC(0);
		writeD(1586932);
	}
}
