package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 风道（Windstream）状态同步服务端包。
 * Server packet that syncs windstream state.
 */
@AllArgsConstructor
public class SM_WINDSTREAM extends AionServerPacket {

	private final int unk1;
	private final int unk2;

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(unk1);
		writeC(unk2);
	}
}
