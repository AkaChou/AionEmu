package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 周期性数据保存间隔相关配置，支持 Spring Boot ConfigurationProperties 与静态门面访问。
 * Periodic data save interval related configuration supporting Spring Boot ConfigurationProperties and static facade access.
 */
@Component
@ConfigurationProperties(prefix = "gameserver.periodicsave")
public class PeriodicSaveConfig {

	/**
	 * 玩家通用数据保存间隔（秒）。
	 * Player general data save interval in seconds.
	 */
	@Property(key = "gameserver.periodicsave.player.general", defaultValue = "900")
	public static int PLAYER_GENERAL;

	/**
	 * 玩家物品数据保存间隔（秒）。
	 * Player items save interval in seconds.
	 */
	@Property(key = "gameserver.periodicsave.player.items", defaultValue = "900")
	public static int PLAYER_ITEMS;

	/**
	 * 军团物品数据保存间隔（秒）。
	 * Legion items save interval in seconds.
	 */
	@Property(key = "gameserver.periodicsave.legion.items", defaultValue = "1200")
	public static int LEGION_ITEMS;

	/**
	 * 玩家宠物数据保存间隔（秒）。
	 * Player pets save interval in seconds.
	 */
	@Property(key = "gameserver.periodicsave.player.pets", defaultValue = "5")
	public static int PLAYER_PETS;

	private final PlayerProperties player = new PlayerProperties();
	private final LegionProperties legion = new LegionProperties();

	public PlayerProperties getPlayer() {
		return player;
	}

	public LegionProperties getLegion() {
		return legion;
	}

	public static class PlayerProperties {
		public int getGeneral() {
			return PLAYER_GENERAL;
		}

		public void setGeneral(int general) {
			PLAYER_GENERAL = general;
		}

		public int getItems() {
			return PLAYER_ITEMS;
		}

		public void setItems(int items) {
			PLAYER_ITEMS = items;
		}

		public int getPets() {
			return PLAYER_PETS;
		}

		public void setPets(int pets) {
			PLAYER_PETS = pets;
		}
	}

	public static class LegionProperties {
		public int getItems() {
			return LEGION_ITEMS;
		}

		public void setItems(int items) {
			LEGION_ITEMS = items;
		}
	}
}
