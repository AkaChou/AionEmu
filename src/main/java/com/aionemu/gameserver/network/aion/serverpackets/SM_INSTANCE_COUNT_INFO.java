package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 向客户端发送实例地图与实例 ID 计数信息的服务端包。
 * Server packet that sends instance map and instance-id count info to the client.
 */
@AllArgsConstructor
public class SM_INSTANCE_COUNT_INFO extends AionServerPacket {

	private final int mapId;
	private final int instanceId;

	protected void writeImpl(AionConnection con) {
		writeD(mapId);
		writeD(instanceId);
		writeD(1);
	}
}
