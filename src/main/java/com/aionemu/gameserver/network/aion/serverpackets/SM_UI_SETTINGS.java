package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 同步客户端 UI 设置二进制数据的服务端包。
 * Server packet that syncs client UI settings binary data.
 */
@AllArgsConstructor
public class SM_UI_SETTINGS extends AionServerPacket {
	private final byte[] data;
	private final int type;

	@Override
	protected void writeImpl(AionConnection con) {
		writeC(type);
		writeH(0x1C00);
		writeB(data);
		if (0x1C00 > data.length) {
			writeB(new byte[0x1C00 - data.length]);
		}
	}
}
