package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 条件变量同步包：向客户端写入实例内的命名条件变量。
 * Server packet that syncs a named instance condition variable to the client.
 *
 * @author Ranastic
 */
public class SM_CONDITION_VARIABLE extends AionServerPacket {
	private int instanceId;
	private int value;
	private String variable;

	/**
	 * 构造条件变量同步包（取玩家所在实例 ID）。
	 * Creates a condition variable sync packet (uses the player's instance id).
	 *
	 * @param player 目标玩家 / target player
	 * @param variable 条件变量名 / condition variable name
	 * @param value 变量值 / variable value
	 */
	public SM_CONDITION_VARIABLE(Player player, String variable, int value) {
		this.instanceId = player.getInstanceId();
		this.variable = variable;
		this.value = value;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(instanceId);
		writeS(variable);
		writeD(value);
	}
}
