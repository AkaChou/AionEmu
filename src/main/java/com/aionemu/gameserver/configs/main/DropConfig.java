package com.aionemu.gameserver.configs.main;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.aionemu.commons.configuration.Property;

import lombok.Getter;

/**
 * 掉落相关配置，支持 Spring Boot ConfigurationProperties 与静态门面访问。
 * Drop related configuration supporting Spring Boot ConfigurationProperties and static facade access.
 */
@Component
@ConfigurationProperties(prefix = "gameserver")
public class DropConfig {
	/**
	 * 是否禁用掉落衰减。
	 * Whether drop reduction is disabled.
	 */
	@Property(key = "gameserver.drop.reduction.disable", defaultValue = "false")
	public static boolean DISABLE_DROP_REDUCTION;

	/**
	 * 是否启用独特掉落全服公告。
	 * Whether unique drop announce is enabled.
	 */
	@Property(key = "gameserver.unique.drop.announce.enable", defaultValue = "true")
	public static boolean ENABLE_UNIQUE_DROP_ANNOUNCE;

	/**
	 * 禁用掉落衰减的区域列表。
	 * Zones where drop reduction is disabled.
	 */
	@Property(key = "gameserver.drop.noreduction", defaultValue = "0")
	public static String DISABLE_DROP_REDUCTION_IN_ZONES;

	/**
	 * 是否启用全局掉落。
	 * Whether global drops are enabled.
	 */
	@Property(key = "gameserver.drop.enable.global.drops", defaultValue = "true")
	public static boolean ENABLE_GLOBAL_DROPS;

	/**
	 * 独立基纳掉落倍率。
	 * Dedicated Kinah drop multiplier.
	 */
	@Property(key = "gameserver.drop.kinah.rate", defaultValue = "1.0")
	public static float KINAH_RATE;

	/**
	 * 每个 NPC 允许的全局掉落数量上限。
	 * Maximum global drop items allowed per NPC.
	 */
	@Property(key = "gameserver.drop.max.global.drops.per.npc", defaultValue = "15")
	public static int MAX_GLOBAL_DROPS_PER_NPC;

	@Getter
	private final DropProperties drop = new DropProperties();
	@Getter
	private final UniqueProperties unique = new UniqueProperties();

	/** 校验掉落配置。 / Validates drop configuration. */
	public static void refresh() {
		if (!Float.isFinite(KINAH_RATE) || KINAH_RATE <= 0f) {
			throw new IllegalArgumentException("Kinah drop rate must be finite and greater than zero");
		}
	}

	@Getter
	public static class DropProperties {
		private final ReductionProperties reduction = new ReductionProperties();
		private final KinahProperties kinah = new KinahProperties();
		private final EnableProperties enable = new EnableProperties();
		private final MaxProperties max = new MaxProperties();

		public String getNoreduction() {
			return DISABLE_DROP_REDUCTION_IN_ZONES;
		}

		public void setNoreduction(String noreduction) {
			DISABLE_DROP_REDUCTION_IN_ZONES = noreduction;
		}

		public static class ReductionProperties {
			public boolean isDisable() {
				return DISABLE_DROP_REDUCTION;
			}

			public void setDisable(boolean disable) {
				DISABLE_DROP_REDUCTION = disable;
			}
		}

		public static class KinahProperties {
			public float getRate() {
				return KINAH_RATE;
			}

			public void setRate(float rate) {
				KINAH_RATE = rate;
			}
		}

		@Getter
		public static class EnableProperties {
			private final GlobalProperties global = new GlobalProperties();

			public static class GlobalProperties {
				public boolean isDrops() {
					return ENABLE_GLOBAL_DROPS;
				}

				public void setDrops(boolean drops) {
					ENABLE_GLOBAL_DROPS = drops;
				}
			}
		}

		@Getter
		public static class MaxProperties {
			private final GlobalProperties global = new GlobalProperties();

			@Getter
			public static class GlobalProperties {
				private final DropsProperties drops = new DropsProperties();

				@Getter
				public static class DropsProperties {
					private final PerProperties per = new PerProperties();

					public static class PerProperties {
						public int getNpc() {
							return MAX_GLOBAL_DROPS_PER_NPC;
						}

						public void setNpc(int npc) {
							MAX_GLOBAL_DROPS_PER_NPC = npc;
						}
					}
				}
			}
		}
	}

	@Getter
	public static class UniqueProperties {
		private final UniqueDropProperties drop = new UniqueDropProperties();

		@Getter
		public static class UniqueDropProperties {
			private final AnnounceProperties announce = new AnnounceProperties();

			public static class AnnounceProperties {
				public boolean isEnable() {
					return ENABLE_UNIQUE_DROP_ANNOUNCE;
				}

				public void setEnable(boolean enable) {
					ENABLE_UNIQUE_DROP_ANNOUNCE = enable;
				}
			}
		}
	}
}
