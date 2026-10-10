package com.aionemu.gameserver.configs.main;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.aionemu.commons.configuration.Property;

import lombok.Getter;

/**
 * PvP 模式扩展（如战场）相关配置，支持 Spring Boot ConfigurationProperties 与静态门面访问。
 * PvP mode extensions (e.g. battleground) related configuration supporting Spring Boot ConfigurationProperties and static facade access.
 * Created by wanke on 12/02/2017.
 */
@Component
@ConfigurationProperties(prefix = "gameserver")
public class PvPModConfig {
	/**
	 * 是否启用战场模式。
	 * Whether battleground mode is enabled.
	 */
	@Property(key = "gameserver.pvp.mod.bg.enabled", defaultValue = "true")
	public static boolean BG_ENABLED;

	@Getter
	private final PvpProperties pvp = new PvpProperties();

	@Getter
	public static class PvpProperties {
		private final ModProperties mod = new ModProperties();

		@Getter
		public static class ModProperties {
			private final BgProperties bg = new BgProperties();

			public static class BgProperties {
				public boolean isEnabled() {
					return BG_ENABLED;
				}

				public void setEnabled(boolean enabled) {
					BG_ENABLED = enabled;
				}
			}
		}
	}
}
