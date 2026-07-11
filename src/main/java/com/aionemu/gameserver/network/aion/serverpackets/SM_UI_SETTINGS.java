package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 同步客户端 UI 设置二进制数据的服务端包。
 * Server packet that syncs client UI settings binary data.
 */
public class SM_UI_SETTINGS extends AionServerPacket {
	private byte[] data;
	private int type;

	/**
	 * @param data UI 设置字节数据 / UI settings byte data
	 * @param type 设置类型 / settings type
	 */
	public SM_UI_SETTINGS(byte[] data, int type) {
		this.data = data;
		this.type = type;
	}

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
