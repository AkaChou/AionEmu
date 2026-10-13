package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.actions.PlayerMode;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.world.World;

/**
 * 飞行传送/气流移动过程中的位置同步客户端包。
 * Client packet for position sync during flight-teleport or windstream movement.
 * @author -Nemesiss-, Sweetkr, KID
 */
public class CM_MOVE_IN_AIR extends AionClientPacket {

	float x, y, z;
	int distance;
	/**
	 * 构造该客户端包。
	 * Constructs this client packet.
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余合法状态 / additional valid states
	 */
	public CM_MOVE_IN_AIR(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void readImpl() {
		readD();
		x = readF();
		y = readF();
		z = readF();
		readC();
		distance = readD();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();

		if (player.isInState(CreatureState.FLIGHT_TELEPORT)) {
			if (player.isUsingFlyTeleport()) {
				player.setFlightDistance(distance);
			} else if (player.isInPlayerMode(PlayerMode.WINDSTREAM)) {
				player.windstreamPath.distance = distance;
			}
			World.getInstance().updatePosition(player, x, y, z, (byte) 0);
			player.getMoveController().updateLastMove();
		}
	}
}
