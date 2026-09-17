package com.aionemu.gameserver.model.gameobjects.player;

import lombok.Data;
import lombok.AllArgsConstructor;

/**
 * 传送门冷却物品游戏对象。
 * Portal Cooldown Item game object.
 */

@Data
@AllArgsConstructor
public class PortalCooldownItem {
	/** 返回世界 ID / Returns the world id */
	private final int worldId;
	/** 获取条目计数。 / Returns the entry count. */
	private int entryCount;
	/** 获取冷却。 / Returns the cooldown. */
	private long cooldown;
}
