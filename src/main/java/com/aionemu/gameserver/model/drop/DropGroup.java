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
import jakarta.xml.bind.annotation.XmlTransient;
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
	@XmlAttribute(name = "drop_group_adjustment")
	private int dropGroupAdjustment = 100;
	@XmlTransient
	private float chanceMultiplier = 1f;

	/** 获取掉落。 / Returns the drop. */
	public List<Drop> getDrop() {
		return this.drop;
	}

	/** 获取种族。 / Returns the race. */
	public Race getRace() {
		return race;
	}

	/** 返回最大掉落数量 / Returns the max items */
	public int getMaxItems() {
		return maxItems;
	}

	/**
	 * 是否启用基于等级的掉率衰减。
	 * Whether to use level based chance reduction.
	 */
	public boolean isUseLevelBasedChanceReduction() {
		return useLevelBasedChanceReduction != null ? useLevelBasedChanceReduction : true;
	}

	/**
	 * 返回掉落组名称。
	 * Returns the drop group name.
	 *
	 * @return 名称，未设置时为空串 / the name, empty string if unset
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
		copy.dropGroupAdjustment = dropGroupAdjustment;
		copy.chanceMultiplier = chanceMultiplier;
		return copy;
	}

	/** 设置 NPC 专属掉落倍率，1 表示 1 倍。 / Sets the NPC-specific drop multiplier, 1 means 1x. */
	public void setChanceMultiplier(float chanceMultiplier) {
		this.chanceMultiplier = chanceMultiplier;
	}

	/** 返回 NPC 专属掉落倍率。 / Returns the NPC-specific drop multiplier. */
	public float getChanceMultiplier() {
		return chanceMultiplier;
	}

	/** 返回应用 NPC 和掉落组专属倍率后的基础概率。 / Returns the base chance after applying the NPC and drop-group multipliers. */
	public float getAdjustedChance(Drop candidate) {
		return candidate.getChance() * chanceMultiplier * dropGroupAdjustment / 100f;
	}

	/**
	 * 掉落计算器：在候选掉落中按有效概率抽取至多 max_items 个掉落项。
	 * Drop calculator: draws up to max_items drops from the candidates by effective chance.
	 *
	 * @param result 掉落物结果集 / drop-item result set
	 * @param index 当前索引 / current index
	 * @param dropModifiers 掉落修正器 / drop modifiers
	 * @param groupMembers 队伍成员（用于每人一份的掉落）/ group members (for each-member drops)
	 * @return 下一个可用索引 / the next available index
	 */
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
		return dropModifiers.calculateDropChance(getAdjustedChance(candidate),
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
