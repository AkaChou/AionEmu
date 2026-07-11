package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端发送实例地图与实例 ID 计数信息的服务端包。
 * Server packet that sends instance map and instance-id count info to the client.
 */
public class SM_INSTANCE_COUNT_INFO extends AionServerPacket {

	private int mapId;
	private int instanceId;

	/**
	 * 使用地图 ID 与实例 ID 构造计数信息包。
	 * Creates a count-info packet for the given map id and instance id.
	 *
	 * map id
	 * instance id
	 */
	public SM_INSTANCE_COUNT_INFO(int mapId, int instanceId) {
		this.mapId = mapId;
		this.instanceId = instanceId;
	}

	protected void writeImpl(AionConnection con) {
		writeD(mapId);
		writeD(instanceId);
		writeD(1);
	}
}
