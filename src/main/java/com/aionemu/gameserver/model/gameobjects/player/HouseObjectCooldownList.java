package com.aionemu.gameserver.model.gameobjects.player;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

/**
 * 房屋对象冷却列表。
 * House Object Cooldown List game object.
 *
 * @author Rolandas
 */
@Getter
public class HouseObjectCooldownList {

	/** 返回 house object cooldowns / Returns the house object cooldowns */
	private Map<Integer, Long> houseObjectCooldowns;

	HouseObjectCooldownList(Player owner) {
	}

	/** 是否可使用房屋物件 / Whether can use object */
	public boolean isCanUseObject(int objectId) {
		if (houseObjectCooldowns == null || !houseObjectCooldowns.containsKey(objectId)) {
			return true;
		}
		Long coolDown = houseObjectCooldowns.get(objectId);
		if (coolDown == null) {
			return true;
		}
		if (coolDown < System.currentTimeMillis()) {
			houseObjectCooldowns.remove(objectId);
			return true;
		}
		return false;
	}

	/** 获取房屋对象冷却。 / Returns the house object cooldown. */
	public long getHouseObjectCooldown(int objectId) {
		if (houseObjectCooldowns == null || !houseObjectCooldowns.containsKey(objectId)) {
			return 0;
		}
		return houseObjectCooldowns.get(objectId);
	}

	/** 设置 house object cooldowns / Sets the house object cooldowns */
	public void setHouseObjectCooldowns(Map<Integer, Long> houseObjectCooldowns) {
		this.houseObjectCooldowns = houseObjectCooldowns;
	}

	/** 添加房屋对象冷却。 / Adds house object cooldown. */
	public void addHouseObjectCooldown(int objectId, int delay) {
		if (houseObjectCooldowns == null) {
			houseObjectCooldowns = new HashMap<Integer, Long>();
		}

		long nextUseTime = System.currentTimeMillis() + (delay * 1000L);
		houseObjectCooldowns.put(objectId, nextUseTime);
	}

	/** 返回 reuse delay / Returns the reuse delay */
	public int getReuseDelay(int objectId) {
		if (isCanUseObject(objectId)) {
			return 0;
		}
		long cd = getHouseObjectCooldown(objectId);
		int delay = (int) ((cd - System.currentTimeMillis()) / 1000);
		return delay;
	}
}
