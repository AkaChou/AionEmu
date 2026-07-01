/*

 *
 *  Encom is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Encom is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser Public License
 *  along with Encom.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.gameserver.services.ranking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.SeasonRankingDAO;
import com.aionemu.gameserver.model.ranking.SeasonRankingEnum;
import com.aionemu.gameserver.model.ranking.SeasonRankingResult;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SEASON_RANKING;

/**
 * Created by Wnkrz on 24/07/2017.
 */

@Slf4j(topic = "com.aionemu.gameserver.services.ranking.SeasonRankingService")
public class SeasonRankingUpdateService {
	private static volatile ObjectProvider<SeasonRankingUpdateService> instanceProvider;
	private int lastUpdate;
	private final Map<Integer, List<SM_SEASON_RANKING>> players = new HashMap<>();

	public void onStart() {
		renewPlayerRanking(SeasonRankingEnum.HALL_OF_TENACITY.getId());
		renewPlayerRanking(SeasonRankingEnum.ARENA_OF_TENACITY.getId());
		renewPlayerRanking(SeasonRankingEnum.TOWER_OF_CHALLENGE.getId());
		renewPlayerRanking(SeasonRankingEnum.ARENA_6V6.getId());
		log.info("Season Ranking Loaded");
	}

	private void renewPlayerRanking(int tableId) {
		List<SM_SEASON_RANKING> newlyCalculated;
		newlyCalculated = loadRankPacket(tableId);
		players.remove(tableId);
		players.put(tableId, newlyCalculated);
		log.info("Season Ranking Updated");
	}

	private List<SM_SEASON_RANKING> loadRankPacket(int tableid) {
		ArrayList<SeasonRankingResult> list = getDAO().getCompetitionRankingPlayers(tableid);
		List<SM_SEASON_RANKING> playerPackets = new ArrayList<SM_SEASON_RANKING>();
		for (int i = 0; i < list.size(); i += 94) {
			if (list.size() > i + 94) {
				playerPackets.add(new SM_SEASON_RANKING(tableid, 0, list.subList(i, i + 94), lastUpdate));
				playerPackets.add(new SM_SEASON_RANKING(tableid, 1, list.subList(i, i + 94), lastUpdate));
			} else {
				playerPackets.add(new SM_SEASON_RANKING(tableid, 0, list.subList(i, list.size()), lastUpdate));
				playerPackets.add(new SM_SEASON_RANKING(tableid, 1, list.subList(i, list.size()), lastUpdate));
			}
		}
		return playerPackets;
	}

	public List<SM_SEASON_RANKING> getPlayers(int tableId) {
		return players.get(tableId);
	}

	private SeasonRankingDAO getDAO() {
		return DAOManager.getDAO(SeasonRankingDAO.class);
	}

	public static final SeasonRankingUpdateService getInstance() {
		ObjectProvider<SeasonRankingUpdateService> provider = instanceProvider;
		if (provider == null) {
			return SingletonHolder.INSTANCE;
		}
		return provider.getIfAvailable(() -> SingletonHolder.INSTANCE);
	}

	public static void setInstanceProvider(ObjectProvider<SeasonRankingUpdateService> instanceProvider) {
		SeasonRankingUpdateService.instanceProvider = instanceProvider;
	}

	private static class SingletonHolder {
		protected static final SeasonRankingUpdateService INSTANCE = new SeasonRankingUpdateService();
	}
}
