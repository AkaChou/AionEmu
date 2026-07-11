package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 风道（Windstream）状态同步服务端包。
 * Server packet that syncs windstream state.
 */
public class SM_WINDSTREAM extends AionServerPacket {

	private int unk1;
	private int unk2;

	/**
	 * @param unk1 风道相关参数 1 / windstream parameter 1
	 * @param unk2 风道相关参数 2 / windstream parameter 2
	 */
	public SM_WINDSTREAM(int unk1, int unk2) {
		this.unk1 = unk1;
		this.unk2 = unk2;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(unk1);
		writeC(unk2);
	}
}
