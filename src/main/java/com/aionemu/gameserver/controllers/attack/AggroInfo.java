package com.aionemu.gameserver.controllers.attack;

import java.util.HashMap;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.AionObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.AccessLevel;

/**
 * 仇恨信息：记录攻击者对本单位的仇恨值与累计伤害。
 * Aggro info: stores an attacker's hate and accumulated damage against this unit.
 *
 * @author ATracer, Sarynth
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class AggroInfo {

	/** 攻击者 / Attacker */
	@Getter
	private final AionObject attacker;
	/** 仇恨值 / Hate value */
	private int hate;
	/** 累计伤害 / Accumulated damage */
	private int damage;
	private long volatileHateSequence;
	private final Map<Long, Integer> volatileHate = new HashMap<>();

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
	 * @return 仇恨值 / hate
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
	 * @return 累计伤害 / damage
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
