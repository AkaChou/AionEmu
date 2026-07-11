package com.aionemu.gameserver.network.aion.clientpackets;

import lombok.extern.slf4j.Slf4j;
import java.util.List;

import com.aionemu.gameserver.lifecycle.GameMaintenanceServices;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SEASON_RANKING;

/**
 * 客户端赛季排行榜查询请求包。
 * Client packet for requesting season ranking data.
 *
 * @author Wnkrz
 */
@Slf4j

public class CM_SEASON_RANKING extends AionClientPacket {
	private int tableId;
	private int serverSwitch;

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_SEASON_RANKING(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		tableId = readD();
		serverSwitch = readC();
	}

	@Override
	protected void runImpl() {
		List<SM_SEASON_RANKING> results = GameMaintenanceServices.seasonRankingUpdateService().getPlayers(tableId);
		for (SM_SEASON_RANKING packet : results) {
			sendPacket(packet);
		}
	}
}
