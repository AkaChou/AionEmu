package com.aionemu.gameserver.services.siegeservice;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.siege.SiegeNpc;
import com.aionemu.gameserver.model.siege.SiegeRace;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * 攻城计数器，汇总伤害与欧比斯点数。
 * Siege counter aggregating damage and abyss points.
 */
public class SiegeCounter {
	private final Map<SiegeRace, SiegeRaceCounter> siegeRaceCounters = Maps.newHashMap();

	public SiegeCounter() {
		siegeRaceCounters.put(SiegeRace.ELYOS, new SiegeRaceCounter(SiegeRace.ELYOS));
		siegeRaceCounters.put(SiegeRace.ASMODIANS, new SiegeRaceCounter(SiegeRace.ASMODIANS));
		siegeRaceCounters.put(SiegeRace.BALAUR, new SiegeRaceCounter(SiegeRace.BALAUR));
	}

	/**
	 * 累计伤害。
	 * Adds damage.
	 *
	 * creature
	 * damage
	 */
	public void addDamage(Creature creature, int damage) {
		SiegeRace siegeRace;
		if (creature instanceof Player) {
			siegeRace = SiegeRace.getByRace(((Player) creature).getRace());
		} else if (creature instanceof SiegeNpc) {
			siegeRace = ((SiegeNpc) creature).getSiegeRace();
		} else {
			return;
		}
		siegeRaceCounters.get(siegeRace).addPoints(creature, damage);
	}

	/**
	 * 累计欧比斯点数。
	 * Adds abyss points.
	 *
	 * @param player 玩家 / player
	 * @param ap 欧比斯点 / ap
	 */
	public void addAbyssPoints(Player player, int ap) {
		SiegeRace sr = SiegeRace.getByRace(player.getRace());
		siegeRaceCounters.get(sr).addAbyssPoints(player, ap);
	}

	/**
	 * getRaceCounter 方法。
	 * getRaceCounter method.
	 *
	 * 阵营 / race
	 * result
	 */
	public SiegeRaceCounter getRaceCounter(SiegeRace race) {
		return siegeRaceCounters.get(race);
	}

	/**
	 * 累计种族伤害。
	 * Adds race damage.
	 *
	 * 阵营 / race
	 * damage
	 */
	public void addRaceDamage(SiegeRace race, int damage) {
		getRaceCounter(race).addTotalDamage(damage);
	}

	/**
	 * getWinnerRaceCounter 方法。
	 * getWinnerRaceCounter method.
	 * result
	 */
	public SiegeRaceCounter getWinnerRaceCounter() {
		List<SiegeRaceCounter> list = Lists.newArrayList(siegeRaceCounters.values());
		Collections.sort(list);
		return list.get(0);
	}
}