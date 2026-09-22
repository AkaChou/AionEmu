package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端同步玩家采集状态的服务端包。
 * Server packet that synchronizes a player's gathering status to the client.
 * @author orz
 * @author Antraxx
 */
public class SM_GATHER_STATUS extends AionServerPacket {

	private final int status;
	private final int playerobjid;
	private final int gatherableobjid;

	/**
	 * @param playerobjid 玩家对象 ID / Player object ID
	 * @param gatherableobjid 可采集物对象 ID / Gatherable object ID
	 * @param status 采集状态 / Gathering status
	 */
	public SM_GATHER_STATUS(int playerobjid, int gatherableobjid, int status) {
		this.playerobjid = playerobjid;
		this.gatherableobjid = gatherableobjid;
		this.status = status;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(playerobjid);
		writeD(gatherableobjid);
		writeH(0); // 未知 / unk
		writeC(status);

	}
}
