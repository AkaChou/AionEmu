package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端同步目标军团头衔/职位信息的服务端包。
 * Server packet that synchronizes a target's legion title/rank info to the client.
 *
 * @author sweetkr
 */
public class SM_LEGION_UPDATE_TITLE extends AionServerPacket {

	private int objectId;
	private int legionId;
	private String legionName;
	private int rank;

	/**
	 * 使用目标对象、军团与职位构造头衔更新包。
	 * Creates a title-update packet from target, legion and rank data.
	 *
	 * target object id
	 * legion id
	 * legion name
	 * @param rank 职位等级 / rank level
	 */
	public SM_LEGION_UPDATE_TITLE(int objectId, int legionId, String legionName, int rank) {
		this.objectId = objectId;
		this.legionId = legionId;
		this.legionName = legionName;
		this.rank = rank;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(objectId);
		writeD(legionId);
		writeS(legionName);
		writeC(rank); // 0: commander(?), 1: centurion, 2: soldier
	}
}
