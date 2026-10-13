package com.aionemu.gameserver.services.abyss;


import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.services.CronService;
import com.aionemu.gameserver.services.abyss.AbyssRankingCache;
import java.util.Objects;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;



import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimerTask;
import java.util.concurrent.Future;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.configs.main.RankingConfig;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.AbyssRank;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.persistence.dao.AbyssRankDao;
import com.aionemu.gameserver.persistence.dao.ServerVariableDao;
import com.aionemu.gameserver.services.mail.SystemMailService;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.utils.stats.AbyssRankEnum;
import com.aionemu.gameserver.world.World;

/**
 * 欧比斯军阶定时刷新与周奖励分发服务。
 * Abyss-rank scheduled refresh and weekly reward distribution service.
 * <p><b>WIP：</b> 仅文档化，逻辑未改动。 / <b>WIP:</b> docs only; logic untouched.</p>
 *
 * @author Rinzler (Encom)
 */
@Slf4j
public class AbyssRankUpdateService {
	private final AbyssRankingCache abyssRankingCache;
	private static volatile ObjectProvider<AbyssRankUpdateService> instanceProvider;
	private static volatile AbyssRankUpdateService resolvedInstance;
	private final AbyssRankDao abyssRankDAO;
	private final ServerVariableDao serverVariablesDAO;
	private Race rewardRace;
	private final Runnable updateTask = this::performUpdate;
	private Future<?> minuteUpdateTask;

	/**
	 * 使用指定的 DAO 集合构造军阶刷新服务。
	 * Constructs the rank update service with the given DAOs.
	 *
	 * @param abyssRankDAO       欧比斯军阶数据访问对象 / Abyss-rank DAO
	 * @param serverVariablesDAO 服务器变量数据访问对象 / Server-variables DAO
	 */
	public AbyssRankUpdateService(@NonNull AbyssRankDao abyssRankDAO, @NonNull ServerVariableDao serverVariablesDAO, @NonNull AbyssRankingCache abyssRankingCache) {
		this.abyssRankingCache = Objects.requireNonNull(abyssRankingCache, "abyssRankingCache must not be null");

		this.abyssRankDAO = abyssRankDAO;
		this.serverVariablesDAO = serverVariablesDAO;
	}

