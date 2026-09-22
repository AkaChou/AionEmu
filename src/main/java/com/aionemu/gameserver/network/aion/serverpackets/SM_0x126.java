package com.aionemu.gameserver.network.aion.serverpackets;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 未命名操作码 0x126 的服务端包，向客户端同步一个未知短整型值。
 * Server packet for opcode 0x126 that synchronizes an unknown short value to the client.
 * @author Ranastic (Encom)
 */
@Slf4j

@AllArgsConstructor
public class SM_0x126 extends AionServerPacket {

	private final int unk;

	@Override
	protected void writeImpl(AionConnection con) {
		writeH(unk);
		writeD(0);
	}
}
