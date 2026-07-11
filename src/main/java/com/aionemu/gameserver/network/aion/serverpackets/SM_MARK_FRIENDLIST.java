package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 标记好友列表相关状态的服务端包。
 * Server packet related to marked friend-list state.
 *
 * @author xTz
 */
public class SM_MARK_FRIENDLIST extends AionServerPacket {

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(con.getActivePlayer().getObjectId());
		writeC(1);
		writeH(0);
	}
}
