package com.aionemu.gameserver.controllers.attack;

import java.util.HashMap;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.AionObject;

/**
 * 仇恨信息：记录攻击者对本单位的仇恨值与累计伤害。
 * Aggro info: stores an attacker's hate and accumulated damage against this unit.
 *
 * @author ATracer, Sarynth
 */
public class AggroInfo {

	/** 攻击者 / Attacker */
	private AionObject attacker;
	/** 仇恨值 / Hate value */
	private int hate;
	/** 累计伤害 / Accumulated damage */
	private int damage;
	private long volatileHateSequence;
	private final Map<Long, Integer> volatileHate = new HashMap<>();

	/**
	 * 以指定攻击者创建仇恨条目。
	 * Creates an aggro entry for the given attacker.
	 *
	 * attacker
	 */
	AggroInfo(AionObject attacker) {
		this.attacker = attacker;
	}

	/**
	 * 返回该条目对应的攻击者。
	 * Returns the attacker associated with this entry.
	 *
	 * attacker
	 */
	public AionObject getAttacker() {
		return attacker;
	}

	/**
	 * 累加伤害，结果不会低于 0。
	 * Adds damage; the total is clamped to be non-negative.
	 *
	 * @param damage 本次伤害增量 / damage delta to add
	 */
	public synchronized void addDamage(int damage) {
		this.damage += damage;
		if (this.damage < 0) {
			this.damage = 0;
		}
	}

	/**
	 * 累加仇恨，结果不会低于 1。
	 * Adds hate; the total is clamped to be at least 1.
	 *
	 * @param damage 本次仇恨增量 / hate delta to add
	 */
	public synchronized void addHate(int damage) {
		this.hate += damage;
		if (this.hate < 1) {
			this.hate = 1;
		}
	}

	/**
	 * 返回当前仇恨值。
	 * Returns the current hate value.
	 *
	 * hate
	 */
	public synchronized int getHate() {
		return this.hate;
	}

	/**
	 * 直接设置仇恨值。
	 * Sets the hate value directly.
	 *
	 * @param hate 新的仇恨值 / new hate value
	 */
	public synchronized void setHate(int hate) {
		this.hate = hate;
		volatileHate.clear();
	}

	public synchronized long addVolatileHate(int hate) {
		long token = ++volatileHateSequence;
		int previousHate = this.hate;
		addHate(hate);
		volatileHate.put(token, this.hate - previousHate);
		return token;
	}

	public synchronized void removeVolatileHate(long token) {
		Integer hate = volatileHate.remove(token);
		if (hate != null) {
			this.hate = Math.max(0, this.hate - hate);
		}
	}

	public synchronized void resetVolatileHate() {
		long total = 0;
		for (int hate : volatileHate.values()) {
			total += hate;
		}
		this.hate = (int) Math.max(0, Math.min(Integer.MAX_VALUE, this.hate - total));
		volatileHate.clear();
	}

	/**
	 * 返回累计伤害。
	 * Returns the accumulated damage.
	 *
	 * damage
	 */
	public synchronized int getDamage() {
		return this.damage;
	}

	/**
	 * 直接设置累计伤害。
	 * Sets the accumulated damage directly.
	 *
	 * @param damage 新的累计伤害 / new damage value
	 */
	public synchronized void setDamage(int damage) {
		this.damage = damage;
	}
}
