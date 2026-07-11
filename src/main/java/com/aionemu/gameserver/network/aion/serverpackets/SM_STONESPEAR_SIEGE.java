package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.team.legion.Legion;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端同步 Stonespear 攻城相关军团数据。
 * Server packet synchronizing Stonespear siege legion data to the client.
 */
public class SM_STONESPEAR_SIEGE extends AionServerPacket {
	Legion legion;
	int type = 0;

	/**
	 * 使用给定参数构造 SM_STONESPEAR_SIEGE 包。
	 * Creates a SM_STONESPEAR_SIEGE packet with the given parameters.
	 *
	 * legion
	 * type
	 */
	public SM_STONESPEAR_SIEGE(Legion legion, int type) {
		this.legion = legion;
		this.type = type;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(legion.getTerritory().getId());
		writeC(type);
		writeH(0);
	}
}
