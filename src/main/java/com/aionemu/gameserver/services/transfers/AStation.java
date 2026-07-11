package com.aionemu.gameserver.services.transfers;

/**
 * A-Station（跨服站点）配置数据，描述目标服务器 ID、图标集以及等级限制。
 * A-Station (cross-server station) configuration holding server ID, icon set and level limits.
 *
 * @author Ranastic
 */
public class AStation {
	private int serverId;
	private int iconSet;
	private int minlevel, maxlevel;

	/**
	 * 构造跨服站点配置。
	 * Construct a cross-server station configuration.
	 *
	 * Server ID
	 * @param sendIcon  是否发送主服图标（true=主服 257，false=正式服 513） / Whether to use master-server icon set
	 * Minimum level
	 * Maximum level
	 */
	public AStation(int serverId, boolean sendIcon, int minLevel, int maxLevel) {
		this.serverId = serverId;
		this.iconSet = sendIcon ? 257 : 513; // 257 Master Server / 513 Live Server
		this.minlevel = minLevel;
		this.maxlevel = maxLevel;
	}

	/**
	 * 获取服务器 ID。
	 * Get the server ID.
	 *
	 * Server ID
	 */
	public int getServerId() {
		return serverId;
	}

	/**
	 * 获取图标集标识。
	 * Get the icon set identifier.
	 *
	 * Icon set
	 */
	public int getIconSet() {
		return iconSet;
	}

	/**
	 * 获取最低等级限制。
	 * Get the minimum level limit.
	 *
	 * Minimum level
	 */
	public int getMinLevel() {
		return minlevel;
	}

	/**
	 * 获取最高等级限制。
	 * Get the maximum level limit.
	 *
	 * Maximum level
	 */
	public int getMaxLevel() {
		return maxlevel;
	}
}
