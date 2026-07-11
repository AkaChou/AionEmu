package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.aionemu.gameserver.model.AbyssRankingResult;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端同步军团欧比斯排行榜数据的服务端包。
 * Server packet synchronizing Abyss legion ranking data to the client.
 *
 * @author zdead, LokiReborn
 */
public class SM_ABYSS_RANKING_LEGIONS extends AionServerPacket {

	private List<AbyssRankingResult> data;
	private Race race;
	private int updateTime;
	private int sendData = 0;

	/**
	 * 使用排行榜数据构造更新包（会标记为需要刷新表格）。
	 * Creates an update packet from ranking data (marks the table for refresh).
	 *
	 * @param updateTime 排行榜更新时间戳 / ranking update timestamp
	 * @param data 军团排行结果列表 / legion ranking results
	 * @param race 种族（天族/魔族） / race (Elyos/Asmodian)
	 */
	public SM_ABYSS_RANKING_LEGIONS(int updateTime, ArrayList<AbyssRankingResult> data, Race race) {
		this.updateTime = updateTime;
		this.data = data;
		this.race = race;
		this.sendData = 1;
	}

	/**
	 * 构造空数据占位包（无表格更新）。
	 * Creates an empty placeholder packet (no table update).
	 *
	 * @param updateTime 排行榜更新时间戳 / ranking update timestamp
	 * @param race 种族（天族/魔族） / race (Elyos/Asmodian)
	 */
	public SM_ABYSS_RANKING_LEGIONS(int updateTime, Race race) {
		this.updateTime = updateTime;
		this.data = Collections.emptyList();
		this.race = race;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(race.getRaceId());// 0:Elyos 1:Asmo
		writeD(updateTime);// Date
		writeD(sendData);// 0:Nothing 1:Update Table
		writeD(sendData);// 0:Nothing 1:Update Table
		writeH(data.size());// list size
		for (AbyssRankingResult rs : data) {
			writeD(rs.getRankPos());// Current Rank
			writeD((rs.getOldRankPos() == 0) ? 76 : rs.getOldRankPos());// Old Rank
			writeD(rs.getLegionId());// Legion Id
			writeD(race.getRaceId());// 0:Elyos 1:Asmo
			writeC(rs.getLegionLevel());// Legion Level
			writeD(rs.getLegionMembers());// Legion Members
			writeQ(rs.getLegionCP());// Contribution Points
			writeS(rs.getLegionName(), 82);// Legion Name
		}
	}
}
