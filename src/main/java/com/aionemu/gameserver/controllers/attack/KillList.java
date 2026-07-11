package com.aionemu.gameserver.controllers.attack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.configs.main.PvPConfig;

/**
 * PvP 击杀记录表，按受害者 ID 记录击杀时间并支持链式击杀限制。
 * PvP kill list that tracks kill timestamps per victim and enforces chain-kill limits.
 *
 * @author Sarynth
 */
public class KillList {

	/** 受害者 ID → 击杀时间戳列表 / victim id → list of kill timestamps */
	private Map<Integer, List<Long>> killList;

	/**
	 * 创建空的击杀记录表。
	 * Creates an empty kill list.
	 */
	public KillList() {
		killList = new LinkedHashMap<Integer, List<Long>>();
	}

	/**
	 * 返回在链式击杀时间窗口内对该受害者的有效击杀次数。
	 * Returns the number of valid kills against the victim within the chain-kill window.
	 *
	 * @param victimId 受害者对象 ID / victim object id
	 * @return 有效击杀次数 / valid kill count for the victim
	 */
	public int getKillsFor(int victimId) {
		List<Long> killTimes = killList.get(victimId);

		if (killTimes == null) {
			return 0;
		}

		long now = System.currentTimeMillis();
		int killCount = 0;

		for (Iterator<Long> i = killTimes.iterator(); i.hasNext();) {
			if (now - i.next().longValue() > PvPConfig.CHAIN_KILL_TIME_RESTRICTION) {
				i.remove();
			} else {
				killCount++;
			}
		}
		return killCount;
	}

	/**
	 * 记录一次对指定受害者的击杀。
	 * Records a kill against the given victim.
	 *
	 * @param victimId 受害者对象 ID / victim object id
	 */
	public void addKillFor(int victimId) {
		List<Long> killTimes = killList.get(victimId);
		if (killTimes == null) {
			killTimes = new ArrayList<Long>();
			killList.put(victimId, killTimes);
		}

		killTimes.add(System.currentTimeMillis());
	}
}
