package com.aionemu.gameserver.configs.main;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.aionemu.commons.configuration.Property;

import lombok.Getter;

/**
 * 世界区域与频道分线相关配置，支持 Spring Boot ConfigurationProperties 与静态门面访问。
 * World region and channel twin related configuration supporting Spring Boot ConfigurationProperties and static facade access.
 */
@Component
@ConfigurationProperties(prefix = "gameserver")
public class WorldConfig {
	/**
	 * 世界区域格子大小。
	 * World region grid size.
	 */
	@Property(key = "gameserver.world.region.size", defaultValue = "128")
	public static int WORLD_REGION_SIZE;
	/**
	 * 是否启用活跃区域追踪。
	 * Whether active region tracing is enabled.
	 */
	@Property(key = "gameserver.world.region.active.trace", defaultValue = "true")
	public static boolean WORLD_ACTIVE_TRACE;
	/**
	 * 是否模拟 A-Station 世界行为。
	 * Whether A-Station world behavior is emulated.
	 */
	@Property(key = "gameserver.world.emulate.a.station", defaultValue = "false")
	public static boolean WORLD_EMULATE_A_STATION;
	/**
	 * 常规地图最大频道数。
	 * Maximum twin/channel count for usual maps.
	 */
	@Property(key = "gameserver.world.max.twincount.usual", defaultValue = "1")
	public static int WORLD_MAX_TWINS_USUAL;
	/**
	 * 新手地图最大频道数（-1 表示不限制）。
	 * Maximum twin/channel count for beginner maps (-1 unlimited).
	 */
	@Property(key = "gameserver.world.max.twincount.beginner", defaultValue = "-1")
	public static int WORLD_MAX_TWINS_BEGINNER;

	@Getter
	private final WorldProperties world = new WorldProperties();

	@Getter
	public static class WorldProperties {
		private final RegionProperties region = new RegionProperties();
		private final EmulateProperties emulate = new EmulateProperties();
		private final MaxProperties max = new MaxProperties();

		@Getter
		public static class RegionProperties {
			private final ActiveProperties active = new ActiveProperties();

			public int getSize() {
				return WORLD_REGION_SIZE;
			}

			public void setSize(int size) {
				WORLD_REGION_SIZE = size;
			}

			public static class ActiveProperties {
				public boolean isTrace() {
					return WORLD_ACTIVE_TRACE;
				}

				public void setTrace(boolean trace) {
					WORLD_ACTIVE_TRACE = trace;
				}
			}
		}

		@Getter
		public static class EmulateProperties {
			private final AProperties a = new AProperties();

			public static class AProperties {
				public boolean isStation() {
					return WORLD_EMULATE_A_STATION;
				}

				public void setStation(boolean station) {
					WORLD_EMULATE_A_STATION = station;
				}
			}
		}

		@Getter
		public static class MaxProperties {
			private final TwincountProperties twincount = new TwincountProperties();

			public static class TwincountProperties {
				public int getUsual() {
					return WORLD_MAX_TWINS_USUAL;
				}

				public void setUsual(int usual) {
					WORLD_MAX_TWINS_USUAL = usual;
				}

				public int getBeginner() {
					return WORLD_MAX_TWINS_BEGINNER;
				}

				public void setBeginner(int beginner) {
					WORLD_MAX_TWINS_BEGINNER = beginner;
				}
			}
		}
	}
}
