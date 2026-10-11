package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.List;

import com.aionemu.gameserver.configs.network.NetworkConfig;
import com.aionemu.gameserver.model.ranking.SeasonRankingResult;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端同步赛季排行榜数据。
 * Server packet synchronizing season ranking data to the client.
 * @author Wnkrz
 */
public class SM_SEASON_RANKING extends AionServerPacket {
	private final int tableId;
	private final int server_switch;
	private final List<SeasonRankingResult> data;
	private final int lastUpdate;

	/**
	 * 使用给定参数构造 SM_SEASON_RANKING 包。
	 * Creates a SM_SEASON_RANKING packet with the given parameters.
	 * table id
	 * switch flag
	 * @param data 排行数据 / ranking data
	 * @param lastUpdate 上次更新时间 / last update time
	 */
	public SM_SEASON_RANKING(int tableId, int s_switch, List<SeasonRankingResult> data, int lastUpdate) {
		this.tableId = tableId;
		this.data = data;
		this.lastUpdate = lastUpdate;
		this.server_switch = s_switch;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(tableId);
		writeC(server_switch);
		writeC(0);
		writeD(lastUpdate);
		writeH(data.size());
		for (SeasonRankingResult rs : data) {
			writeD(tableId);
			writeD(rs.getPoints());
			writeD(rs.rank());
			writeD(rs.oldRank());
			writeD(0); // Sex ? 0=male / 1=female
			writeD(rs.playerId());
			writeD(rs.playerRace());
			writeD(rs.playerClass().getClassId());
			writeC(NetworkConfig.GAMESERVER_ID);
			writeS(rs.playerName(), 52);// Player Name
		}
	}
}
