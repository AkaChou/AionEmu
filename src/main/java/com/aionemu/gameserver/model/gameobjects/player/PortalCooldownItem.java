package com.aionemu.gameserver.model.gameobjects.player;

/**
 * 传送门冷却物品游戏对象。
 * Portal Cooldown Item game object.
 */

public class PortalCooldownItem {
	private int worldId;
	private int entryCount;
	private long cooldown;

	public PortalCooldownItem(int worldId, int entryCount, long cooldown) {
		this.worldId = worldId;
		this.entryCount = entryCount;
		this.cooldown = cooldown;
	}

	/** 返回世界 ID / Returns the world id */
	public int getWorldId() {
		return worldId;
	}

	/** 获取条目计数。 / Returns the entry count. */
	public int getEntryCount() {
		return entryCount;
	}

	/** 设置条目计数。 / Sets the entry count. */
	public void setEntryCount(int entryCount) {
		this.entryCount = entryCount;
	}

	/** 获取冷却。 / Returns the cooldown. */
	public long getCooldown() {
		return cooldown;
	}

	/** 设置冷却。 / Sets the cooldown. */
	public void setCooldown(long cooldown) {
		this.cooldown = cooldown;
	}
}
