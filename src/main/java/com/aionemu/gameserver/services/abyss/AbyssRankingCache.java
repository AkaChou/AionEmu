package com.aionemu.gameserver.services.abyss;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.AbyssRankDAO;
import com.aionemu.gameserver.model.AbyssRankingResult;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ABYSS_RANKING_LEGIONS;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ABYSS_RANKING_PLAYERS;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 欧比斯排行榜缓存：按种族缓存玩家/军团排行数据包并支持全量刷新。
 * Abyss ranking cache: race-keyed player/legion ranking packets with full reload.
 */
@Slf4j
public class AbyssRankingCache {
	private static volatile ObjectProvider<AbyssRankingCache> instanceProvider;
	private int lastUpdate;
	private final Map<Race, List<SM_ABYSS_RANKING_PLAYERS>> players = new HashMap<>();
	private final Map<Race, SM_ABYSS_RANKING_LEGIONS> legions = new HashMap<>();

	/**
	 * 从 DAO 重算排行并刷新玩家/军团缓存，重置在线玩家的排行已更新标志。
	 * Recompute rankings from DAO, refresh player/legion caches, reset online players' ranking flags.
	 */
	public void reloadRankings() {
		log.info(I18n.get("log.d227d2ed702b"));
		this.lastUpdate = (int) (System.currentTimeMillis() / 1000);
		getDAO().updateRankList();
		renewPlayerRanking(Race.ASMODIANS);
		renewPlayerRanking(Race.ELYOS);
		renewLegionRanking();
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				player.resetAbyssRankListUpdated();
			}
		});
	}

	/**
	 * 重算双方军团排行并推送给军团服务。
	 * Rebuild both races' legion rankings and push to the legion service.
	 */
	private void renewLegionRanking() {
		Map<Integer, Integer> newLegionRankingCache = new HashMap<Integer, Integer>();
		ArrayList<AbyssRankingResult> elyosRanking = getDAO().getAbyssRankingLegions(Race.ELYOS);
		ArrayList<AbyssRankingResult> asmoRanking = getDAO().getAbyssRankingLegions(Race.ASMODIANS);
		legions.clear();
		legions.put(Race.ASMODIANS, new SM_ABYSS_RANKING_LEGIONS(lastUpdate, asmoRanking, Race.ASMODIANS));
		legions.put(Race.ELYOS, new SM_ABYSS_RANKING_LEGIONS(lastUpdate, elyosRanking, Race.ELYOS));
		for (AbyssRankingResult result : elyosRanking) {
			newLegionRankingCache.put(Integer.valueOf(result.getLegionId()), result.getRankPos());
		}
		for (AbyssRankingResult result : asmoRanking) {
			newLegionRankingCache.put(Integer.valueOf(result.getLegionId()), result.getRankPos());
		}
		GameCoreGameplayServices.legionService().performRankingUpdate(newLegionRankingCache);
	}

	/**
	 * 重算指定种族的玩家排行数据包。
	 * Rebuild player-ranking packets for the given race.
	 *
	 * @param race 阵营 / Race
	 */
	private void renewPlayerRanking(Race race) {
		List<SM_ABYSS_RANKING_PLAYERS> newlyCalculated;
		newlyCalculated = generatePacketsForRace(race);
		players.remove(race);
		players.put(race, newlyCalculated);
	}

	/**
	 * 按每页 44 条切分玩家排行结果为客户端包列表。
	 * Split player ranking results into client packets of 44 entries per page.
	 *
	 * @param race 阵营 / Race
	 * @return 分页数据包 / Paged packets
	 */
	private List<SM_ABYSS_RANKING_PLAYERS> generatePacketsForRace(Race race) {
		ArrayList<AbyssRankingResult> list = getDAO().getAbyssRankingPlayers(race);
		int page = 1;
		List<SM_ABYSS_RANKING_PLAYERS> playerPackets = new ArrayList<SM_ABYSS_RANKING_PLAYERS>();
		for (int i = 0; i < list.size(); i += 44) {
			if (list.size() > i + 44) {
				playerPackets.add(new SM_ABYSS_RANKING_PLAYERS(lastUpdate, list.subList(i, i + 44), race, page, false));
			} else {
				playerPackets
						.add(new SM_ABYSS_RANKING_PLAYERS(lastUpdate, list.subList(i, list.size()), race, page, true));
			}
			page++;
		}
		return playerPackets;
	}

	/**
	 * @param race 阵营 / Race
	 * @return 玩家排行包列表 / Player ranking packets
	 */
	public List<SM_ABYSS_RANKING_PLAYERS> getPlayers(Race race) {
		return players.get(race);
	}

	/**
	 * @param race 阵营 / Race
	 * @return 军团排行包 / Legion ranking packet
	 */
	public SM_ABYSS_RANKING_LEGIONS getLegions(Race race) {
		return legions.get(race);
	}

	/**
	 * @return 上次刷新时间戳（秒） / Last-update epoch seconds
	 */
	public int getLastUpdate() {
		return lastUpdate;
	}

	/**
	 * @return 欧比斯军阶 DAO / Abyss-rank DAO
	 */
	private AbyssRankDAO getDAO() {
		return DAOManager.getDAO(AbyssRankDAO.class);
	}

	/**
	 * 获取单例（优先 Spring {@link ObjectProvider}）。
	 * Obtain the singleton (prefer Spring {@link ObjectProvider}).
	 *
	 * @return 缓存实例 / cache instance
	 */
	public static final AbyssRankingCache getInstance() {
		ObjectProvider<AbyssRankingCache> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.INSTANCE);
		}
		return SingletonHolder.INSTANCE;
	}

	/**
	 * 注入 Spring 实例提供者。
	 * Inject the Spring instance provider.
	 *
	 * @param provider 实例提供者 / Instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<AbyssRankingCache> provider) {
		instanceProvider = provider;
	}

	/**
	 * 静态单例持有者。
	 * Static singleton holder.
	 */
	private static class SingletonHolder {
		protected static final AbyssRankingCache INSTANCE = new AbyssRankingCache();
	}
}
