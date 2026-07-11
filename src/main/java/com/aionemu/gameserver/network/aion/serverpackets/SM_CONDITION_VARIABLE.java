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
