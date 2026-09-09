package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.NoArgsConstructor;

/**
 * 未命名操作码 0x125 的服务端占位包，向客户端写入固定短整型与字节标记。
 * Placeholder server packet for opcode 0x125, writing fixed short and byte markers to the client.
 *
 * @author wanke
 */
@NoArgsConstructor
public class SM_0x125 extends AionServerPacket {

	@Override
	protected void writeImpl(AionConnection con) {
		writeH(1);
		writeC(1);
	}
}
