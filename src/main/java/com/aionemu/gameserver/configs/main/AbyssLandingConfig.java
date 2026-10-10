package com.aionemu.gameserver.configs.main;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.aionemu.commons.configuration.Property;

import lombok.Getter;

/**
 * 欧比斯登陆点任务与积分重置相关配置，支持 Spring Boot ConfigurationProperties 与静态门面访问。
 * Abyss landing quest and points reset related configuration supporting Spring Boot ConfigurationProperties and static facade access.
 */
@Component
@ConfigurationProperties(prefix = "gameserver")
public class AbyssLandingConfig {
	/**
	 * 是否启用欧比斯登陆点任务重置。
	 * Whether abyss landing quest reset is enabled.
	 */
	@Property(key = "gameserver.landing.quest.reset.enable", defaultValue = "true")
	public static boolean ABYSS_LANDING_QUEST_RESET_ENABLED;
	/**
	 * 是否启用欧比斯登陆点积分重置。
	 * Whether abyss landing points reset is enabled.
	 */
	@Property(key = "gameserver.landing.points.reset.enable", defaultValue = "true")
	public static boolean ABYSS_LANDING_POINTS_RESET_ENABLED;
	/**
	 * 欧比斯登陆点任务重置 Cron 时间。
	 * Cron schedule for abyss landing quest reset.
	 */
	@Property(key = "gameserver.landing.quest.reset.time", defaultValue = "0 0 12 ? * MON *")
	public static String ABYSS_LANDING_QUEST_RESET_TIME;
	/**
	 * 欧比斯登陆点积分重置 Cron 时间。
	 * Cron schedule for abyss landing points reset.
	 */
	@Property(key = "gameserver.landing.points.reset.time", defaultValue = "0 0 0 ? * MON *")
	public static String ABYSS_LANDING_POINTS_RESET_TIME;
	/**
	 * 欧比斯登陆点任务奖励倍率。
	 * Abyss landing quest reward rate.
	 */
	@Property(key = "gameserver.landing.quest.rate", defaultValue = "1")
	public static int ABYSS_LANDING_QUEST_RATE;

	@Getter
	private final LandingProperties landing = new LandingProperties();

	@Getter
	public static class LandingProperties {
		private final QuestProperties quest = new QuestProperties();
		private final PointsProperties points = new PointsProperties();

		@Getter
		public static class QuestProperties {
			private final ResetProperties reset = new ResetProperties();

			public int getRate() {
				return ABYSS_LANDING_QUEST_RATE;
			}

			public void setRate(int rate) {
				ABYSS_LANDING_QUEST_RATE = rate;
			}

			public static class ResetProperties {
				public boolean isEnable() {
					return ABYSS_LANDING_QUEST_RESET_ENABLED;
				}

				public void setEnable(boolean enable) {
					ABYSS_LANDING_QUEST_RESET_ENABLED = enable;
				}

				public String getTime() {
					return ABYSS_LANDING_QUEST_RESET_TIME;
				}

				public void setTime(String time) {
					ABYSS_LANDING_QUEST_RESET_TIME = time;
				}
			}
		}

		@Getter
		public static class PointsProperties {
			private final ResetProperties reset = new ResetProperties();

			public static class ResetProperties {
				public boolean isEnable() {
					return ABYSS_LANDING_POINTS_RESET_ENABLED;
				}

				public void setEnable(boolean enable) {
					ABYSS_LANDING_POINTS_RESET_ENABLED = enable;
				}

				public String getTime() {
					return ABYSS_LANDING_POINTS_RESET_TIME;
				}

				public void setTime(String time) {
					ABYSS_LANDING_POINTS_RESET_TIME = time;
				}
			}
		}
	}
}
