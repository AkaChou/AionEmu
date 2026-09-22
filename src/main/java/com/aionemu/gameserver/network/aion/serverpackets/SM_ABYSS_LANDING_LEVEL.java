package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 向客户端通知欧比斯着陆点等级变化的服务端包。
 * Server packet notifying the client of an Abyss landing location level change.
 * @author wanke
 */
@AllArgsConstructor
public class SM_ABYSS_LANDING_LEVEL extends AionServerPacket {
	private final int id;
	private final int level;
	private final int newLevel;

	@Override
	protected void writeImpl(AionConnection con) {
		writeC(id);
		writeC(level);
		writeC(newLevel);
	}
}
