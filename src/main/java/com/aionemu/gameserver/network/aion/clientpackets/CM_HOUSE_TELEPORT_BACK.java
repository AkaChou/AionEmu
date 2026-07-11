package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.TeleportAnimation;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.services.teleport.TeleportService2;

/**
 * 从房屋战斗返回点传回的客户端包。
 * Client packet for teleporting back from a house battle-return point.
 */
public class CM_HOUSE_TELEPORT_BACK extends AionClientPacket {
	/**
	 * 构造该客户端包。
	 * Constructs this client packet.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余合法状态 / additional valid states
	 */
	public CM_HOUSE_TELEPORT_BACK(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		float[] coords = player.getBattleReturnCoords();
		if (coords != null && player.getBattleReturnMap() != 0) {
			TeleportService2.teleportTo(player, player.getBattleReturnMap(), 1, coords[0], coords[1], coords[2],
					(byte) 0, TeleportAnimation.JUMP_ANIMATION_2);
			player.setBattleReturnCoords(0, null);
		}
	}
}
