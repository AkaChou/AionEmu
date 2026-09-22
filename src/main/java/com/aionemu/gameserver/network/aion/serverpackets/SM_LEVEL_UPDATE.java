package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 向客户端同步目标等级变化的服务端包。
 * Server packet that synchronizes a target's level change to the client.
 * @author ATracer
 */
@AllArgsConstructor
public class SM_LEVEL_UPDATE extends AionServerPacket {

	private final int targetObjectId;
	private final int effect;
	private final int level;

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(targetObjectId);
		writeH(effect); // 未知 / unk
		writeH(level);
		writeH(0x00); // 未知 / unk
	}
}
