package com.aionemu.gameserver.services.abyss;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.lifecycle.GameCronServices;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimerTask;
import java.util.concurrent.Future;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.configs.main.RankingConfig;
import com.aionemu.gameserver.dao.AbyssRankDAO;
import com.aionemu.gameserver.dao.ServerVariablesDAO;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.AbyssRank;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.mail.SystemMailService;
import com.aionemu.gameserver.utils.stats.AbyssRankEnum;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 欧比斯军阶定时刷新与周奖励分发服务。
 * Abyss-rank scheduled refresh and weekly reward distribution service.
 *
 * <p><b>WIP：</b> 仅文档化，逻辑未改动。 / <b>WIP:</b> docs only; logic untouched.</p>
 *
 * @author Rinzler (Encom)
 */
@Slf4j
public class AbyssRankUpdateService {
	private static volatile ObjectProvider<AbyssRankUpdateService> instanceProvider;
	private Race rewardRace;
	private final Runnable updateTask = this::performUpdate;
	private Future<?> minuteUpdateTask;


	/**
	 * 默认构造。
	 * Default constructor.
	 */
	public AbyssRankUpdateService() {
	}

	/**
	 * 获取单例（优先 Spring {@link ObjectProvider}）。
	 * Obtain the singleton (prefer Spring {@link ObjectProvider}).
	 *
	 * Service instance
	 */
	public static AbyssRankUpdateService getInstance() {
		ObjectProvider<AbyssRankUpdateService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.instance);
		}
		return SingletonHolder.instance;
	}

	/**
	 * 注入 Spring 实例提供者。
	 * Inject the Spring instance provider.
	 *
	 * @param provider 实例提供者 / Instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<AbyssRankUpdateService> provider) {
		instanceProvider = provider;
	}

	/**
	 * 按小时 cron 规则调度军阶刷新。
	 * Schedule rank refresh on the hourly cron rule.
	 */
	public void scheduleUpdateHour() {
		ServerVariablesDAO dao = DAOManager.getDAO(ServerVariablesDAO.class);
		int nextTime = dao.load("abyssRankUpdate");
		if (nextTime < System.currentTimeMillis() / 1000) {
			performUpdate();
		}
		log.info(I18n.get("log.fd688613d2c1"));
		GameCronServices.cronService().schedule(updateTask, RankingConfig.TOP_RANKING_UPDATE_RULE, true);
	}

	/**
	 * 按分钟固定间隔调度军阶刷新。
	 * Schedule rank refresh on a fixed minute interval.
	 */
	public void scheduleUpdateMinute() {
		ServerVariablesDAO dao = DAOManager.getDAO(ServerVariablesDAO.class);
		int nextTime = dao.load("abyssRankUpdate");
		if (nextTime < System.currentTimeMillis() / 1000) {
			performUpdate();
		}
		log.info(I18n.get("log.fd688613d2c1"));
		minuteUpdateTask = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				performUpdate();
			}
		}, 0, RankingConfig.TOP_RANKING_UPDATE_RULE2 * 60 * 1000);
	}

	/**
	 * 取消现有调度后按配置重新挂载。
	 * Cancel existing schedules and re-attach based on config.
	 */
	public synchronized void reload() {
		GameCronServices.cronService().cancel(updateTask);
		if (minuteUpdateTask != null) {
			minuteUpdateTask.cancel(false);
			minuteUpdateTask = null;
		}
		if (RankingConfig.TOP_RANKING_UPDATE_SETTING) {
			scheduleUpdateHour();
		} else {
			scheduleUpdateMinute();
		}
	}

	/**
	 * 执行全量军阶刷新：在线玩家落库、限名额 GP 军阶与排行缓存。
	 * Run full rank refresh: persist online players, limited GP ranks, ranking cache.
	 */
	public void performUpdate() {
		log.info(I18n.get("log.97b1fd95b293"));
		long startTime = System.currentTimeMillis();
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				AbyssPointsService.AbyssRankCheck(player);
				player.getAbyssRank().doUpdate();
				DAOManager.getDAO(AbyssRankDAO.class).storeAbyssRank(player);
			}
		});
		updateLimitedGpRanks();
		AbyssRankingCacheUpdate();
		log.info(I18n.get("log.06a04520551c", (System.currentTimeMillis() - startTime) / 1000));
	}

	/**
	 * 初始化每周一中午的军阶邮件奖励。
	 * Initialize Monday-noon weekly rank mail rewards.
	 */
	public void initRewardWeeklyManager() {
		log.info(I18n.get("log.eacf3461fb47"));
		String weekly = "0 0 12 ? * MON *";
		GameCronServices.cronService().schedule(new Runnable() {
			public void run() {
				sendRewardWeekly();
			}
		}, weekly);
	}

	/**
	 * 按当前军阶向在线玩家发送周奖励模板邮件。
	 * Send weekly template-reward mail to online players by current rank.
	 */
	private void sendRewardWeekly() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				switch (player.getAbyssRank().getRank()) {
				case SUPREME_COMMANDER:
					final int reward1 = rewardRace == Race.ASMODIANS ? 10 : 1;
					GameFeatureServices.systemMailService().sendTemplateRewardMail(reward1, player.getCommonData());
					break;
				case COMMANDER:
					final int reward2 = rewardRace == Race.ASMODIANS ? 11 : 2;
					GameFeatureServices.systemMailService().sendTemplateRewardMail(reward2, player.getCommonData());
					break;
				case GREAT_GENERAL:
					final int reward3 = rewardRace == Race.ASMODIANS ? 12 : 3;
					GameFeatureServices.systemMailService().sendTemplateRewardMail(reward3, player.getCommonData());
					break;
				case GENERAL:
					final int reward4 = rewardRace == Race.ASMODIANS ? 13 : 4;
					GameFeatureServices.systemMailService().sendTemplateRewardMail(reward4, player.getCommonData());
					break;
				case STAR5_OFFICER:
					final int reward5 = rewardRace == Race.ASMODIANS ? 14 : 5;
					GameFeatureServices.systemMailService().sendTemplateRewardMail(reward5, player.getCommonData());
					break;
				case STAR4_OFFICER:
					final int reward6 = rewardRace == Race.ASMODIANS ? 15 : 6;
					GameFeatureServices.systemMailService().sendTemplateRewardMail(reward6, player.getCommonData());
					break;
				case STAR3_OFFICER:
					final int reward7 = rewardRace == Race.ASMODIANS ? 16 : 7;
					GameFeatureServices.systemMailService().sendTemplateRewardMail(reward7, player.getCommonData());
					break;
				case STAR2_OFFICER:
					final int reward8 = rewardRace == Race.ASMODIANS ? 17 : 8;
					GameFeatureServices.systemMailService().sendTemplateRewardMail(reward8, player.getCommonData());
					break;
				case STAR1_OFFICER:
					final int reward9 = rewardRace == Race.ASMODIANS ? 18 : 9;
					GameFeatureServices.systemMailService().sendTemplateRewardMail(reward9, player.getCommonData());
					break;
				}
			}
		});
	}

	/**
	 * 延迟 3 秒刷新排行缓存。
	 * Reload ranking cache after a 3-second delay.
	 */
	public void AbyssRankingCacheUpdate() {
		GameThreadPoolServices.threadPoolManager().schedule(new TimerTask() {
			@Override
			public void run() {
				GameCoreGameplayServices.abyssRankingCache().reloadRankings();
			}
		}, 3 * 1000);
	}

	/**
	 * 更新双方种族的限名额 GP 军阶。
	 * Update limited GP ranks for both races.
	 */
	private void updateLimitedGpRanks() {
		updateAllRanksGpForRace(Race.ASMODIANS, AbyssRankEnum.STAR1_OFFICER.getGpRequired(),
				RankingConfig.TOP_RANKING_MAX_OFFLINE_DAYS);
		updateAllRanksGpForRace(Race.ELYOS, AbyssRankEnum.STAR1_OFFICER.getGpRequired(),
				RankingConfig.TOP_RANKING_MAX_OFFLINE_DAYS);
	}

	/**
	 * 按 GP 降序为指定种族分配军官名额。
	 * Assign officer quotas for a race by descending GP.
	 *
	 * 阵营 / Race
	 * Minimum GP threshold
	 * @param activeAfterDays 活跃离线天数上限 / Max offline days still considered active
	 */
	private void updateAllRanksGpForRace(Race race, int gpLimit, int activeAfterDays) {
		Map<Integer, Integer> playerGpMap = DAOManager.getDAO(AbyssRankDAO.class).loadPlayersGp(race, gpLimit,
				activeAfterDays);
		List<Entry<Integer, Integer>> playerGpEntries = new ArrayList<Entry<Integer, Integer>>(playerGpMap.entrySet());
		Collections.sort(playerGpEntries, new PlayerGpComparator<Integer, Integer>());
		selectGpRank(AbyssRankEnum.SUPREME_COMMANDER, playerGpEntries);
		selectGpRank(AbyssRankEnum.COMMANDER, playerGpEntries);
		selectGpRank(AbyssRankEnum.GREAT_GENERAL, playerGpEntries);
		selectGpRank(AbyssRankEnum.GENERAL, playerGpEntries);
		selectGpRank(AbyssRankEnum.STAR5_OFFICER, playerGpEntries);
		selectGpRank(AbyssRankEnum.STAR4_OFFICER, playerGpEntries);
		selectGpRank(AbyssRankEnum.STAR3_OFFICER, playerGpEntries);
		selectGpRank(AbyssRankEnum.STAR2_OFFICER, playerGpEntries);
		selectGpRank(AbyssRankEnum.STAR1_OFFICER, playerGpEntries);
		updateToNoQuotaGpRank(playerGpEntries);
	}

	/**
	 * 从 GP 列表头部按名额选取玩家写入军阶。
	 * Take quota players from the head of the GP list and write their ranks.
	 *
	 * @param rank            目标军阶 / Target rank
	 * Ordered GP entries
	 */
	private void selectGpRank(AbyssRankEnum rank, List<Entry<Integer, Integer>> playerGpEntries) {
		int quota = (rank.getId() > 9 && rank.getId() < 18)
				? rank.getQuota() - AbyssRankEnum.getRankById(rank.getId() + 1).getQuota()
				: rank.getQuota();
		for (int i = 0; i < quota; i++) {
			if (playerGpEntries.isEmpty()) {
				return;
			}
			// 检查列表中下一名玩家 / check next player in list
			Entry<Integer, Integer> playerGp = playerGpEntries.get(0);
			// 检查地图中是否还有玩家 / check if there are some players left in map
			if (playerGp == null) {
				return;
			}
			int playerId = playerGp.getKey();
			int gp = playerGp.getValue();
			// 检查该玩家（及其他人）是否有足够 GP / check if this (and the rest) player has required gp count
			if (gp < rank.getGpRequired()) {
				return;
			}
			// 移除玩家并更新其 rankGp / remove player and update its rankGp
			playerGpEntries.remove(0);
			updateGpRankTo(rank, playerId);
		}
	}

	/**
	 * 剩余未占名额玩家回写为最高指挥官（原逻辑保留）。
	 * Write remaining players to SUPREME_COMMANDER (original logic preserved).
	 *
	 * Remaining GP entries
	 */
	private void updateToNoQuotaGpRank(List<Entry<Integer, Integer>> playerGpEntries) {
		for (Entry<Integer, Integer> playerGpEntry : playerGpEntries) {
			updateGpRankTo(AbyssRankEnum.SUPREME_COMMANDER, playerGpEntry.getKey());
		}
	}

	/**
	 * 将 AP 军阶写入在线玩家或离线 DAO。
	 * Write AP rank to an online player or offline DAO.
	 *
	 * New rank
	 * Player id
	 */
	protected void updateRankTo(AbyssRankEnum newRank, int playerId) {
		// 检查在线玩家军阶是否变化 / check if rank is changed for online players
		Player onlinePlayer = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(playerId);
		if (onlinePlayer != null) {
			AbyssRank abyssRank = onlinePlayer.getAbyssRank();
			AbyssRankEnum currentRank = abyssRank.getRank();
			if (currentRank != newRank) {
				abyssRank.setRank(newRank);
				AbyssPointsService.checkRankChanged(onlinePlayer, currentRank, newRank);
			}
		} else {
			DAOManager.getDAO(AbyssRankDAO.class).updateAbyssRank(playerId, newRank);
		}
	}

	/**
	 * 将 GP 军阶写入在线玩家或离线 DAO。
	 * Write GP rank to an online player or offline DAO.
	 *
	 * New rank
	 * Player id
	 */
	protected void updateGpRankTo(AbyssRankEnum newRank, int playerId) {
		// 检查在线玩家 rankGp 是否变化 / check if rankGp is changed for online players
		Player onlinePlayer = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(playerId);
		if (onlinePlayer != null) {
			AbyssRank abyssRank = onlinePlayer.getAbyssRank();
			AbyssRankEnum currentRank = abyssRank.getRank();
			if (currentRank != newRank) {
				abyssRank.setRank(newRank);
				AbyssPointsService.checkRankGpChanged(onlinePlayer, currentRank, newRank);
			}
		} else {
			DAOManager.getDAO(AbyssRankDAO.class).updateAbyssRank(playerId, newRank);
		}
	}

	/**
	 * 静态单例持有者。
	 * Static singleton holder.
	 */
	private static class SingletonHolder {
		protected static final AbyssRankUpdateService instance = new AbyssRankUpdateService();
	}

	/**
	 * 按 GP 降序比较的条目比较器。
	 * Entry comparator ordering by GP descending.
	 *
	 * @param <K> 键类型 / Key type
	 * @param <V> 可比较值类型 / Comparable value type
	 */
	private static class PlayerGpComparator<K, V extends Comparable<V>> implements Comparator<Entry<K, V>> {
		@Override
		public int compare(Entry<K, V> o1, Entry<K, V> o2) {
			return -o1.getValue().compareTo(o2.getValue()); // descending order
		}
	}
}
