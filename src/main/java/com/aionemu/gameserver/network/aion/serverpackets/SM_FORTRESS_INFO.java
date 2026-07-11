package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 要塞信息包：同步要塞位置与可否传送状态。
 * Fortress info packet: location id and teleport availability.
 */
public class SM_FORTRESS_INFO extends AionServerPacket {

	private int locationId;
	private boolean teleportStatus;
	private int unk;

	public SM_FORTRESS_INFO(int locationId, boolean teleportStatus) {
		this.locationId = locationId;
		this.teleportStatus = teleportStatus;
	}

	protected void writeImpl(AionConnection con) {
		writeD(1);// 4.3 protocol changed
		writeD(locationId);
		writeD(unk);// 4.3 protocol changed
		writeC(teleportStatus ? 1 : 0);
	}
}
