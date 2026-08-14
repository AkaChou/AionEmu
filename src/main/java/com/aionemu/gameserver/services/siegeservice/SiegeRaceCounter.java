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
	 * @param creature 造成伤害的生物 / damaging creature
	 * @param damage 伤害量 / damage
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
	 * @param damage 伤害量 / damage
	 */
	public void addTotalDamage(int damage) {
		totalDamage.addAndGet(damage);
	}

	/**
	 * 累计玩家伤害。
	 * Adds player damage.
	 *
	 * @param player 玩家 / player
	 * @param damage 伤害量 / damage
	 */
	public void addPlayerDamage(Player player, int damage) {
		addToCounter(player.getObjectId(), damage, playerDamageCounter);
	}

	/**
	 * 累计欧比斯点数。
	 * Adds abyss points.
	 *
	 * @param player 玩家 / player
	 * @param abyssPoints 欧比斯点数 / abyss points
	 */
	public void addAbyssPoints(Player player, int abyssPoints) {
		addToCounter(player.getObjectId(), abyssPoints, playerAPCounter);
	}

	/**
	 * 累加到计数器。
	 * Adds to counter.
	 *
	 * @param key 计数器键 / counter key
	 * @param value 累加值 / value
	 * @param counterMap 目标计数器表 / target counter map
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
	 * 返回总伤害。
	 * Returns the total damage.
	 *
	 * @return 总伤害 / total damage
	 */
	public long getTotalDamage() {
		return totalDamage.get();
	}

	/**
	 * 返回玩家伤害计数器（按伤害降序）。
	 * Returns the player damage counter (descending by damage).
	 *
	 * @return 玩家伤害映射 / player damage map
	 */
	public Map<Integer, Long> getPlayerDamageCounter() {
		return getOrderedCounterMap(playerDamageCounter);
	}

	/**
	 * 返回玩家欧比斯点数计数器（按点数降序）。
	 * Returns the player abyss-points counter (descending by points).
	 *
	 * @return 玩家欧比斯点数映射 / player abyss-points map
	 */
	public Map<Integer, Long> getPlayerAbyssPoints() {
		return getOrderedCounterMap(playerAPCounter);
	}

	/**
	 * 返回按值降序排序的计数器映射。
	 * Returns the counter map ordered by value descending.
	 *
	 * @param unorderedMap 原始计数器表 / raw counter map
	 * @return 排序后的映射 / ordered map
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
			 * @param o1 前一条目 / first entry
			 * @param o2 后一条目 / second entry
			 * @return 比较结果 / comparison result
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
	 * 按总伤害比较两个种族计数器。
	 * Compares this race counter to another by total damage.
	 *
	 * @param o 对方计数器 / other counter
	 * @return 比较结果 / comparison result
	 */
	public int compareTo(SiegeRaceCounter o) {
		return Long.compare(o.getTotalDamage(), getTotalDamage());
	}

	/**
	 * 返回本计数器所属阵营。
	 * Returns the race of this counter.
	 *
	 * @return 阵营 / siege race
	 */
	public SiegeRace getSiegeRace() {
		return siegeRace;
	}

	/**
	 * 返回伤害最高的队伍所属军团 ID（无则 null）。
	 * Returns the legion id of the top-damage team (or null when none).
	 *
	 * @return 军团 ID / legion id
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
