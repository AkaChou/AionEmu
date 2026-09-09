package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 融合（Coalescence）启动/就绪包。
 * Server packet notifying that coalescence is ready to start.
 *
 * @author Ranastic
 */
@AllArgsConstructor
public class SM_COALESCENCE_STARTUP extends AionServerPacket {
	private final int unk;

	@Override
	protected void writeImpl(AionConnection client) {
		writeD(unk);
	}
}
