package com.aionemu.gameserver.configs.main;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.aionemu.commons.configuration.Property;

import lombok.Getter;

/**
 * 排行榜更新相关配置，支持 Spring Boot ConfigurationProperties 与静态门面访问。
 * Top ranking update related configuration supporting Spring Boot ConfigurationProperties and static facade access.
 */
@Component
@ConfigurationProperties(prefix = "gameserver")
public class RankingConfig {
	/**
	 * 是否使用自定义排行榜更新设置。
	 * Whether custom top ranking update settings are used.
	 */
	@Property(key = "gameserver.top.ranking.update.setting", defaultValue = "true")
	public static boolean TOP_RANKING_UPDATE_SETTING;

	/**
	 * 排行榜更新 Cron 规则。
	 * Cron rule for top ranking update.
	 */
	@Property(key = "gameserver.top.ranking.update.hour", defaultValue = "0 0 */2 ? * *")
	public static String TOP_RANKING_UPDATE_RULE;

	/**
	 * 排行榜更新备用分钟间隔。
	 * Alternative ranking update interval in minutes.
	 */
	@Property(key = "gameserver.top.ranking.update.minute", defaultValue = "10")
	public static int TOP_RANKING_UPDATE_RULE2;

	/**
	 * 排行榜统计允许的最大离线天数（0 表示不限制）。
	 * Max offline days allowed in ranking (0 means unlimited).
	 */
	@Property(key = "gameserver.top.ranking.max.offline.days", defaultValue = "0")
	public static int TOP_RANKING_MAX_OFFLINE_DAYS;

	@Getter
	private final TopProperties top = new TopProperties();

	@Getter
	public static class TopProperties {
		private final RankingProperties ranking = new RankingProperties();

		@Getter
		public static class RankingProperties {
			private final UpdateProperties update = new UpdateProperties();
			private final MaxProperties max = new MaxProperties();

			public static class UpdateProperties {
				public boolean isSetting() {
					return TOP_RANKING_UPDATE_SETTING;
				}

				public void setSetting(boolean setting) {
					TOP_RANKING_UPDATE_SETTING = setting;
				}

				public String getHour() {
					return TOP_RANKING_UPDATE_RULE;
				}

				public void setHour(String hour) {
					TOP_RANKING_UPDATE_RULE = hour;
				}

				public int getMinute() {
					return TOP_RANKING_UPDATE_RULE2;
				}

				public void setMinute(int minute) {
					TOP_RANKING_UPDATE_RULE2 = minute;
				}
			}

			@Getter
			public static class MaxProperties {
				private final OfflineProperties offline = new OfflineProperties();

				public static class OfflineProperties {
					public int getDays() {
						return TOP_RANKING_MAX_OFFLINE_DAYS;
					}

					public void setDays(int days) {
						TOP_RANKING_MAX_OFFLINE_DAYS = days;
					}
				}
			}
		}
	}
}
