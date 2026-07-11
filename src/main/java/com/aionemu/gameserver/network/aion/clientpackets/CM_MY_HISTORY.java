package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.lifecycle.GameGameplayServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.services.ranking.SeasonRankingService;

/**
 * 请求赛季排行/个人历史记录的客户端包。
 * Client packet requesting season ranking or personal history data.
 *
 * @author Wnkrz
 */
public class CM_MY_HISTORY extends AionClientPacket {
	private int tableId;
	/**
	 * 构造该客户端包。
	 * Constructs this client packet.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余合法状态 / additional valid states
	 */
	public CM_MY_HISTORY(int opcode, AionConnection.State state, AionConnection.State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		tableId = readD();
	}

	@Override
	protected void runImpl() {
		final Player player = this.getConnection().getActivePlayer();
		GameGameplayServices.seasonRankingService().loadPacketPlayer(player, tableId);
	}
}
