package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 向客户端同步目标军团头衔/职位信息的服务端包。
 * Server packet that synchronizes a target's legion title/rank info to the client.
 *
 * @author sweetkr
 */
@AllArgsConstructor
public class SM_LEGION_UPDATE_TITLE extends AionServerPacket {

	private final int objectId;
	private final int legionId;
	private final String legionName;
	private final int rank;

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(objectId);
		writeD(legionId);
		writeS(legionName);
		writeC(rank); // 0: commander(?), 1: centurion, 2: soldier
	}
}
