package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;

/**
 * 客户端停止训练/试炼副本训练请求包。
 * Client packet for stopping training in training instance maps.
 */
public class CM_STOP_TRAINING extends AionClientPacket {
	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_STOP_TRAINING(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		switch (player.getWorldId()) {
		case 300320000:
		case 300300000:
		case 302400000:
			player.getPosition().getWorldMapInstance().getInstanceHandler().onStopTraining(player);
			break;
		}
	}
}
