package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 向客户端发送军团自定义徽章二进制数据块的服务端包。
 * Server packet that sends a binary data chunk of a custom legion emblem to the client.
 * @author cura
 */
@AllArgsConstructor
public class SM_LEGION_SEND_EMBLEM_DATA extends AionServerPacket {

	private final int size;
	private final byte[] data;

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(size);
		writeB(data);
	}
}
