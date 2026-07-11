package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;

/**
 * PvP 模式扩展（如战场）相关配置。
 * PvP mode extensions (e.g. battleground) related configuration.
 *
 * Created by wanke on 12/02/2017.
 */
public class PvPModConfig {
	/**
	 * 是否启用战场模式。
	 * Whether battleground mode is enabled.
	 */
	@Property(key = "gameserver.pvp.mod.bg.enabled", defaultValue = "true")
	public static boolean BG_ENABLED;
}
