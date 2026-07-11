package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端同步玩家欧比斯恩惠（Abyss Favor）百分比数值的服务端包。
 * Server packet synchronizing the player's Abyss Favor percentage value to the client.
 *
 * @author Wnkrz
 */
public class SM_ABYSS_FAVOR extends AionServerPacket {
	@Override
	protected void writeImpl(AionConnection con) {
		Player player = con.getActivePlayer();
		// 百分比（50000=5%，100000=10%） / Percent (50000 = 5%, 100000 = 10%)
		writeQ(player.getCommonData().getAbyssFavor());
	}
}
