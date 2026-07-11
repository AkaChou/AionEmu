package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;

/**
 * 处理客户端 /loc 命令的包，返回当前坐标。
 * Client packet for the /loc command, returning the player's current coordinates.
 *
 * @author SoulKeeper
 * @author EvilSpirit
 */
public class CM_CLIENT_COMMAND_LOC extends AionClientPacket {

	/**
	 * 构造客户端包实例。
	 * Constructs a new client packet instance.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_CLIENT_COMMAND_LOC(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);

	}

	/**
	 * 无操作 / Nothing to do
	 */
	@Override
	protected void readImpl() {
		// 空 / empty
	}

	/**
	 * 记录日志 / Logging
	 */
	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		sendPacket(SM_SYSTEM_MESSAGE.STR_CMD_LOCATION_DESC(player.getWorldId(), player.getX(), player.getY(),
				player.getZ()));
	}
}