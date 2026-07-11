package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 时间校验完成后向客户端回执的服务端包。
 * Server packet acknowledging a completed client time check.
 *
 * @author Alcapwnd
 */
public class SM_AFTER_TIME_CHECK extends AionServerPacket {

	@Override
	protected void writeImpl(AionConnection con) {
		writeH(1);
		writeD(0);
	}
}
