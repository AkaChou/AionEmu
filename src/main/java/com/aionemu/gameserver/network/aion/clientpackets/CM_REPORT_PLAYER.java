package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.utils.audit.AuditLogger;

/**
 * 客户端举报玩家请求包（/ReportAutoHunting）。
 * Client packet for reporting a player via /ReportAutoHunting.
 *
 * @author Jego
 */
public class CM_REPORT_PLAYER extends AionClientPacket {

	private String player;

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_REPORT_PLAYER(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		readB(1); // unknown byte.
		player = readS(); // the name of the reported person.
	}

	@Override
	protected void runImpl() {
		Player p = getConnection().getActivePlayer();
		AuditLogger.info(p, "Reports the player: " + player);
	}
}
