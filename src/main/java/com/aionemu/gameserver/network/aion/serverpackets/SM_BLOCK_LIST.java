package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.player.BlockList;
import com.aionemu.gameserver.model.gameobjects.player.BlockedPlayer;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 同步玩家黑名单（屏蔽列表）的服务端包。
 * Server packet synchronizing the player's block (ignore) list to the client.
 *
 * @author Ben
 */
public class SM_BLOCK_LIST extends AionServerPacket {

	@Override
	protected void writeImpl(AionConnection con) {
		BlockList list = con.getActivePlayer().getBlockList();
		writeH(list.getSize());
		writeC(0); // Unk
		for (BlockedPlayer player : list) {
			writeS(player.getName());
			writeS(player.getReason());
		}
	}
}
