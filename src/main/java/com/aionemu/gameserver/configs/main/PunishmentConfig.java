package com.aionemu.gameserver.configs.main;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.aionemu.commons.configuration.Property;

import lombok.Getter;

/**
 * 自动惩罚机制相关配置，支持 Spring Boot ConfigurationProperties 与静态门面访问。
 * Automatic punishment mechanism related configuration supporting Spring Boot ConfigurationProperties and static facade access.
 * @author synchro2
 */
@Component
@ConfigurationProperties(prefix = "gameserver")
public class PunishmentConfig {

	/**
	 * 是否启用自动惩罚。
	 * Whether automatic punishment is enabled.
	 */
	@Property(key = "gameserver.punishment.enable", defaultValue = "false")
	public static boolean PUNISHMENT_ENABLE;
	/**
	 * 惩罚类型。
	 * Punishment type.
	 */
	@Property(key = "gameserver.punishment.type", defaultValue = "1")
	public static int PUNISHMENT_TYPE;
	/**
	 * 惩罚时长（分钟）。
	 * Punishment duration in minutes.
	 */
	@Property(key = "gameserver.punishment.time", defaultValue = "1440")
	public static int PUNISHMENT_TIME;
	/**
	 * 惩罚时减少的欧比斯点数。
	 * Abyss points reduced by punishment.
	 */
	@Property(key = "gameserver.punishment.reduceap", defaultValue = "0")
	public static int PUNISHMENT_REDUCEAP;

	@Getter
	private final PunishmentProperties punishment = new PunishmentProperties();

	@Getter
	public static class PunishmentProperties {
		public boolean isEnable() {
			return PUNISHMENT_ENABLE;
		}

		public void setEnable(boolean enable) {
			PUNISHMENT_ENABLE = enable;
		}

		public int getType() {
			return PUNISHMENT_TYPE;
		}

		public void setType(int type) {
			PUNISHMENT_TYPE = type;
		}

		public int getTime() {
			return PUNISHMENT_TIME;
		}

		public void setTime(int time) {
			PUNISHMENT_TIME = time;
		}

		public int getReduceap() {
			return PUNISHMENT_REDUCEAP;
		}

		public void setReduceap(int reduceap) {
			PUNISHMENT_REDUCEAP = reduceap;
		}
	}
}
