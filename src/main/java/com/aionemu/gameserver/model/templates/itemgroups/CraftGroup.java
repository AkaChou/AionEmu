/*

 *
 *  Encom is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Encom is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser Public License
 *  along with Encom.  If not, see <http://www.gnu.org/licenses/>.
 */
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
 * @author Rolandas
 */
public abstract class CraftGroup extends BonusItemGroup {

	@XmlTransient
	private Map<Integer, Map<Range<Integer>, List<CraftReward>>> dataHolder;

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
