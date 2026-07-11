package com.aionemu.gameserver.model.gameobjects.player;

import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

import com.aionemu.gameserver.model.templates.event.AtreianPassport;

/**
 * 玩家 Passports 游戏对象。
 * Player Passports game object.
 *
 * @author Ranastic
 */
public class PlayerPassports {

	private final SortedMap<Integer, AtreianPassport> passports = new TreeMap<Integer, AtreianPassport>();

	/** 添加 passport / Adds passport */
	public synchronized boolean addPassport(int id, AtreianPassport ap) {
		if (passports.containsKey(id)) {
			return false;
		}
		passports.put(id, ap);
		return true;
	}

	/** 移除 passport / Removes passport */
	public synchronized boolean removePassport(int id) {
		if (passports.containsKey(id)) {
			passports.remove(id);
			return true;
		}
		return false;
	}

	/** 返回全部通行证 / Returns the all passports*/
	public Collection<AtreianPassport> getAllPassports() {
		return passports.values();
	}
}
