package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端通知欧比斯着陆点等级变化的服务端包。
 * Server packet notifying the client of an Abyss landing location level change.
 *
 * @author wanke
 */
public class SM_ABYSS_LANDING_LEVEL extends AionServerPacket {
	private int id;
	private int level;
	private int newLevel;

	/**
	 * 构造着陆点等级变化通知包。
	 * Creates a landing level-change notification packet.
	 *
	 * @param id 着陆点 ID / landing location id
	 * previous level
	 * new level
	 */
	public SM_ABYSS_LANDING_LEVEL(int id, int level, int newLevel) {
		this.id = id;
		this.level = level;
		this.newLevel = newLevel;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeC(id);
		writeC(level);
		writeC(newLevel);
	}
}
