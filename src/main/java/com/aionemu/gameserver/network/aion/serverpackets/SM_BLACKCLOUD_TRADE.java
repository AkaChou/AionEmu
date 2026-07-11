package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 黑云交易（Blackcloud Trade）相关的服务端包（占位/初始化）。
 * initialization). / initialization).
 *
 * @author wanke
 */
public class SM_BLACKCLOUD_TRADE extends AionServerPacket {
	public SM_BLACKCLOUD_TRADE() {
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(0);
	}
}
