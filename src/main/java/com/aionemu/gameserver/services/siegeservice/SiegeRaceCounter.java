package com.aionemu.gameserver.services.siegeservice;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import com.aionemu.commons.utils.GenericValidator;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.siege.SiegeRace;
import com.aionemu.gameserver.model.team.legion.Legion;
import com.aionemu.gameserver.world.World;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * 攻城种族计数器，按种族累计伤害/AP 并比较排名。
 * Siege race counter accumulating damage/AP by race and ranking them.
 */
public class SiegeRaceCounter implements Comparable<SiegeRaceCounter> {
	private final AtomicLong totalDamage = new AtomicLong();
	private final Map<Integer, AtomicLong> playerDamageCounter = new LinkedHashMap<Integer, AtomicLong>();
	private final Map<Integer, AtomicLong> playerAPCounter = new LinkedHashMap<Integer, AtomicLong>();
	private final SiegeRace siegeRace;

	public SiegeRaceCounter(SiegeRace siegeRace) {
		this.siegeRace = siegeRace;
	}

	/**
	 * 累计点数。
	 * Adds points.
	 *
	 * creature
	 * damage
	 */
	public void addPoints(Creature creature, int damage) {
		addTotalDamage(damage);
		if (creature instanceof Player) {
			addPlayerDamage((Player) creature, damage);
		}
	}

	/**
	 * 累计总伤害。
	 * Adds total damage.
	 *
	 * damage
	 */
	public void addTotalDamage(int damage) {
		totalDamage.addAndGet(damage);
	}

	/**
	 * 累计玩家伤害。
	 * Adds player damage.
	 *
	 * 玩家 / player
	 * damage
	 */
	public void addPlayerDamage(Player player, int damage) {
		addToCounter(player.getObjectId(), damage, playerDamageCounter);
	}

	/**
	 * 累计欧比斯点数。
	 * Adds abyss points.
	 *
	 * 玩家 / player
	 * abyssPoints
	 */
	public void addAbyssPoints(Player player, int abyssPoints) {
		addToCounter(player.getObjectId(), abyssPoints, playerAPCounter);
	}

	/**
	 * 累加到计数器。
	 * Adds to counter.
	 *
	 * key
	 * value
	 * @param AtomicLong 原子长整型 / AtomicLong
	 */
	protected <K> void addToCounter(K key, int value, Map<K, AtomicLong> counterMap) {
		AtomicLong counter;
		synchronized (counterMap) {
			counter = counterMap.get(key);
			if (counter == null) {
				counter = new AtomicLong();
				counterMap.put(key, counter);
			}
		}
		counter.addAndGet(value);
	}

	/**
	 * getTotalDamage 方法。
	 * getTotalDamage method.
	 * result
	 */
	public long getTotalDamage() {
		return totalDamage.get();
	}

	/**
	 * getPlayerDamageCounter 方法。
	 * getPlayerDamageCounter method.
	 * result
	 */
	public Map<Integer, Long> getPlayerDamageCounter() {
		return getOrderedCounterMap(playerDamageCounter);
	}

	/**
	 * getPlayerAbyssPoints 方法。
	 * getPlayerAbyssPoints method.
	 * result
	 */
	public Map<Integer, Long> getPlayerAbyssPoints() {
		return getOrderedCounterMap(playerAPCounter);
	}

	/**
	 * getOrderedCounterMap 方法。
	 * getOrderedCounterMap method.
	 *
	 * @param AtomicLong 原子长整型 / AtomicLong
	 * result
	 */
	protected <K> Map<K, Long> getOrderedCounterMap(Map<K, AtomicLong> unorderedMap) {
		LinkedList<Map.Entry<K, AtomicLong>> tempList;
		synchronized (unorderedMap) {
			if (GenericValidator.isBlankOrNull(unorderedMap)) {
				return Collections.emptyMap();
			}
			tempList = Lists.newLinkedList(unorderedMap.entrySet());
		}
		Collections.sort(tempList, new Comparator<Map.Entry<K, AtomicLong>>() {
			@Override
			/**
			 * 比较排序。
			 * Compares for ordering.
			 *
			 * @param AtomicLong 原子长整型 / AtomicLong
			 * @param AtomicLong 原子长整型 / AtomicLong
			 * result
			 */
			public int compare(Map.Entry<K, AtomicLong> o1, Map.Entry<K, AtomicLong> o2) {
				return Long.compare(o2.getValue().get(), o1.getValue().get());
			}
		});
		Map<K, Long> result = Maps.newLinkedHashMap();
		for (Map.Entry<K, AtomicLong> entry : tempList) {
			if (entry.getValue().get() > 0) {
				result.put(entry.getKey(), entry.getValue().get());
			}
		}
		return result;
	}

	@Override
	/**
	 * compareTo 方法。
	 * compareTo method.
	 *
	 * @param o 对象 / o
	 * result
	 */
	public int compareTo(SiegeRaceCounter o) {
		return Long.compare(o.getTotalDamage(), getTotalDamage());
	}

	/**
	 * getSiegeRace 方法。
	 * getSiegeRace method.
	 * result
	 */
	public SiegeRace getSiegeRace() {
		return siegeRace;
	}

	/**
	 * getWinnerLegionId 方法。
	 * getWinnerLegionId method.
	 * result
	 */
	public Integer getWinnerLegionId() {
		Map<Player, AtomicLong> teamDamageMap = new HashMap<Player, AtomicLong>();
		for (Integer id : getCounterKeys(playerDamageCounter)) {
			Player player = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(id);
			if (player != null && player.getCurrentTeam() != null) {
				Player teamLeader = player.getCurrentTeam().getLeaderObject();
				long damage = playerDamageCounter.get(id).get();
				if (teamLeader != null) {
					if (!teamDamageMap.containsKey(teamLeader)) {
						teamDamageMap.put(teamLeader, new AtomicLong());
					}
					teamDamageMap.get(teamLeader).addAndGet(damage);
				}
			}
		}
		if (teamDamageMap.isEmpty()) {
			return null;
		}
		Player topTeamLeader = getOrderedCounterMap(teamDamageMap).keySet().iterator().next();
		Legion legion = topTeamLeader.getLegion();
		return legion != null ? legion.getLegionId() : null;
	}

	private <K> Set<K> getCounterKeys(Map<K, AtomicLong> counterMap) {
		synchronized (counterMap) {
			return new HashSet<K>(counterMap.keySet());
		}
	}
}
