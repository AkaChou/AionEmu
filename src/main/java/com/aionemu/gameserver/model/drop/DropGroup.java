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
package com.aionemu.gameserver.model.drop;

import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "dropGroup", propOrder = { "drop" })
@Slf4j
public class DropGroup implements DropCalculator {


	protected List<Drop> drop;
	@XmlAttribute
	protected Race race = Race.PC_ALL;
	@XmlAttribute(name = "name")
	protected String group_name;
	@XmlAttribute(name = "use_category")
	private Boolean legacyUseCategory;
	@XmlAttribute(name = "level_based_chance_reduction")
	private Boolean useLevelBasedChanceReduction;
	@XmlAttribute(name = "max_items")
	private int maxItems = 1;

	public List<Drop> getDrop() {
		return this.drop;
	}

	public Race getRace() {
		return race;
	}

	public int getMaxItems() {
		return maxItems;
	}

	public boolean isUseLevelBasedChanceReduction() {
		return useLevelBasedChanceReduction != null ? useLevelBasedChanceReduction : true;
	}

	/**
	 * @return the name
	 */
	public String getGroupName() {
		if (group_name == null) {
			return "";
		}
		return group_name;
	}

	public DropGroup copy() {
		DropGroup copy = new DropGroup();
		copy.drop = drop == null ? null : new ArrayList<>(drop);
		copy.race = race;
		copy.group_name = group_name;
		copy.legacyUseCategory = legacyUseCategory;
		copy.useLevelBasedChanceReduction = useLevelBasedChanceReduction;
		copy.maxItems = maxItems;
		return copy;
	}

	@Override
	public int dropCalculator(Set<DropItem> result, int index, DropModifiers dropModifiers, Collection<Player> groupMembers) {
		if (drop == null || drop.isEmpty()) {
			log.debug("Drop group {} is empty", getGroupName());
			return index;
		}
		Set<Drop> remainingDrops = new HashSet<>(drop);
		for (int i = 0; i < maxItems && !remainingDrops.isEmpty(); i++) {
			float chance = Rnd.get() * 100;
			float nearestChanceDiff = Float.MAX_VALUE;
			List<Drop> nearestDropsOfSameChance = new ArrayList<>();
			for (Drop candidate : remainingDrops) {
				float finalChance = calculateEffectiveChance(candidate, dropModifiers);
				if (chance < finalChance) {
					float chanceDiff = finalChance - chance;
					if (nearestDropsOfSameChance.isEmpty() || chanceDiff <= nearestChanceDiff) {
						if (chanceDiff < nearestChanceDiff) {
							nearestDropsOfSameChance.clear();
							nearestChanceDiff = chanceDiff;
						}
						nearestDropsOfSameChance.add(candidate);
					}
				}
			}
			Drop selected = nearestDropsOfSameChance.isEmpty() ? null : Rnd.get(nearestDropsOfSameChance);
			if (selected != null) {
				index = addDropItem(result, index, selected, groupMembers);
				remainingDrops.remove(selected);
			}
		}
		return index;
	}

	float calculateEffectiveChance(Drop candidate, DropModifiers dropModifiers) {
		return dropModifiers.calculateDropChance(candidate.getChance(),
				isUseLevelBasedChanceReduction() && !candidate.isNoReduction());
	}

	private int addDropItem(Set<DropItem> result, int index, Drop selected, Collection<Player> groupMembers) {
		if (selected.isEachMember() && groupMembers != null && !groupMembers.isEmpty()) {
			for (Player player : groupMembers) {
				DropItem item = new DropItem(selected);
				item.calculateCount();
				item.setIndex(index++);
				item.setPlayerObjId(player.getObjectId());
				item.setWinningPlayer(player);
				item.isDistributeItem(true);
				result.add(item);
			}
		} else {
			DropItem item = new DropItem(selected);
			item.calculateCount();
			item.setIndex(index++);
			result.add(item);
		}
		return index;
	}
}
