package com.aionemu.gameserver.services.ranking;


import com.aionemu.boot.i18n.I18n;
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
 * 赛季排行榜刷新服务，启动时预加载各榜单缓存包供客户端拉取。
 * Season ranking refresh service that preloads cached ranking packets for client requests on startup.
 *
 * @author Wnkrz
 */
@Slf4j(topic = "com.aionemu.gameserver.services.ranking.SeasonRankingService")
public class SeasonRankingUpdateService {
	private static volatile ObjectProvider<SeasonRankingUpdateService> instanceProvider;
	private int lastUpdate;
	private final Map<Integer, List<SM_SEASON_RANKING>> players = new HashMap<>();

	/**
	 * 服务启动时刷新全部赛季榜单缓存。
	 * Refresh all season ranking caches when the service starts.
	 */
	public void onStart() {
		renewPlayerRanking(SeasonRankingEnum.HALL_OF_TENACITY.getId(), I18n.get("ranking.hall_of_tenacity"));
		renewPlayerRanking(SeasonRankingEnum.ARENA_OF_TENACITY.getId(), I18n.get("ranking.arena_of_tenacity"));
		renewPlayerRanking(SeasonRankingEnum.TOWER_OF_CHALLENGE.getId(), I18n.get("ranking.tower_of_challenge"));
		renewPlayerRanking(SeasonRankingEnum.ARENA_6V6.getId(), I18n.get("ranking.arena_6v6"));
		log.info(I18n.get("log.7b171ec16882"));
	}

	/**
	 * 重新计算指定榜单并替换内存缓存。
	 * Recalculate the given ranking table and replace the in-memory cache.
	 *
	 * @param tableId 排行表 ID / Ranking table ID
	 * @param rankingName 排行名称 / Ranking name
	 */
	private void renewPlayerRanking(int tableId, String rankingName) {
		List<SM_SEASON_RANKING> newlyCalculated;
		newlyCalculated = loadRankPacket(tableId);
		players.remove(tableId);
		players.put(tableId, newlyCalculated);
		log.info(I18n.get("log.34194b33f70d", rankingName));
	}

	/**
	 * 从 DAO 读取竞争排名并按 94 条分页组装下发包。
	 * Load competition ranking from DAO and build dispatch packets in pages of 94 entries.
	 *
	 * @param tableid 排行表 ID / Ranking table ID
	 * @return 排行下发包列表 / Ranking dispatch packets
	 */
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

	/**
	 * 获取指定榜单的缓存下发包列表。
	 * Get the cached dispatch packets for a ranking table.
	 *
	 * @param tableId 排行表 ID / Ranking table ID
	 * @return 下发包列表 / Dispatch packets
	 */
	public List<SM_SEASON_RANKING> getPlayers(int tableId) {
		return players.get(tableId);
	}

	private SeasonRankingDAO getDAO() {
		return DAOManager.getDAO(SeasonRankingDAO.class);
	}

	/**
	 * 获取服务单例（优先 Spring ObjectProvider，否则回退本地单例）。
	 * Get the service singleton (prefer Spring ObjectProvider, otherwise local holder).
	 *
	 * @return 服务实例 / Service instance
	 */
	public static final SeasonRankingUpdateService getInstance() {
		ObjectProvider<SeasonRankingUpdateService> provider = instanceProvider;
		if (provider == null) {
			return SingletonHolder.INSTANCE;
		}
		return provider.getIfAvailable(() -> SingletonHolder.INSTANCE);
	}

	/**
	 * 注入 Spring 实例提供者。
	 * Inject the Spring instance provider.
	 *
	 * @param instanceProvider 实例提供者 / Instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<SeasonRankingUpdateService> instanceProvider) {
		SeasonRankingUpdateService.instanceProvider = instanceProvider;
	}

	private static class SingletonHolder {
		protected static final SeasonRankingUpdateService INSTANCE = new SeasonRankingUpdateService();
	}
}
