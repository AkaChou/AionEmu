package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.Collection;

import com.aionemu.gameserver.model.team.legion.LegionJoinRequest;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 向客户端发送军团加入申请列表的服务端包。
 * Server packet that sends the legion join-request list to the client.
 */
@AllArgsConstructor
public class SM_LEGION_REQUEST_LIST extends AionServerPacket {
	private final Collection<LegionJoinRequest> ljrList;

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
