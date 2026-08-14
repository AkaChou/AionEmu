package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.utils.gametime.GameTimeManager;

/**
 * 向客户端发送当前游戏时间（自 1/1/00 00:00:00 起的分钟数）。
 * Server packet that sends the current game time in minutes since 1/1/00 00:00:00.
 *
 * @author Ben
 */
public class SM_GAME_TIME extends AionServerPacket {

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(GameTimeManager.getGameTime().getTime()); // 自 1/1/00 00:00:00 起的分钟数 / minutes since 1/1/00 00:00:00
	}
}
