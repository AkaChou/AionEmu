package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.atreian_bestiary.PlayerABEntry;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 同步玩家完整阿特雷亚图鉴列表的服务端包。
 * Server packet synchronizing the player's full Atreian Bestiary list.
 *
 * @author Ranastic
 */
public class SM_ATREIAN_BESTIARY_LIST extends AionServerPacket {
	PlayerABEntry[] allAB;
	@SuppressWarnings("unused")
	private Player player;

	/**
	 * @param player 目标玩家，用于读取其图鉴数据 / player whose bestiary data is sent
	 */
	public SM_ATREIAN_BESTIARY_LIST(Player player) {
		this.player = player;
		this.allAB = player.getAtreianBestiary().getAllAB();
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeH(allAB.length);
		for (PlayerABEntry entry : allAB) {
			writeD(entry.getId()); // id
			writeD(entry.getKillCount()); // current kill
			writeC(entry.claimRewardLevel()); // claim Reward
			writeC(entry.getLevel()); // current level
		}
	}
}
