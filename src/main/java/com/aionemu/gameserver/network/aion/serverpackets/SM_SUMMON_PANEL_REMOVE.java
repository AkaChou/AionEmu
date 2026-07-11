package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 通知客户端关闭/移除召唤物控制面板。
 * Server packet notifying the client to close/remove the summon control panel.
 *
 * @author ATracer
 */
public class SM_SUMMON_PANEL_REMOVE extends AionServerPacket {

	@Override
	protected void writeImpl(AionConnection con) {

		writeH(0);
		writeC(0);
	}
}
