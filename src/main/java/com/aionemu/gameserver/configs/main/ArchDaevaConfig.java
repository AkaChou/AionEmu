package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;

/**
 * 大天使（Arch Daeva）相关配置。
 * Arch Daeva related configuration.
 */
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
}
