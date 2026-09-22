package com.aionemu.gameserver.services.abyss;


import com.aionemu.boot.i18n.I18n;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.configs.main.CleaningConfig;
import com.aionemu.gameserver.dao.AbyssRankDAO;
import com.aionemu.gameserver.model.AbyssRankingResult;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 欧比斯排行清理服务：按配置周期剔除长期未登录上榜玩家。
 * Abyss-rank cleaning service: drops long-offline ranked players by configured period.
 */
@Slf4j
public class AbyssRankCleaningService {

    /**
     * -- SETTER --
     *  注入 Spring 实例提供者。
     *  Inject the Spring instance provider.
     *
     * @param provider 实例提供者 / Instance provider
     */
    @Setter
    private static volatile ObjectProvider<AbyssRankCleaningService> instanceProvider;

	private final int SECURITY_MINIMUM_PERIOD = 30;

	private long startTime;

	/**
	 * 构造时若启用清理配置则立即执行一轮。
	 * Runs one cleaning pass on construction when the cleaning config is enabled.
	 */
	public AbyssRankCleaningService() {
		if (CleaningConfig.ABYSS_CLEANING_ENABLE) {
			runCleaning();
		}
	}

	/**
	 * 校验最小安全周期后执行排行清理。
	 * Validate the security-minimum period, then run ranking cleaning.
	 */
	private void runCleaning() {
		log.info(I18n.get("log.fb9f433da952"));
		startTime = System.currentTimeMillis();

		int periodInDays = CleaningConfig.ABYSS_CLEANING_PERIOD;

		if (periodInDays > SECURITY_MINIMUM_PERIOD) {
			runAbyssRankingCleaning(periodInDays);
		} else {
			log.warn(I18n.get("log.d2c5a329849e"));
		}
	}

	/**
	 * 扫描双方上榜玩家，移除过期离线者并刷新缓存。
	 * Scan both races' ranked players, remove stale offline ones, and reload cache.
	 */
	private void runAbyssRankingCleaning(int periodInDays) {
		ArrayList<AbyssRankingResult> rankingsElyos = DAOManager.getDAO(AbyssRankDAO.class)
				.getAbyssRankingPlayers(Race.ELYOS);
		ArrayList<AbyssRankingResult> rankingsAsmos = DAOManager.getDAO(AbyssRankDAO.class)
				.getAbyssRankingPlayers(Race.ASMODIANS);
		List<Player> ToArray = new ArrayList<>();
		long offlineThresholdMs = (long) periodInDays * 24L * 60L * 60L * 1000L;
		for (AbyssRankingResult result : rankingsElyos) {
			Player p = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(result.getPlayerName());
			if (p == null) {
				continue;
			}
			Timestamp t = p.getCommonData().getLastOnline();
			boolean isOutOfDate = (System.currentTimeMillis() - t.getTime()) >= offlineThresholdMs;
			if (isOutOfDate) {
				ToArray.add(p);
			}
		}

		for (AbyssRankingResult result : rankingsAsmos) {
			Player p = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(result.getPlayerName());
			if (p == null) {
				continue;
			}

			Timestamp t = p.getCommonData().getLastOnline();
			boolean isOutOfDate = (System.currentTimeMillis() - t.getTime()) >= offlineThresholdMs;
			if (isOutOfDate) {
				ToArray.add(p);
			}
		}

		if (ToArray.size() > 0) {
			DAOManager.getDAO(AbyssRankDAO.class).removePlayer(ToArray);
			GameCoreGameplayServices.abyssRankingCache().reloadRankings();
			log.info(I18n.get("log.3ff8b7b008ed", ToArray.size(), (System.currentTimeMillis() - startTime) / 1000L));
		} else {
			log.info(I18n.get("log.5be7179a3b2d"));
		}
	}

	/**
	 * 获取实例：必须由 Spring 提供（{@link #setInstanceProvider(ObjectProvider)}）。
	 * Returns the instance, which must be supplied by Spring.
	 *
	 * <p>双源静态兜底已退役：缺少 provider 时直接 fail-fast，避免在容器之外静默创建第二套实例。
	 * The legacy static fallback is retired: a missing provider now fails fast instead of silently
	 * creating a second instance outside the container.</p>
	 *
	 * @return 由 Spring 提供的实例 / the Spring-provided instance
	 * @throws IllegalStateException provider 未注入或容器中没有该 Bean /
	 *         when no provider or bean is available
	 */
	public static AbyssRankCleaningService getInstance() {
		ObjectProvider<AbyssRankCleaningService> provider = instanceProvider;
		AbyssRankCleaningService provided = provider == null ? null : provider.getIfAvailable();
		if (provided == null) {
			throw new IllegalStateException("AbyssRankCleaningService 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		return provided;
	}

}
