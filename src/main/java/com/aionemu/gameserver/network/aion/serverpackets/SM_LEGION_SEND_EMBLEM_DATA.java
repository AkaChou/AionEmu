package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端发送军团自定义徽章二进制数据块的服务端包。
 * Server packet that sends a binary data chunk of a custom legion emblem to the client.
 *
 * @author cura
 */
public class SM_LEGION_SEND_EMBLEM_DATA extends AionServerPacket {

	private int size;
	private byte[] data;

	/**
	 * 使用数据大小与字节数组构造徽章数据块包。
	 * Creates an emblem data-chunk packet from size and raw bytes.
	 *
	 * @param size 数据大小 / data size
	 * @param data 徽章二进制数据 / emblem binary data
	 */
	public SM_LEGION_SEND_EMBLEM_DATA(int size, byte[] data) {
		this.size = size;
		this.data = data;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(size);
		writeB(data);
	}
}
