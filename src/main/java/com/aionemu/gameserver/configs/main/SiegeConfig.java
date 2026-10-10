package com.aionemu.gameserver.configs.main;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.aionemu.commons.configuration.Property;

import lombok.Getter;

/**
 * 要塞攻城系统相关配置，支持 Spring Boot ConfigurationProperties 与静态门面访问。
 * Fortress siege system related configuration supporting Spring Boot ConfigurationProperties and static facade access.
 */
@Component
@ConfigurationProperties(prefix = "gameserver")
public class SiegeConfig {
	/**
	 * 是否启用攻城系统。
	 * Whether the siege system is enabled.
	 */
	@Property(key = "gameserver.siege.enable", defaultValue = "true")
	public static boolean SIEGE_ENABLED;

	/**
	 * 攻城勋章奖励倍率。
	 * Siege medal reward rate.
	 */
	@Property(key = "gameserver.siege.medal.rate", defaultValue = "1")
	public static int SIEGE_MEDAL_RATE;

	/**
	 * 是否启用攻城护盾。
	 * Whether siege shields are enabled.
	 */
	@Property(key = "gameserver.siege.shield.enable", defaultValue = "true")
	public static boolean SIEGE_SHIELD_ENABLED;

	/**
	 * 是否启用龙族自动突袭。
	 * Whether Balaur auto assault is enabled.
	 */
	@Property(key = "gameserver.siege.assault.enable", defaultValue = "false")
	public static boolean BALAUR_AUTO_ASSAULT;

	/**
	 * 龙族突袭强度倍率。
	 * Balaur assault rate multiplier.
	 */
	@Property(key = "gameserver.siege.assault.rate", defaultValue = "1")
	public static float BALAUR_ASSAULT_RATE;

	/**
	 * 是否启用自动分配攻城阵营。
	 * Whether auto siege race assignment is enabled.
	 */
	@Property(key = "gameserver.auto.siege.race", defaultValue = "false")
	public static boolean SIEGE_AUTO_RACE;

	/**
	 * 自动攻城地点 ID 列表。
	 * Auto siege location ID list.
	 */
	@Property(key = "gameserver.auto.siege.id", defaultValue = "")
	public static String SIEGE_AUTO_LOCID;

	@Getter
	private final SiegeProperties siege = new SiegeProperties();
	@Getter
	private final AutoProperties auto = new AutoProperties();

	@Getter
	public static class SiegeProperties {
		private final MedalProperties medal = new MedalProperties();
		private final ShieldProperties shield = new ShieldProperties();
		private final AssaultProperties assault = new AssaultProperties();

		public boolean isEnable() {
			return SIEGE_ENABLED;
		}

		public void setEnable(boolean enable) {
			SIEGE_ENABLED = enable;
		}

		public static class MedalProperties {
			public int getRate() {
				return SIEGE_MEDAL_RATE;
			}

			public void setRate(int rate) {
				SIEGE_MEDAL_RATE = rate;
			}
		}

		public static class ShieldProperties {
			public boolean isEnable() {
				return SIEGE_SHIELD_ENABLED;
			}

			public void setEnable(boolean enable) {
				SIEGE_SHIELD_ENABLED = enable;
			}
		}

		public static class AssaultProperties {
			public boolean isEnable() {
				return BALAUR_AUTO_ASSAULT;
			}

			public void setEnable(boolean enable) {
				BALAUR_AUTO_ASSAULT = enable;
			}

			public float getRate() {
				return BALAUR_ASSAULT_RATE;
			}

			public void setRate(float rate) {
				BALAUR_ASSAULT_RATE = rate;
			}
		}
	}

	@Getter
	public static class AutoProperties {
		private final AutoSiegeProperties siege = new AutoSiegeProperties();

		public static class AutoSiegeProperties {
			public boolean isRace() {
				return SIEGE_AUTO_RACE;
			}

			public void setRace(boolean race) {
				SIEGE_AUTO_RACE = race;
			}

			public String getId() {
				return SIEGE_AUTO_LOCID;
			}

			public void setId(String id) {
				SIEGE_AUTO_LOCID = id;
			}
		}
	}
}
