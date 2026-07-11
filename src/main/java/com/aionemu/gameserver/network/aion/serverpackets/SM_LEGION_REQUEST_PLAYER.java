package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.team.legion.LegionJoinRequest;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端发送单个军团加入申请玩家信息的服务端包。
 * Server packet that sends a single legion join-request player entry to the client.
 */
public class SM_LEGION_REQUEST_PLAYER extends AionServerPacket {
	private LegionJoinRequest ljr;

	/**
	 * 使用加入申请构造单玩家信息包。
	 * Creates a single-player info packet from a join request.
	 *
	 * @param ljr 加入申请 / join request
	 */
	public SM_LEGION_REQUEST_PLAYER(LegionJoinRequest ljr) {
		this.ljr = ljr;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(ljr.getPlayerId());
		writeS(ljr.getPlayerName());
		writeC(ljr.getPlayerClass());
		writeC(ljr.getGenderId());
		writeH(ljr.getLevel());
		writeS(ljr.getMsg());
		writeD((int) ljr.getDate().getTime());
	}
}