	/**
	 * 获取实例：必须由 Spring 提供（{@link #setInstanceProvider(ObjectProvider)}）。
	 * Returns the instance, which must be supplied by Spring.
	 * <p>双源静态兜底已退役：缺少 provider 时直接 fail-fast，避免在容器之外静默创建第二套实例。
	 * The legacy static fallback is retired: a missing provider now fails fast instead of silently
	 * creating a second instance outside the container.</p>
	 *
	 * @return 由 Spring 提供的实例 / the Spring-provided instance
	 * @throws IllegalStateException provider 未注入或容器中没有该 Bean /
	 *                               when no provider or bean is available
	 */
	public static AbyssRankUpdateService getInstance() {
		AbyssRankUpdateService resolved = resolvedInstance;
		if (resolved != null) {
			return resolved;
		}
		ObjectProvider<AbyssRankUpdateService> provider = instanceProvider;
		resolved = provider == null ? null : provider.getIfAvailable();
		if (resolved == null) {
			throw new IllegalStateException("AbyssRankUpdateService 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		resolvedInstance = resolved;
		return resolved;
	}

	/**
	 * 注入 Spring 的实例提供者。
	 * Sets the Spring instance provider.
	 *
	 * @param instanceProvider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<AbyssRankUpdateService> instanceProvider) {
		AbyssRankUpdateService.instanceProvider = instanceProvider;
		resolvedInstance = null;
	}

	/**
	 * 按小时 cron 规则调度军阶刷新。
	 * Schedule rank refresh on the hourly cron rule.
	 */
	public void scheduleUpdateHour() {
		int nextTime = serverVariablesDAO.load("abyssRankUpdate");
		if (nextTime < System.currentTimeMillis() / 1000) {
			performUpdate();
		}
		log.info(I18n.get("log.fd688613d2c1"));
		CronService.requireCurrent().schedule(updateTask, RankingConfig.TOP_RANKING_UPDATE_RULE, true);
	}

	/**
	 * 按分钟固定间隔调度军阶刷新。
	 * Schedule rank refresh on a fixed minute interval.
	 */
	public void scheduleUpdateMinute() {
		int nextTime = serverVariablesDAO.load("abyssRankUpdate");
		if (nextTime < System.currentTimeMillis() / 1000) {
			performUpdate();
		}
		log.info(I18n.get("log.fd688613d2c1"));
		minuteUpdateTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				performUpdate();
			}
		}, 0, (long) RankingConfig.TOP_RANKING_UPDATE_RULE2 * 60 * 1000);
	}

	/**
	 * 取消现有调度后按配置重新挂载。
	 * Cancel existing schedules and re-attach based on config.
	 */
	public synchronized void reload() {
		CronService.requireCurrent().cancel(updateTask);
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
		World.getInstance().doOnAllPlayers(player -> {
			AbyssPointsService.AbyssRankCheck(player);
			if (player.getAbyssRank() != null) {
				player.getAbyssRank().doUpdate();
			}
			abyssRankDAO.storeAbyssRank(player);
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
		CronService.requireCurrent().schedule(this::sendRewardWeekly, weekly);
	}

	/**
	 * 按当前军阶向在线玩家发送周奖励模板邮件。
	 * Send weekly template-reward mail to online players by current rank.
	 */
	private void sendRewardWeekly() {
		World.getInstance().doOnAllPlayers(player -> {
			switch (player.getAbyssRank().getRank()) {
				case SUPREME_COMMANDER:
					final int reward1 = rewardRace == Race.ASMODIANS ? 10 : 1;
					SystemMailService.getInstance();
					SystemMailService.sendTemplateRewardMail(reward1, player.getCommonData());
					break;
				case COMMANDER:
					final int reward2 = rewardRace == Race.ASMODIANS ? 11 : 2;
					SystemMailService.getInstance();
					SystemMailService.sendTemplateRewardMail(reward2, player.getCommonData());
					break;
				case GREAT_GENERAL:
					final int reward3 = rewardRace == Race.ASMODIANS ? 12 : 3;
					SystemMailService.getInstance();
					SystemMailService.sendTemplateRewardMail(reward3, player.getCommonData());
					break;
				case GENERAL:
					final int reward4 = rewardRace == Race.ASMODIANS ? 13 : 4;
					SystemMailService.getInstance();
					SystemMailService.sendTemplateRewardMail(reward4, player.getCommonData());
					break;
				case STAR5_OFFICER:
					final int reward5 = rewardRace == Race.ASMODIANS ? 14 : 5;
					SystemMailService.getInstance();
					SystemMailService.sendTemplateRewardMail(reward5, player.getCommonData());
					break;
				case STAR4_OFFICER:
					final int reward6 = rewardRace == Race.ASMODIANS ? 15 : 6;
					SystemMailService.getInstance();
					SystemMailService.sendTemplateRewardMail(reward6, player.getCommonData());
					break;
				case STAR3_OFFICER:
					final int reward7 = rewardRace == Race.ASMODIANS ? 16 : 7;
					SystemMailService.getInstance();
					SystemMailService.sendTemplateRewardMail(reward7, player.getCommonData());
					break;
				case STAR2_OFFICER:
					final int reward8 = rewardRace == Race.ASMODIANS ? 17 : 8;
					SystemMailService.getInstance();
					SystemMailService.sendTemplateRewardMail(reward8, player.getCommonData());
					break;
				case STAR1_OFFICER:
					final int reward9 = rewardRace == Race.ASMODIANS ? 18 : 9;
					SystemMailService.getInstance();
					SystemMailService.sendTemplateRewardMail(reward9, player.getCommonData());
					break;
			}
		});
	}

	/**
	 * 延迟 3 秒刷新排行缓存。
	 * Reload ranking cache after a 3-second delay.
	 */
	public void AbyssRankingCacheUpdate() {
		ThreadPoolManager.getInstance().schedule(new TimerTask() {
			@Override
			public void run() {
				abyssRankingCache.reloadRankings();
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
	 * @param race            阵营 / Race
	 * @param gpLimit         最低 GP 门槛 / minimum GP threshold
	 * @param activeAfterDays 活跃离线天数上限 / max offline days still considered active
	 */
	private void updateAllRanksGpForRace(Race race, int gpLimit, int activeAfterDays) {
		Map<Integer, Integer> playerGpMap = abyssRankDAO.loadPlayersGp(race, gpLimit, activeAfterDays);
		List<Entry<Integer, Integer>> playerGpEntries = new ArrayList<>(playerGpMap.entrySet());
		playerGpEntries.sort(new PlayerGpComparator<>());
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
	 * @param playerGpEntries 按 GP 排序的条目列表 / ordered GP entries
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
			Entry<Integer, Integer> playerGp = playerGpEntries.getFirst();
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
			playerGpEntries.removeFirst();
			updateGpRankTo(rank, playerId);
		}
	}

	/**
	 * 剩余未占名额玩家回写为最高指挥官（原逻辑保留）。
	 * Write remaining players to SUPREME_COMMANDER (original logic preserved).
	 *
	 * @param playerGpEntries 剩余 GP 条目 / remaining GP entries
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
	 * @param newRank  新军阶 / new rank
	 * @param playerId 玩家 ID / player id
	 */
	protected void updateRankTo(AbyssRankEnum newRank, int playerId) {
		// 检查在线玩家军阶是否变化 / check if rank is changed for online players
		Player onlinePlayer = World.getInstance().findPlayer(playerId);
		if (onlinePlayer != null) {
			AbyssRank abyssRank = onlinePlayer.getAbyssRank();
			AbyssRankEnum currentRank = abyssRank.getRank();
			if (currentRank != newRank) {
				abyssRank.setRank(newRank);
				AbyssPointsService.checkRankChanged(onlinePlayer, currentRank, newRank);
			}
		} else {
			abyssRankDAO.updateAbyssRank(playerId, newRank);
		}
	}

	/**
	 * 将 GP 军阶写入在线玩家或离线 DAO。
	 * Write GP rank to an online player or offline DAO.
	 *
	 * @param newRank  新军阶 / new rank
	 * @param playerId 玩家 ID / player id
	 */
	protected void updateGpRankTo(AbyssRankEnum newRank, int playerId) {
		// 检查在线玩家 rankGp 是否变化 / check if rankGp is changed for online players
		Player onlinePlayer = World.getInstance().findPlayer(playerId);
		if (onlinePlayer != null) {
			AbyssRank abyssRank = onlinePlayer.getAbyssRank();
			AbyssRankEnum currentRank = abyssRank.getRank();
			if (currentRank != newRank) {
				abyssRank.setRank(newRank);
				AbyssPointsService.checkRankGpChanged(onlinePlayer, currentRank, newRank);
			}
		} else {
			abyssRankDAO.updateAbyssRank(playerId, newRank);
		}
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
