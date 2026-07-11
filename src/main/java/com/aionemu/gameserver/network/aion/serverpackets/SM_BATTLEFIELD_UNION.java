package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 同步战场联盟（要塞战联合队列）可用性与人数的服务端包。
 * Server packet synchronizing Battlefield Union (fortress-war joint queue) availability and member counts.
 *
 * @author wanke
 */
public class SM_BATTLEFIELD_UNION extends AionServerPacket {
	int fortressId;
	boolean isAvailable;
	int timer;
	int memberSize;
	int maxSize;

	/**
	 * fortress id
	 * @param isAvailable 是否开放报名 / whether registration is open
	 * @param memberSize 当前报名人数 / current registered member count
	 * maximum capacity
	 */
	public SM_BATTLEFIELD_UNION(int fortressId, boolean isAvailable, int memberSize, int maxSize) {
		this.fortressId = fortressId;
		this.isAvailable = isAvailable;
		this.memberSize = memberSize;
		this.maxSize = maxSize;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(fortressId);
		writeC(isAvailable ? 0 : 1);
		writeD(-2080374784);
		writeD(4161);
		writeD(memberSize);
		writeD(maxSize);
	}
}
