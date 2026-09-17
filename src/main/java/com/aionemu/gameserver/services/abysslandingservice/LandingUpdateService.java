package com.aionemu.gameserver.services.abysslandingservice;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

import com.aionemu.gameserver.lifecycle.GameCronServices;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.configs.main.AbyssLandingConfig;
import com.aionemu.gameserver.model.landing.LandingLocation;

/**
 * 欧比斯着陆点积分定时重置服务（任务/纪念碑/设施/指挥官）。
 * Abyss-landing points reset scheduler (quest/monument/facility/commander).
 *
 * <p><b>WIP：</b> 仅文档化，逻辑未改动。 / <b>WIP:</b> docs only; logic untouched.</p>
 */
@Slf4j
public class LandingUpdateService {
	private static volatile ObjectProvider<LandingUpdateService> instanceProvider;

	final LandingLocation redemptionLanding = GameLocationBootstrapServices.abyssLandingService().redemptionLanding();
	final LandingLocation harbingerLanding = GameLocationBootstrapServices.abyssLandingService().harbingerLanding();

	// 任务点数。 / Quest Points.
	final int redemptionPts = 0;
	final int harbingerPts = 0;

	// 纪念碑点数。 / Monument Points.
	final int redemptionPts1 = 0;
	final int harbingerPts1 = 0;

	// 设施点数。 / Facility Points.
	final int redemptionPts2 = 0;
	final int harbingerPts2 = 0;

	// 指挥官点数。 / Commander Points.
	final int redemptionPts3 = 0;
	final int harbingerPts3 = 0;

	/**
	 * 默认构造。
	 * Default constructor.
	 */
	public LandingUpdateService() {
	}

	/**
	 * 若配置启用，按 cron 调度任务积分重置。
	 * Schedule quest-points reset via cron when enabled in config.
	 */
	public void initResetQuestPoints() {
		if (AbyssLandingConfig.ABYSS_LANDING_QUEST_RESET_ENABLED) {
			GameCronServices.cronService().schedule(new Runnable() {
				@Override
				public void run() {
					resetQuestPoints();
				}
			}, () -> AbyssLandingConfig.ABYSS_LANDING_QUEST_RESET_TIME);
		}
	}

	/**
	 * 若配置启用，按 cron 调度纪念碑/设施/指挥官积分重置。
	 * Schedule monument/facility/commander points reset via cron when enabled.
	 */
	public void initResetAbyssLandingPoints() {
		if (AbyssLandingConfig.ABYSS_LANDING_POINTS_RESET_ENABLED) {
			GameCronServices.cronService().schedule(new Runnable() {
				@Override
				public void run() {
					resetMonumentPoints();
					resetFacilityPoints();
					resetCommanderPoints();
				}
			}, () -> AbyssLandingConfig.ABYSS_LANDING_POINTS_RESET_TIME);
		}
	}

	/**
	 * 重置双方着陆点的任务积分并触发等级校验。
	 * Reset quest points for both landings and re-check landing levels.
	 */
	public void resetQuestPoints() {
		log.debug("##### Abyss Landing Reset Quest Points #####");
		long startTime = System.currentTimeMillis();
		// 救赎登陆点。 / Redemption's Landing.
		redemptionLanding.setPoints(redemptionPts);
		redemptionLanding.setQuestPoints(0);
		GameLocationBootstrapServices.abyssLandingService().checkRedemptionLanding(redemptionLanding.getPoints(), false);
		// 先驱登陆点。 / Harbinger's Landing.
		harbingerLanding.setPoints(harbingerPts);
		harbingerLanding.setQuestPoints(0);
		GameLocationBootstrapServices.abyssLandingService().checkHarbingerLanding(harbingerLanding.getPoints(), false);
		// 更新全部登陆点。 / Update All Landing.
		GameLocationBootstrapServices.abyssLandingService().onUpdate();
	}

	/**
	 * 重置双方着陆点的纪念碑积分。
	 * Reset monument points for both landings.
	 */
	public void resetMonumentPoints() {
		log.debug("##### Abyss Landing Reset Monuments Points #####");
		long startTime = System.currentTimeMillis();
		// 救赎登陆点。 / Redemption's Landing.
		redemptionLanding.setPoints(redemptionPts1);
		redemptionLanding.setMonumentsPoints(0);
		GameLocationBootstrapServices.abyssLandingService().checkRedemptionLanding(redemptionLanding.getPoints(), false);
		// 先驱登陆点。 / Harbinger's Landing.
		harbingerLanding.setPoints(harbingerPts1);
		harbingerLanding.setMonumentsPoints(0);
		GameLocationBootstrapServices.abyssLandingService().checkHarbingerLanding(harbingerLanding.getPoints(), false);
		// 更新全部登陆点。 / Update All Landing.
		GameLocationBootstrapServices.abyssLandingService().onUpdate();
	}

	/**
	 * 重置双方着陆点的设施积分。
	 * Reset facility points for both landings.
	 */
	public void resetFacilityPoints() {
		log.debug("##### Abyss Landing Reset Facility Points #####");
		long startTime = System.currentTimeMillis();
		// 救赎登陆点。 / Redemption's Landing.
		redemptionLanding.setPoints(redemptionPts2);
		redemptionLanding.setFacilityPoints(0);
		GameLocationBootstrapServices.abyssLandingService().checkRedemptionLanding(redemptionLanding.getPoints(), false);
		// 先驱登陆点。 / Harbinger's Landing.
		harbingerLanding.setPoints(harbingerPts2);
		harbingerLanding.setFacilityPoints(0);
		GameLocationBootstrapServices.abyssLandingService().checkHarbingerLanding(harbingerLanding.getPoints(), false);
		// 更新全部登陆点。 / Update All Landing.
		GameLocationBootstrapServices.abyssLandingService().onUpdate();
	}

	/**
	 * 重置双方着陆点的指挥官积分。
	 * Reset commander points for both landings.
	 */
	public void resetCommanderPoints() {
		log.debug("##### Abyss Landing Reset Commander Points #####");
		long startTime = System.currentTimeMillis();
		// 救赎登陆点。 / Redemption's Landing.
		redemptionLanding.setPoints(redemptionPts3);
		redemptionLanding.setCommanderPoints(0);
		GameLocationBootstrapServices.abyssLandingService().checkRedemptionLanding(redemptionLanding.getPoints(), false);
		// 先驱登陆点。 / Harbinger's Landing.
		harbingerLanding.setPoints(harbingerPts3);
		harbingerLanding.setCommanderPoints(0);
		GameLocationBootstrapServices.abyssLandingService().checkHarbingerLanding(harbingerLanding.getPoints(), false);
		// 更新全部登陆点。 / Update All Landing.
		GameLocationBootstrapServices.abyssLandingService().onUpdate();
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
	 * @throws IllegalStateException provider 未注入或容器中没有该 Bean / when no provider or bean is available
	 */
	public static LandingUpdateService getInstance() {
		ObjectProvider<LandingUpdateService> provider = instanceProvider;
		LandingUpdateService provided = provider == null ? null : provider.getIfAvailable();
		if (provided == null) {
			throw new IllegalStateException("LandingUpdateService 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		return provided;
	}

	/**
	 * 注入 Spring 实例提供者。
	 * Inject the Spring instance provider.
	 *
	 * @param instanceProvider 实例提供者 / Instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<LandingUpdateService> instanceProvider) {
		LandingUpdateService.instanceProvider = instanceProvider;
	}
}
