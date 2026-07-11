package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 通知客户端移除召唤物所有者关联。
 * Server packet notifying the client to remove summon-owner association.
 *
 * @author ATracer
 */
public class SM_SUMMON_OWNER_REMOVE extends AionServerPacket {

	private int summonObjId;

	/**
	 * 使用给定参数构造 SM_SUMMON_OWNER_REMOVE 包。
	 * Creates a SM_SUMMON_OWNER_REMOVE packet with the given parameters.
	 *
	 * @param summonObjId 召唤物对象 ID / summon object id
	 */
	public SM_SUMMON_OWNER_REMOVE(int summonObjId) {
		this.summonObjId = summonObjId;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(summonObjId);
	}
}
