package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.NoArgsConstructor;

/**
 * 黑云交易（Blackcloud Trade）相关的服务端包（占位/初始化）。
 * initialization).
 *
 * @author wanke
 */
@NoArgsConstructor
public class SM_BLACKCLOUD_TRADE extends AionServerPacket {
	@Override
	protected void writeImpl(AionConnection con) {
		writeD(0);
	}
}
