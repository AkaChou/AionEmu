package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 向客户端显示或隐藏指定增益图标的服务端包。
 * Server packet that shows or hides a buff icon on the client.
 */
@AllArgsConstructor
public class SM_ICON_INFO extends AionServerPacket {
	private final int buffId;
	private final boolean display;

	protected void writeImpl(AionConnection con) {
		writeD(1);
		writeD(buffId);
		writeC(display ? 1 : 0);
	}
}
