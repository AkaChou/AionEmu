package com.aionemu.gameserver.configs.main;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.aionemu.commons.configuration.Property;

import lombok.Getter;

/**
 * 交易行相关配置，支持 Spring Boot ConfigurationProperties 与静态门面访问。
 * Broker (auction house) related configuration supporting Spring Boot ConfigurationProperties and static facade access.
 */
@Component
@ConfigurationProperties(prefix = "gameserver")
public class BrokerConfig {
	/**
	 * 过期物品检查间隔。
	 * Interval for checking expired broker items.
	 */
	@Property(key = "gameserver.broker.time.check.expired.items.interval", defaultValue = "60")
	public static int CHECK_EXPIRED_ITEMS_INTERVAL;
	/**
	 * 交易行反作弊惩罚类型。
	 * Broker anti-hack punishment type.
	 */
	@Property(key = "gameserver.broker.anti.hack.punishment", defaultValue = "0")
	public static int ANTI_HACK_PUNISHMENT;
	/**
	 * 交易行物品过期时间。
	 * Broker items expire time.
	 */
	@Property(key = "gameserver.broker.items.expire.time", defaultValue = "8")
	public static int ITEMS_EXPIRE_TIME;

	@Getter
	private final BrokerProperties broker = new BrokerProperties();

	@Getter
	public static class BrokerProperties {
		private final TimeProperties time = new TimeProperties();
		private final AntiProperties anti = new AntiProperties();
		private final ItemsProperties items = new ItemsProperties();

		@Getter
		public static class TimeProperties {
			private final CheckProperties check = new CheckProperties();

			@Getter
			public static class CheckProperties {
				private final ExpiredProperties expired = new ExpiredProperties();

				@Getter
				public static class ExpiredProperties {
					private final ItemsProperties items = new ItemsProperties();

					public static class ItemsProperties {
						public int getInterval() {
							return CHECK_EXPIRED_ITEMS_INTERVAL;
						}

						public void setInterval(int interval) {
							CHECK_EXPIRED_ITEMS_INTERVAL = interval;
						}
					}
				}
			}
		}

		@Getter
		public static class AntiProperties {
			private final HackProperties hack = new HackProperties();

			public static class HackProperties {
				public int getPunishment() {
					return ANTI_HACK_PUNISHMENT;
				}

				public void setPunishment(int punishment) {
					ANTI_HACK_PUNISHMENT = punishment;
				}
			}
		}

		@Getter
		public static class ItemsProperties {
			private final ExpireProperties expire = new ExpireProperties();

			public static class ExpireProperties {
				public int getTime() {
					return ITEMS_EXPIRE_TIME;
				}

				public void setTime(int time) {
					ITEMS_EXPIRE_TIME = time;
				}
			}
		}
	}
}
