package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 融合（Coalescence）启动/就绪包。
 * ready state.
 *
 * @author Ranastic
 */
public class SM_COALESCENCE_STARTUP extends AionServerPacket {
	private int unk;

	public SM_COALESCENCE_STARTUP(int unk) {
		this.unk = unk;
	}

	@Override
	protected void writeImpl(AionConnection client) {
		writeD(unk);
	}
}
