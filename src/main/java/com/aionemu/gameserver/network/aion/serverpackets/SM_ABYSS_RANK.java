package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.player.AbyssRank;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.utils.stats.AbyssRankEnum;

/**
 * 向客户端同步玩家欧比斯军衔（AP/GP、击杀与日/周统计）的服务端包。
 * Server packet synchronizing the player's Abyss rank (AP/GP, kills, and daily/weekly stats) to the client.
 */
public class SM_ABYSS_RANK extends AionServerPacket {
	private AbyssRank rank;
	private int currentRankId;

	/**
	 * 使用玩家当前欧比斯军衔数据构造同步包。
	 * Creates a sync packet from the player's current Abyss rank data.
	 *
	 * @param rank 欧比斯军衔数据 / Abyss rank data
	 */
	public SM_ABYSS_RANK(AbyssRank rank) {
		this.rank = rank;
		this.currentRankId = rank.getRank().getId();
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeQ(rank.getAp()); // Rank AP.
		writeD(rank.getGp()); // Rank GP.
		writeD(currentRankId); // Current Rank Ap/Gp.
		writeD(rank.getTopRanking()); // Top Ranking.
		if (currentRankId < 9) {
			int nextRankId = currentRankId < AbyssRankEnum.values().length ? currentRankId + 1 : currentRankId;
			writeD(100 * rank.getAp() / AbyssRankEnum.getRankById(nextRankId).getApRequired());
		} else if ((currentRankId >= 9) && (currentRankId <= 18)) {
			int nextRankId = currentRankId < AbyssRankEnum.values().length ? currentRankId + 1 : currentRankId;
			writeD(100 * rank.getGp() / AbyssRankEnum.getRankById(nextRankId).getGpRequired());
		}
		writeD(rank.getAllKill()); // All Kill.
		writeD(rank.getMaxRank()); // Max Rank.
		writeD(rank.getDailyKill()); // Daily Kill.
		writeQ(rank.getDailyAP()); // Daily AP.
		writeD(rank.getDailyGP()); // Daily GP.
		writeD(rank.getWeeklyKill()); // Weekly Kill.
		writeQ(rank.getWeeklyAP()); // Weekly AP.
		writeD(rank.getWeeklyGP()); // Weekly GP.
		writeD(rank.getLastKill()); // Last Kill.
		writeQ(rank.getLastAP()); // Last AP.
		writeD(rank.getLastGP()); // Last GP.
		writeC(0x00);
		writeD(0x00);// unk 5.3
		writeD(0x00);// unk 5.3
	}
}
