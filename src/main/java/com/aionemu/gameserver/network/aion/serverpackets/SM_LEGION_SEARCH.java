package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import java.util.List;

import com.aionemu.gameserver.model.team.legion.Legion;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 向客户端返回军团搜索结果列表的服务端包。
 * Server packet that returns the legion search-result list to the client.
 */
@AllArgsConstructor
public class SM_LEGION_SEARCH extends AionServerPacket {
	private final List<Legion> legions;

	@Override
	protected void writeImpl(AionConnection con) {
		writeH(-legions.size());
		for (Legion legion : legions) {
			writeD(legion.getLegionId());
			writeS(legion.getLegionName());
			writeS(GameCoreGameplayServices.legionService().getBrigadeGeneralName(legion));
			writeC(legion.getLegionLevel());
			writeD(legion.getLegionMembers().size());
			writeS(legion.getLegionDescription());
			writeC(legion.getLegionJoinType());
			writeH(legion.getMinLevel());
		}
	}
}
