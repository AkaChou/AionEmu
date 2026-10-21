package com.aionemu.gameserver.model.drop;

import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.Collection;
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
import lombok.Getter;
import lombok.Setter;

/**
 * 掉落队伍模型。
 * Drop Group model.
 */

@Getter
@Setter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "dropGroup", propOrder = { "drop" })
@Slf4j
public class DropGroup implements DropCalculator {


	/** 获取掉落。 / Returns the drop. */
	protected List<Drop> drop;
	/**
	 * 紧凑列式掉落表；静态数据主路径使用它，{@link #drop} 仅保留给 JAXB 兼容路径。
	 * Compact columnar drop table used by the static-data path; {@link #drop} remains only for
	 * JAXB compatibility.
	 */
	@XmlTransient
	private DropTable dropTable;
	/**
	 * 是否声明了 drop 列表；区分“没有列表”和“存在空列表”的旧合并语义。
	 * Whether a drop list was declared; preserves the old merge semantics that distinguish a
	 * missing list from an empty one.
	 */
	@XmlTransient
	private boolean dropListPresent;
	/** 获取种族。 / Returns the race. */
	@XmlAttribute
	protected Race race = Race.PC_ALL;
	@XmlAttribute(name = "name")
	protected String group_name;
	@XmlAttribute(name = "use_category")
	private Boolean legacyUseCategory;
	@XmlAttribute(name = "level_based_chance_reduction")
	private Boolean useLevelBasedChanceReduction;
	/** 返回最大掉落数量 / Returns the max items */
	@XmlAttribute(name = "max_items")
	private int maxItems = 1;
	@XmlAttribute(name = "drop_group_adjustment")
	private int dropGroupAdjustment = 100;
	/** 设置 NPC 专属掉落倍率，1 表示 1 倍。 / Sets the NPC-specific drop multiplier, 1 means 1x. */
	@XmlTransient
	private float chanceMultiplier = 1f;

	/**
	 * 是否启用基于等级的掉率衰减。
	 * Whether to use level based chance reduction.
	 */
	public boolean isUseLevelBasedChanceReduction() {
		return useLevelBasedChanceReduction == null || useLevelBasedChanceReduction;
	}

	/**
	 * 返回掉落组名称。
	 * Returns the drop group name.
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
		copy.dropTable = dropTable;
		copy.dropListPresent = dropListPresent;
		copy.race = race;
		copy.group_name = group_name;
		copy.legacyUseCategory = legacyUseCategory;
		copy.useLevelBasedChanceReduction = useLevelBasedChanceReduction;
		copy.maxItems = maxItems;
		copy.dropGroupAdjustment = dropGroupAdjustment;
		copy.chanceMultiplier = chanceMultiplier;
		return copy;
	}

	/** 返回应用 NPC 和掉落组专属倍率后的基础概率。 / Returns the base chance after applying the NPC and drop-group multipliers. */
	public float getAdjustedChance(Drop candidate) {
		return candidate.getChance() * chanceMultiplier * dropGroupAdjustment / 100f;
	}

	/**
	 * 返回 JAXB 兼容的 Drop 列表；紧凑表会在此处按需物化。
	 * Returns the JAXB-compatible Drop list; the compact table is materialized on demand here.
	 * @return Drop 列表 / Drop list
	 */
	public List<Drop> getDrop() {
		if (drop == null && dropTable != null) {
			drop = dropTable.toDropList();
			dropListPresent = true;
		}
		if (drop == null && dropListPresent) {
			drop = new ArrayList<>();
		}
		return drop;
	}

	/**
	 * 设置 JAXB 兼容列表并清除旧紧凑表。
	 * Sets the JAXB-compatible list and clears the previous compact table.
	 * @param drop Drop 列表 / Drop list
	 */
	public void setDrop(List<Drop> drop) {
		this.drop = drop;
		this.dropTable = null;
		this.dropListPresent = drop != null;
	}

	/**
	 * 返回紧凑掉落表；JAXB 兼容列表在首次访问时转换一次。
	 * Returns the compact drop table; the JAXB compatibility list is converted once on first use.
	 * @return 紧凑掉落表 / compact drop table
	 */
	public DropTable getDropTable() {
		DropTable table = dropTable;
		if (table != null) {
			return table;
		}
		if (drop == null) {
			return DropTable.empty();
		}
		dropListPresent = true;
		// JAXB 兼容列表在生产代码中只读；缓存转换结果，避免管理命令物化后反复重建。
		// The JAXB compatibility list is read-only in production; cache the conversion so admin
		// commands do not force the table to be rebuilt on every later drop calculation.
		table = DropTable.from(drop);
		dropTable = table;
		return table;
	}

