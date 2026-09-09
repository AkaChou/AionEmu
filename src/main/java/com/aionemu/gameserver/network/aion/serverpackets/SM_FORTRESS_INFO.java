package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.RequiredArgsConstructor;

/**
 * 要塞信息包：同步要塞位置与可否传送状态。
 * Fortress info packet: location id and teleport availability.
 */
@RequiredArgsConstructor
public class SM_FORTRESS_INFO extends AionServerPacket {

	private final int locationId;
	private final boolean teleportStatus;
	private int unk;

	protected void writeImpl(AionConnection con) {
		writeD(1);// 4.3 protocol changed
		writeD(locationId);
		writeD(unk);// 4.3 protocol changed
		writeC(teleportStatus ? 1 : 0);
	}
}
