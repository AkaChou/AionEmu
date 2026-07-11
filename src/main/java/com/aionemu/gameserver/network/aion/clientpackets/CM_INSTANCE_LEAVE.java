package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;

/**
 * 离开当前副本实例的客户端包。
 * Client packet for leaving the current instance.
 *
 * @author xTz
 */
public class CM_INSTANCE_LEAVE extends AionClientPacket {
	/**
	 * 构造该客户端包。
	 * Constructs this client packet.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余合法状态 / additional valid states
	 */
	public CM_INSTANCE_LEAVE(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		// 无可读内容 / nothing to read
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		if (player.isInInstance()) {
			player.getPosition().getWorldMapInstance().getInstanceHandler().onExitInstance(player);
		}
	}
}
