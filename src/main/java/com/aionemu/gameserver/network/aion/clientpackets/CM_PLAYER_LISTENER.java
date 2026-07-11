package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.lifecycle.GameFeatureServices;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;

/**
 * 客户端周期监听包，约每五分钟发送一次，用于触发奖励校验等后台逻辑。
 * Client periodic listener packet sent about every five minutes; drives reward checks and similar background logic.
 *
 * @author ginho1
 */
public class CM_PLAYER_LISTENER extends AionClientPacket {

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_PLAYER_LISTENER(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void readImpl() {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();

		if (CustomConfig.ENABLE_REWARD_SERVICE) {
			GameFeatureServices.rewardService().verify(player);
		}
	}
}
