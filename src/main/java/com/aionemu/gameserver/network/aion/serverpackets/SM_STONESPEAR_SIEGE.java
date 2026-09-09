package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.team.legion.Legion;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 向客户端同步 Stonespear 攻城相关军团数据。
 * Server packet synchronizing Stonespear siege legion data to the client.
 */
@AllArgsConstructor
public class SM_STONESPEAR_SIEGE extends AionServerPacket {
	Legion legion;
	int type = 0;

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(legion.getTerritory().getId());
		writeC(type);
		writeH(0);
	}
}
