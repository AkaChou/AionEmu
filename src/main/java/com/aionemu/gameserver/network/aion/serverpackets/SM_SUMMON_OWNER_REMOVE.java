package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 通知客户端移除召唤物所有者关联。
 * Server packet notifying the client to remove summon-owner association.
 *
 * @author ATracer
 */
@AllArgsConstructor
public class SM_SUMMON_OWNER_REMOVE extends AionServerPacket {

	private final int summonObjId;

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(summonObjId);
	}
}
