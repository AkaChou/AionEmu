package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 通知客户端执行 A-Station（跨服中转站）地图迁移的服务端包。
 * Server packet instructing the client to perform an A-Station (cross-server hub) map move.
 *
 * @author Ranastic
 */
public class SM_A_STATION_MOVE extends AionServerPacket {
	private int currentServerId;
	private int newServerId;
	private int mapId;

	/**
	 * @param currentServer 当前服务器 ID / current server id
	 * @param newServerId 目标服务器 ID / destination server id
	 * @param mapId 目标地图 ID / destination map id
	 */
	public SM_A_STATION_MOVE(int currentServer, int newServerId, int mapId) {
		this.currentServerId = currentServer;
		this.newServerId = newServerId;
		this.mapId = mapId;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(newServerId);
		writeD(currentServerId);
		writeC(0);
		writeD(mapId);
	}
}
