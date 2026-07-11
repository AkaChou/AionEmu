package com.aionemu.gameserver.model.gameobjects.player;

import java.util.HashMap;
import java.util.Map;

/**
 * 制作冷却列表。
 * Craft Cooldown List game object.
 *
 * @author synchro2
 */
public class CraftCooldownList {

	private Map<Integer, Long> craftCooldowns;

	CraftCooldownList(Player owner) {
	}

	/**
	 * @param delayId 是否可 craft / 是否可 craft。 / Whether can craft / Whether can craft
	 */
	public boolean isCanCraft(int delayId) {
		if (craftCooldowns == null || !craftCooldowns.containsKey(delayId)) {
			return true;
		}
		Long coolDown = craftCooldowns.get(delayId);
		if (coolDown == null) {
			return true;
		}
		if (coolDown < System.currentTimeMillis()) {
			craftCooldowns.remove(delayId);
			return true;
		}
		return false;
	}

	/** 获取制作冷却。 / Returns the craft cooldown. */
	public long getCraftCooldown(int delayId) {
		if (craftCooldowns == null || !craftCooldowns.containsKey(delayId)) {
			return 0;
		}
		return craftCooldowns.get(delayId);
	}

	/** 返回 craft cool downs / Returns the craft cool downs */
	public Map<Integer, Long> getCraftCoolDowns() {
		return craftCooldowns;
	}

	/** 设置 craft cool downs / Sets the craft cool downs */
	public void setCraftCoolDowns(Map<Integer, Long> craftCoolDowns) {
		this.craftCooldowns = craftCoolDowns;
	}

	/** 添加制作冷却。 / Adds craft cooldown. */
	public void addCraftCooldown(int delayId, int delay) {
		if (craftCooldowns == null) {
			craftCooldowns = new HashMap<Integer, Long>();
		}

		long nextUseTime = System.currentTimeMillis() + (delay * 1000);
		craftCooldowns.put(delayId, nextUseTime);
	}
}
