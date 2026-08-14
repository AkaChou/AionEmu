package com.aionemu.gameserver.services.transfers;

import lombok.Getter;

/**
 * A-Station（跨服站点）配置数据，描述目标服务器 ID、图标集以及等级限制。
 * A-Station (cross-server station) configuration holding server ID, icon set and level limits.
 *
 * @author Ranastic
 */
public class AStation {
	@Getter
	private int serverId;
	@Getter
	private int iconSet;
	private int minlevel, maxlevel;

	/**
	 * 构造跨服站点配置。
	 * Construct a cross-server station configuration.
	 *
	 * @param serverId 目标服务器 ID / target server id
	 * @param sendIcon 是否使用主服图标集（true=主服 257，false=正式服 513） / Whether to use master-server icon set
	 * @param minLevel 最低等级 / minimum level
	 * @param maxLevel 最高等级 / maximum level
	 */
	public AStation(int serverId, boolean sendIcon, int minLevel, int maxLevel) {
		this.serverId = serverId;
		this.iconSet = sendIcon ? 257 : 513; // 257 主服 / 513 正式服 / 257 Master Server / 513 Live Server
		this.minlevel = minLevel;
		this.maxlevel = maxLevel;
	}

	/**
	 * 获取最低等级限制。
	 * Get the minimum level limit.
	 *
	 * @return 最低等级 / minimum level
	 */
	public int getMinLevel() {
		return minlevel;
	}

	/**
	 * 获取最高等级限制。
	 * Get the maximum level limit.
	 *
	 * @return 最高等级 / maximum level
	 */
	public int getMaxLevel() {
		return maxlevel;
	}
}
