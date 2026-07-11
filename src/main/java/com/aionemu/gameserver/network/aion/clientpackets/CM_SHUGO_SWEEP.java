package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.lifecycle.GameEventBootstrapServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.services.events.ShugoSweepService;

/**
 * 客户端术古扫雷活动操作请求包（重置棋盘或投掷骰子）。
 * Client packet for Shugo Sweep event actions (reset board or launch dice).
 *
 * @author Ghostfur
 */
public class CM_SHUGO_SWEEP extends AionClientPacket {

	private int action;

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_SHUGO_SWEEP(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		action = readC();
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		if (player == null) {
			return;
		}
		switch (action) {
		case 0: // 重置 / Reset
			GameEventBootstrapServices.shugoSweepService().resetBoard(player);
			break;
		case 1: // Launch Dice
			if (player.getPlayerShugoSweep().getFreeDice() != 0 || player.getCommonData().getGoldenDice() != 0) {
				GameEventBootstrapServices.shugoSweepService().launchDice(player);
			} else {
				return;
			}
			break;
		}
	}
}
