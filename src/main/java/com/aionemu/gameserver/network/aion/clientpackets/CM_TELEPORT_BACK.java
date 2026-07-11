package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.TeleportAnimation;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.services.teleport.TeleportService2;

/**
 * 客户端返回战场/战斗前坐标请求包。
 * Client packet for teleporting back to battle-return coordinates.
 *
 * @author Falke_34
 */
public class CM_TELEPORT_BACK extends AionClientPacket {

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_TELEPORT_BACK(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		// 无可读内容 / Nothing to read
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		float[] coords = player.getBattleReturnCoords();
		if (coords != null && player.getBattleReturnMap() != 0) {
			TeleportService2.teleportTo(player, player.getBattleReturnMap(), 1, coords[0], coords[1], coords[2],
					(byte) 0, TeleportAnimation.BEAM_ANIMATION);
			player.setBattleReturnCoords(0, null);
		}
	}
}
