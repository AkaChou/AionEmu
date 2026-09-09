package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.team.legion.LegionJoinRequest;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 向客户端发送单个军团加入申请玩家信息的服务端包。
 * Server packet that sends a single legion join-request player entry to the client.
 */
@AllArgsConstructor
public class SM_LEGION_REQUEST_PLAYER extends AionServerPacket {
	private final LegionJoinRequest ljr;

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
