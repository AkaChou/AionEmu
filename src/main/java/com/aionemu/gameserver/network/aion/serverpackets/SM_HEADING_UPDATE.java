package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 通知客户端实体朝向更新的服务端包。
 * Server packet that notifies the client of an entity heading update.
 *
 * @author Nemesiss
 */
@AllArgsConstructor
public class SM_HEADING_UPDATE extends AionServerPacket {
	private final int objectId;
	private final byte heading;

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(objectId);
		writeC(heading);
	}
}
