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

/**
 * 掉落队伍模型。
 * Drop Group model.
 */

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

	/** 获取掉落。 / Returns the drop. */
	public List<Drop> getDrop() {
		return this.drop;
	}

	/** 获取种族。 / Returns the race. */
	public Race getRace() {
		return race;
	}

	/** 返回 max items / Returns the max items */
	public int getMaxItems() {
		return maxItems;
	}

	/**
	 * 是否为 Use 等级 BasedChanceReduction。
	 * Whether use level based chance reduction.
	 */
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

	/** 复制 / copy. */
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

	/** 掉落 Calculator / Drop Calculator */
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
