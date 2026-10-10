package com.aionemu.gameserver.configs.main;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.aionemu.commons.configuration.Property;

import lombok.Getter;

/**
 * A-Station（跨服）相关配置，支持 Spring Boot ConfigurationProperties 与静态门面访问。
 * A-Station (cross-server) related configuration supporting Spring Boot ConfigurationProperties and static facade access.
 * @author Ranastic
 */
@Component
@ConfigurationProperties(prefix = "a")
public class AStationConfig {
	/**
	 * A-Station 目标服务器 ID。
	 * A-Station target server ID.
	 */
	@Property(key = "a.station.server.id", defaultValue = "2")
	public static int A_STATION_SERVER_ID;
	/**
	 * A-Station 最大角色等级。
	 * Maximum character level for A-Station.
	 */
	@Property(key = "a.station.max.level", defaultValue = "83")
	public static int A_STATION_MAX_LEVEL;
	/**
	 * 是否启用 A-Station。
	 * Whether A-Station is enabled.
	 */
	@Property(key = "a.station.enable", defaultValue = "true")
	public static boolean A_STATION_ENABLE;

	@Getter
	private final StationProperties station = new StationProperties();

	@Getter
	public static class StationProperties {
		private final ServerProperties server = new ServerProperties();
		private final MaxProperties max = new MaxProperties();

		public boolean isEnable() {
			return A_STATION_ENABLE;
		}

		public void setEnable(boolean enable) {
			A_STATION_ENABLE = enable;
		}

		@Getter
		public static class ServerProperties {
			public int getId() {
				return A_STATION_SERVER_ID;
			}

			public void setId(int id) {
				A_STATION_SERVER_ID = id;
			}
		}

		@Getter
		public static class MaxProperties {
			public int getLevel() {
				return A_STATION_MAX_LEVEL;
			}

			public void setLevel(int level) {
				A_STATION_MAX_LEVEL = level;
			}
		}
	}
}
