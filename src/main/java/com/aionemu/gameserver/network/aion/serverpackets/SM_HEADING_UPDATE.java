package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 通知客户端实体朝向更新的服务端包。
 * Server packet that notifies the client of an entity heading update.
 *
 * @author Nemesiss
 */
public class SM_HEADING_UPDATE extends AionServerPacket {
	private final int objectId;
	private final byte heading;

	/**
	 * 构造朝向更新包。
	 * Creates a heading update packet.
	 *
	 * entity object id
	 * heading value
	 */
	public SM_HEADING_UPDATE(int objectId, byte heading) {
		this.objectId = objectId;
		this.heading = heading;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(objectId);
		writeC(heading);
	}
}
