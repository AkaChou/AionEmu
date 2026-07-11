package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.Collection;

import com.aionemu.gameserver.model.team.legion.LegionJoinRequest;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端发送军团加入申请列表的服务端包。
 * Server packet that sends the legion join-request list to the client.
 */
public class SM_LEGION_REQUEST_LIST extends AionServerPacket {
	private Collection<LegionJoinRequest> ljrList;

	/**
	 * 使用加入申请集合构造列表包。
	 * Creates a list packet from a collection of join requests.
	 *
	 * @param ljrList 加入申请集合 / join-request collection
	 */
	public SM_LEGION_REQUEST_LIST(Collection<LegionJoinRequest> ljrList) {
		this.ljrList = ljrList;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeH(-ljrList.size());
		for (LegionJoinRequest ljr : ljrList) {
			writeD(ljr.getPlayerId());
			writeS(ljr.getPlayerName());
			writeC(ljr.getPlayerClass());
			writeC(ljr.getGenderId());
			writeH(ljr.getLevel());
			writeS(ljr.getMsg());
			writeD((int) ljr.getDate().getTime());
		}
	}
}
