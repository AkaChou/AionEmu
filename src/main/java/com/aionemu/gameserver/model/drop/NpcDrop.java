package com.aionemu.gameserver.model.drop;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * NPC 掉落，用于掉落相关逻辑。
 * Npc Drop for drop logic.
 */

@XmlRootElement(name = "npc_drop")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "npcDrop", propOrder = { "dropGroup", "commonDropGroup" })
public class NpcDrop implements DropCalculator {

	@XmlElement(name = "drop_group")
	protected List<DropGroup> dropGroup;
	@XmlElement(name = "common_drop_group")
	protected List<CommonDropGroup> commonDropGroup;
	@XmlAttribute(name = "npc_id", required = true)
	protected int npcId;

	/** 获取掉落队伍。 / Returns the drop group. */
	public List<DropGroup> getDropGroup() {
		if (dropGroup == null) {
			return Collections.emptyList();
		}
		return this.dropGroup;
	}

	/** 返回公共掉落组名称列表 / Returns the common drop group names */
	public List<String> getCommonDropGroupNames() {
		if (commonDropGroup == null) {
			return Collections.emptyList();
		}
		return commonDropGroup.stream()
			.map(CommonDropGroup::getName)
			.toList();
	}

	/** 返回公共掉落组引用。 / Returns the common drop groups. */
	public List<CommonDropGroup> getCommonDropGroups() {
		return commonDropGroup == null ? Collections.emptyList() : commonDropGroup;
	}

	/** 添加掉落组 / Adds drop groups */
	public void addDropGroups(List<DropGroup> groups) {
		if (groups.isEmpty()) {
			return;
		}
		if (dropGroup == null) {
			dropGroup = new ArrayList<>();
		}
		dropGroup.addAll(groups);
	}

	 /**
	  * 获取 npcId 属性值。
	  * Gets the value of the npcId property
	  */
	public int getNpcId() {
		return npcId;
	}

	/**
	 * 掉落计算器：按种族筛选掉落组后依次执行各组计算。
	 * Drop calculator: filters drop groups by race, then runs each matching group.
	 *
	 * @param result 掉落物结果集 / drop-item result set
	 * @param index 当前索引 / current index
	 * @param dropModifiers 掉落修正器 / drop modifiers
	 * @param groupMembers 队伍成员（用于每人一份的掉落）/ group members (for each-member drops)
	 * @return 下一个可用索引 / the next available index
	 */
	@Override
	public int dropCalculator(Set<DropItem> result, int index, DropModifiers dropModifiers,
			Collection<Player> groupMembers) {
		if (dropGroup == null || dropGroup.isEmpty()) {
			return index;
		}
		for (DropGroup dg : dropGroup) {
			if (dg.getRace() == Race.PC_ALL || dg.getRace() == dropModifiers.getDropRace()) {
				index = dg.dropCalculator(result, index, dropModifiers, groupMembers);
			}
		}
		return index;
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class CommonDropGroup {
		@XmlAttribute(name = "name", required = true)
		protected String name;
		@XmlAttribute(name = "common_drop_adjustment")
		protected int commonDropAdjustment = 100;

		/** 获取名称。 / Returns the name. */
		public String getName() {
			return name;
		}

		/** 返回公共掉落倍率，100 表示 1 倍。 / Returns the common drop adjustment, 100 means 1x. */
		public int getCommonDropAdjustment() {
			return commonDropAdjustment;
		}
	}
}
