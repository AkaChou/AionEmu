package com.aionemu.gameserver.configs.main;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.aionemu.commons.configuration.Property;

import lombok.Getter;

/**
 * 大天使（Arch Daeva）相关配置，支持 Spring Boot ConfigurationProperties 与静态门面访问。
 * Arch Daeva related configuration supporting Spring Boot ConfigurationProperties and static facade access.
 */
@Component
@ConfigurationProperties(prefix = "gameserver")
public class ArchDaevaConfig {

	/**
	 * 创造点数（CP）上限。
	 * Maximum creativity points (CP) limit.
	 */
	@Property(key = "gameserver.max.cp.limit", defaultValue = "1000")
	public static int CP_LIMIT_MAX;

	/**
	 * 是否启用高等大天使禁用物品限制。
	 * Whether item restriction for high Daeva is enabled.
	 */
	@Property(key = "gameserver.item.not.for.highdaeva.enable", defaultValue = "false")
	public static boolean ITEM_NOT_FOR_HIGHDAEVA_ENABLE;

	@Getter
	private final MaxProperties max = new MaxProperties();
	@Getter
	private final ItemProperties item = new ItemProperties();

	@Getter
	public static class MaxProperties {
		private final CpProperties cp = new CpProperties();

		public static class CpProperties {
			public int getLimit() {
				return CP_LIMIT_MAX;
			}

			public void setLimit(int limit) {
				CP_LIMIT_MAX = limit;
			}
		}
	}

	@Getter
	public static class ItemProperties {
		private final NotProperties not = new NotProperties();

		public static class NotProperties {
			private final ForProperties forProperties = new ForProperties();

			/**
			 * 属性名必须与键段 for 对齐；for 为 Java 关键字，仅能以方法名形式出现。
			 * The accessor name must match the key segment "for"; the keyword can only appear in a method name.
			 */
			public ForProperties getFor() {
				return forProperties;
			}

			@Getter
			public static class ForProperties {
				private final HighdaevaProperties highdaeva = new HighdaevaProperties();

				public static class HighdaevaProperties {
					public boolean isEnable() {
						return ITEM_NOT_FOR_HIGHDAEVA_ENABLE;
					}

					public void setEnable(boolean enable) {
						ITEM_NOT_FOR_HIGHDAEVA_ENABLE = enable;
					}
				}
			}
		}
	}
}
