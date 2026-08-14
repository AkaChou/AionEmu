package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 融合（Coalescence）启动/就绪包。
 * Server packet notifying that coalescence is ready to start.
 *
 * @author Ranastic
 */
public class SM_COALESCENCE_STARTUP extends AionServerPacket {
	private int unk;

	/**
	 * 构造融合启动包。
	 * Creates a coalescence startup packet.
	 *
	 * @param unk 未知字段 / unknown field
	 */
	public SM_COALESCENCE_STARTUP(int unk) {
		this.unk = unk;
	}

	@Override
	protected void writeImpl(AionConnection client) {
		writeD(unk);
	}
}
