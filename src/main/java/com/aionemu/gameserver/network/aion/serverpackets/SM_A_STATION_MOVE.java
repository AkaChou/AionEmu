package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 通知客户端执行 A-Station（跨服中转站）地图迁移的服务端包。
 * Server packet instructing the client to perform an A-Station (cross-server hub) map move.
 *
 * @author Ranastic
 */
@AllArgsConstructor
public class SM_A_STATION_MOVE extends AionServerPacket {
	private final int currentServerId;
	private final int newServerId;
	private final int mapId;

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(newServerId);
		writeD(currentServerId);
		writeC(0);
		writeD(mapId);
	}
}