	/**
	 * 直接设置紧凑掉落表，避免为每个候选条目创建 Drop。
	 * Sets the compact drop table directly, avoiding a Drop allocation per candidate entry.
	 * @param table 紧凑掉落表 / compact drop table
	 */
	public void setDropTable(DropTable table) {
		replaceDropTable(table, table != null && !table.isEmpty());
	}

	/**
	 * 替换紧凑表并显式指定是否保留“已声明列表”标记，用于过滤后变空的情况。
	 * Replaces the compact table and explicitly controls the declared-list marker, used when
	 * filtering leaves an empty table.
	 * @param table 紧凑表 / compact table
	 * @param listPresent 是否仍视为声明过列表 / whether a list is still considered declared
	 */
	public void replaceDropTable(DropTable table, boolean listPresent) {
		this.dropTable = table == null || table.isEmpty() ? null : table;
		this.dropListPresent = listPresent;
		this.drop = null;
	}

	/**
	 * 旧合并逻辑只在两侧都声明了列表时执行 addAll；此方法保留该判定。
	 * The old merge logic only appended when both sides declared a list; this accessor keeps
	 * that decision explicit.
	 * @return 声明了 drop 列表时为 true / {@code true} when a drop list was declared
	 */
	public boolean hasDropList() {
		if (drop != null) {
			return true;
		}
		if (dropTable == null) {
			return dropListPresent;
		}
		return dropListPresent || !dropTable.isEmpty();
	}

	/**
	 * 掉落计算器：在候选掉落中按有效概率抽取至多 max_items 个掉落项。
	 * Drop calculator: draws up to max_items drops from the candidates by effective chance.
	 * @param result 掉落物结果集 / drop-item result set
	 * @param index 当前索引 / current index
	 * @param dropModifiers 掉落修正器 / drop modifiers
	 * @param groupMembers 队伍成员（用于每人一份的掉落）/ group members (for each-member drops)
	 * @return 下一个可用索引 / the next available index
	 */
	@Override
	public int dropCalculator(Set<DropItem> result, int index, DropModifiers dropModifiers, Collection<Player> groupMembers) {
		DropTable table = getDropTable();
		if (table.isEmpty()) {
			log.debug("Drop group {} is empty", getGroupName());
			return index;
		}
		boolean[] remaining = new boolean[table.size()];
		java.util.Arrays.fill(remaining, true);
		int[] nearestIndexes = new int[table.size()];
		int remainingCount = table.size();
		for (int i = 0; i < maxItems && remainingCount > 0; i++) {
			float chance = Rnd.get() * 100;
			float nearestChanceDiff = Float.MAX_VALUE;
			int nearestCount = 0;
			for (int candidateIndex = 0; candidateIndex < table.size(); candidateIndex++) {
				if (!remaining[candidateIndex]) {
					continue;
				}
				float finalChance = calculateEffectiveChance(candidateIndex, table, dropModifiers);
				if (chance < finalChance) {
					float chanceDiff = finalChance - chance;
					if (nearestCount == 0 || chanceDiff <= nearestChanceDiff) {
						if (chanceDiff < nearestChanceDiff) {
							nearestCount = 0;
							nearestChanceDiff = chanceDiff;
						}
						nearestIndexes[nearestCount++] = candidateIndex;
					}
				}
			}
			if (nearestCount > 0) {
				int selectedIndex = nearestIndexes[Rnd.get(nearestCount)];
				index = addDropItem(result, index, table.dropAt(selectedIndex), groupMembers);
				remaining[selectedIndex] = false;
				remainingCount--;
			}
		}
		return index;
	}

	private float calculateEffectiveChance(int index, DropTable table, DropModifiers dropModifiers) {
		float adjustedChance = table.chance(index) * chanceMultiplier * dropGroupAdjustment / 100f;
		return dropModifiers.calculateDropChance(adjustedChance,
			isUseLevelBasedChanceReduction() && !table.isNoReduction(index));
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
