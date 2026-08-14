package com.aionemu.gameserver.services.ranking;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.SeasonRankingDAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.ranking.Arena6V6Ranking;
import com.aionemu.gameserver.model.gameobjects.player.ranking.ArenaOfTenacityRank;
import com.aionemu.gameserver.model.gameobjects.player.ranking.GoldArenaRank;
import com.aionemu.gameserver.model.gameobjects.player.ranking.TowerOfChallengeRank;
import com.aionemu.gameserver.model.ranking.SeasonRankingEnum;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MY_HISTORY;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 赛季排行榜服务，负责加载玩家各类竞技/挑战积分并同步个人历史包。
 * Season ranking service loading player arena/challenge scores and syncing personal history packets.
 *
 * @author Wnkrz
 */
public class SeasonRankingService {
	private static volatile ObjectProvider<SeasonRankingService> instanceProvider;

	/**
	 * 按表 ID 加载玩家对应赛季排行数据并下发。
	 * Load and dispatch the player's season ranking data by table ID.
	 *
	 * 玩家 / Player
	 * @param tableid 排行表 ID / Ranking table ID
	 */
	public void loadPacketPlayer(Player player, int tableid) {
		if (tableid == 1) {
			loadGoldArenaScore(player);
		} else if (tableid == 2) {
			loadTowerScore(player);
		} else if (tableid == 3) {
			loadArena6v6Score(player);
		} else if (tableid == 541) {
			loadArenaOfTenacityScore(player);
		} else {
			return;
		}
	}

	/**
	 * 加载黄金竞技场（坚韧殿堂）积分并下发历史包。
	 * Load Gold Arena (Hall of Tenacity) score and send history packet.
	 *
	 * @param player 玩家 / Player
	 */
	public void loadGoldArenaScore(Player player) {
		GoldArenaRank rank = getDAO().loadGoldArenaRank(player.getObjectId(),
				SeasonRankingEnum.HALL_OF_TENACITY.getId());
		player.setArenaGoldRank(rank);
		PacketSendUtility.sendPacket(player,
				new SM_MY_HISTORY(SeasonRankingEnum.HALL_OF_TENACITY.getId(), player.getArenaGoldRank()));
	}

	/**
	 * 加载挑战之塔积分并下发历史包。
	 * Load Tower of Challenge score and send history packet.
	 *
	 * @param player 玩家 / Player
	 */
	public void loadTowerScore(Player player) {
		TowerOfChallengeRank rank = getDAO().loadTowerOfChallengeRank(player.getObjectId(),
				SeasonRankingEnum.TOWER_OF_CHALLENGE.getId());
		player.setTowerRank(rank);
		PacketSendUtility.sendPacket(player,
				new SM_MY_HISTORY(SeasonRankingEnum.TOWER_OF_CHALLENGE.getId(), player.getTowerRank()));
	}

	/**
	 * 加载 6v6 竞技场积分并下发历史包。
	 * Load Arena 6v6 score and send history packet.
	 *
	 * @param player 玩家 / Player
	 */
	public void loadArena6v6Score(Player player) {
		Arena6V6Ranking rank = getDAO().loadArena6v6Rank(player.getObjectId(), SeasonRankingEnum.ARENA_6V6.getId());
		player.set6v6Rank(rank);
		PacketSendUtility.sendPacket(player,
				new SM_MY_HISTORY(SeasonRankingEnum.ARENA_6V6.getId(), player.get6v6Rank()));
	}

	/**
	 * 加载孤独竞技场积分并下发历史包。
	 * Load Arena of Tenacity score and send history packet.
	 *
	 * @param player 玩家 / Player
	 */
	public void loadArenaOfTenacityScore(Player player) {
		ArenaOfTenacityRank rank = getDAO().loadArenaOfTenacityRank(player.getObjectId(),
				SeasonRankingEnum.ARENA_OF_TENACITY.getId());
		player.setTenacityRank(rank);
		PacketSendUtility.sendPacket(player,
				new SM_MY_HISTORY(SeasonRankingEnum.ARENA_OF_TENACITY.getId(), player.getTenacityRank()));
	}

	/**
	 * 保存熔炉尖塔（挑战之塔）通关时间并更新最佳/上次/当前记录。
	 * Save Crusible Spire (Tower of Challenge) clear time and update best/last/current records.
	 *
	 * 玩家 / Player
	 * @param newTime 新通关时间 / New clear time
	 */
	public void saveCrusibleSpireTime(Player player, int newTime) {
		TowerOfChallengeRank rank = player.getTowerRank();
		// 若新时间更优则更新最佳时间。 / update best time if new time is supp
		if (rank.getBestTime() == 0) {
			rank.setCurrentTime(newTime);
		} else if (rank.getBestTime() > newTime) {
			rank.setBestTime(newTime);
		}
		// 添加上次时间 / add last time
		if (rank.getLastTime() == 0) {
			rank.setLastTime(newTime);
		} else {
			rank.setLastTime(rank.getCurrentTime());
		}
		// 添加当前时间 / add curren time
		rank.setCurrentTime(newTime);
		// 保存到数据库 / save to database
		DAOManager.getDAO(SeasonRankingDAO.class).storeTowerRank(player);
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
	public static final SeasonRankingService getInstance() {
		ObjectProvider<SeasonRankingService> provider = instanceProvider;
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
	public static void setInstanceProvider(ObjectProvider<SeasonRankingService> provider) {
		instanceProvider = provider;
	}

	private static class SingletonHolder {
		protected static final SeasonRankingService INSTANCE = new SeasonRankingService();
	}
}
