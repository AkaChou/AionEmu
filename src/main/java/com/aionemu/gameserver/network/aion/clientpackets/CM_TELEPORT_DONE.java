package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_SPAWN;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapType;

/**
 * 客户端传送完成确认包；私人地图下会重新发送玩家生成包。
 * Client packet confirming teleport is done; re-sends player spawn on personal maps.
 */
public class CM_TELEPORT_DONE extends AionClientPacket {

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_TELEPORT_DONE(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		if (WorldMapType.getWorld(player.getWorldId()).isPersonal()) {
			PacketSendUtility.sendPacket(player, new SM_PLAYER_SPAWN(player));
		}
	}
}
