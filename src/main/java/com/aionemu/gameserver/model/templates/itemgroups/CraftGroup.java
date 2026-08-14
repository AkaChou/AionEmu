package com.aionemu.gameserver.model.templates.itemgroups;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang3.Range;

import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.model.templates.rewards.CraftReward;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 制作奖励组：按等级范围索引的制作奖励。
 * Craft reward group: craft rewards indexed by level range.
 *
 * @author Rolandas
 */
public abstract class CraftGroup extends BonusItemGroup {

	@XmlTransient
	private Map<Integer, Map<Range<Integer>, List<CraftReward>>> dataHolder;

	/** 获取奖励。 / Returns the rewards. */
	public ItemRaceEntry[] getRewards(Integer skillId) {
		if (!dataHolder.containsKey(skillId)) {
			return new ItemRaceEntry[0];
		}
		List<CraftReward> result = new ArrayList<CraftReward>();
		for (List<CraftReward> items : dataHolder.get(skillId).values()) {
			result.addAll(items);
		}
		return result.toArray(new ItemRaceEntry[0]);
	}

	/** 获取奖励。 / Returns the rewards. */
	public ItemRaceEntry[] getRewards(Integer skillId, Integer skillPoints) {
		if (!dataHolder.containsKey(skillId)) {
			return new ItemRaceEntry[0];
		}
		List<CraftReward> result = new ArrayList<CraftReward>();
		for (Entry<Range<Integer>, List<CraftReward>> entry : dataHolder.get(skillId).entrySet())
			if (entry.getKey().contains(skillPoints)) {
				result.addAll(entry.getValue());
			}
		return result.toArray(new ItemRaceEntry[0]);
	}

	/**
	 * @return the dataHolder
	 */
	public Map<Integer, Map<Range<Integer>, List<CraftReward>>> getDataHolder() {
		return dataHolder;
	}

	/**
	 * @param dataHolder the dataHolder to set
	 */
	public void setDataHolder(Map<Integer, Map<Range<Integer>, List<CraftReward>>> dataHolder) {
		this.dataHolder = dataHolder;
	}
}
