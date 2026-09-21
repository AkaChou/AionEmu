package com.aionemu.gameserver.dataholders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.npcshout.NpcShout;
import com.aionemu.gameserver.model.templates.npcshout.ShoutEventType;
import com.aionemu.gameserver.model.templates.npcshout.ShoutGroup;
import com.aionemu.gameserver.model.templates.npcshout.ShoutList;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * NPC 喊话数据容器，按世界 ID 与 NPC ID 索引 {@link NpcShout}。
 * NPC shout data holder, indexing {@link NpcShout} by world id and npc id.
 *
 * @author Rolandas
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "shoutGroups" })
@XmlRootElement(name = "npc_shouts")
public class NpcShoutData {

	@XmlElement(name = "shout_group")
	protected List<ShoutGroup> shoutGroups;

	/**
	 * 冻结后的世界索引（worldId 升序，供二分查找）。
	 * Frozen world index (world ids ascending, binary searchable).
	 */
	@XmlTransient
	private int[] worldIds = new int[0];

	/** 与 {@link #worldIds} 平行的单世界喊话索引。 / Per-world shout indexes parallel to {@link #worldIds}. */
	@XmlTransient
	private List<WorldShouts> shoutsByWorld = List.of();

	@XmlTransient
	private int count = 0;

	/**
	 * JAXB 反序列化完成后，按世界与 NPC 建立喊话索引并释放原始分组。
	 * After JAXB unmarshalling, indexes shouts by world and npc, then clears raw groups.
	 */
	public void afterUnmarshal(Unmarshaller u, Object parent) {
		Map<Integer, Map<Integer, List<NpcShout>>> byWorld = new LinkedHashMap<>();
		for (ShoutGroup group : shoutGroups) {
			for (int i = group.getShoutNpcs().size() - 1; i >= 0; i--) {
				ShoutList shoutList = group.getShoutNpcs().get(i);
				int worldId = shoutList.getRestrictWorld();

				Map<Integer, List<NpcShout>> worldShouts = byWorld.computeIfAbsent(worldId, ignored -> new LinkedHashMap<>());

				this.count += shoutList.getNpcShouts().size();
				for (int j = shoutList.getNpcIds().size() - 1; j >= 0; j--) {
					int npcId = shoutList.getNpcIds().get(j);
					List<NpcShout> shouts = new ArrayList<>(shoutList.getNpcShouts());
					if (worldShouts.get(npcId) == null) {
						worldShouts.put(npcId, shouts);
					} else {
						worldShouts.get(npcId).addAll(shouts);
					}
					shoutList.getNpcIds().remove(j);
				}
				shoutList.getNpcShouts().clear();
				shoutList.makeNull();
				group.getShoutNpcs().remove(i);
			}
			group.makeNull();
		}
		this.shoutGroups.clear();
		this.shoutGroups = null;
		freeze(byWorld);
	}

	/**
	 * 将加载期容器冻结为升序 int 数组索引：查询时不再为 worldId/npcId 装箱，也不再复制列表。
	 * Freezes the load-time containers into ascending int-array indexes so lookups neither box
	 * worldId/npcId nor copy the shout lists.
	 *
	 * @param byWorld 加载期按世界与 NPC 建立的索引 / load-time index by world and npc
	 */
	private void freeze(Map<Integer, Map<Integer, List<NpcShout>>> byWorld) {
		List<Integer> worlds = new ArrayList<>(byWorld.keySet());
		Collections.sort(worlds);
		int[] ids = new int[worlds.size()];
		List<WorldShouts> frozen = new ArrayList<>(worlds.size());
		for (int i = 0; i < worlds.size(); i++) {
			ids[i] = worlds.get(i);
			frozen.add(WorldShouts.of(byWorld.get(ids[i])));
		}
		this.worldIds = ids;
		this.shoutsByWorld = List.copyOf(frozen);
	}

	/**
	 * 返回指定世界与 NPC 的喊话列表（不复制，调用方禁止修改）。
	 * Returns the shout list of one world and npc (no copy; callers must not modify it).
	 *
	 * @param worldId 世界 ID / world id
	 * @param npcId NPC ID / npc id
	 * @return 列表或 null / list or null
	 */
	private List<NpcShout> shouts(int worldId, int npcId) {
		int index = Arrays.binarySearch(worldIds, worldId);
		return index < 0 ? null : shoutsByWorld.get(index).find(npcId);
	}

	/**
	 * 单个世界的冻结索引：npcId 升序的键数组 + 平行列表。
	 * Frozen index of one world: ascending npc ids plus the parallel shout lists.
	 */
	private record WorldShouts(int[] npcIds, List<List<NpcShout>> shouts) {

		private static WorldShouts of(Map<Integer, List<NpcShout>> byNpcId) {
			List<Integer> ids = new ArrayList<>(byNpcId.keySet());
			Collections.sort(ids);
			int[] npcIds = new int[ids.size()];
			List<List<NpcShout>> lists = new ArrayList<>(ids.size());
			for (int i = 0; i < ids.size(); i++) {
				npcIds[i] = ids.get(i);
				lists.add(byNpcId.get(npcIds[i]));
			}
			return new WorldShouts(npcIds, List.copyOf(lists));
		}

		private List<NpcShout> find(int npcId) {
			int index = Arrays.binarySearch(npcIds, npcId);
			return index < 0 ? null : shouts.get(index);
		}
	}

	/**
	 * 返回已加载的喊话条目数量。
	 * Returns the number of loaded shout entries.
	 *
	 * @return 已加载的呐喊条目数量 / Returns the number of loaded shout entries.
	 */
	public int size() {
		return this.count;
	}

	/**
	 * 获取全局喊话与世界限定喊话的合并副本；用完后请清理。
	 * Returns a combined copy of global and world-specific shouts; clean up after use.
	 *
	 * @param worldId 世界 ID / world id
	 * @param npcId NPC ID / npc id
	 * @return 喊话列表，不存在则为 null / shout list or null
	 */
	public List<NpcShout> getNpcShouts(int worldId, int npcId) {
		List<NpcShout> globalShouts = shouts(0, npcId);
		List<NpcShout> worldShouts = shouts(worldId, npcId);
		if (globalShouts == null) {
			return worldShouts == null ? null : new ArrayList<>(worldShouts);
		}
		List<NpcShout> npcShouts = new ArrayList<>(globalShouts);
		if (worldShouts != null) {
			npcShouts.addAll(worldShouts);
		}
		return npcShouts;
	}

	/**
	 * 轻量检查是否存在喊话，不复制列表。
	 * Lightweight check for any shouts without copying lists.
	 *
	 * @param worldId 世界 ID / world id
	 * @param npcId NPC ID / npc id
	 * @return 存在喊话则为 true / true if any shout exists
	 */
	public boolean hasAnyShout(int worldId, int npcId) {
		return shouts(0, npcId) != null || shouts(worldId, npcId) != null;
	}

	/**
	 * 轻量检查是否存在指定事件类型的喊话。
	 * Lightweight check for shouts of the given event type.
	 *
	 * @param worldId 世界 ID / world id
	 * @param npcId NPC ID / npc id
	 * @param type 喊话事件类型 / shout event type
	 * @return 存在匹配喊话则为 true / true if any matching shout exists
	 */
	public boolean hasAnyShout(int worldId, int npcId, ShoutEventType type) {
		// 这里每次生物事件都会走到：只判定“是否存在该类型”，既不复制列表也不再装箱。
		// This runs on every creature event: test for a matching type without copying the lists or boxing.
		return matchesType(shouts(0, npcId), type) || matchesType(shouts(worldId, npcId), type);
	}

	/**
	 * 是否存在指定事件类型的喊话。
	 * Whether the given list contains a shout of the requested event type.
	 *
	 * @param shouts 喊话列表（可为 null） / shout list (may be null)
	 * @param type 事件类型 / event type
	 * @return 存在则为 true / true if present
	 */
	private static boolean matchesType(List<NpcShout> shouts, ShoutEventType type) {
		if (shouts == null) {
			return false;
		}
		for (NpcShout shout : shouts) {
			if (shout.getWhen() == type) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 按事件类型 / 模式 / 技能序号筛选 NPC 喊话。
	 * Filters NPC shouts by event type, pattern and skill number.
	 *
	 * @param worldId 世界 ID / world id
	 * @param npcId NPC ID / npc id
	 * @param type 喊话事件类型 / shout event type
	 * @param pattern 模式；null 表示不过滤 / pattern, null for any
	 * @param skillNo 技能序号；0 表示不过滤 / skill number, 0 for any
	 * @return 匹配的喊话列表，无匹配则为 null / matching shout list or null
	 */
	public List<NpcShout> getNpcShouts(int worldId, int npcId, ShoutEventType type, String pattern, int skillNo) {
		List<NpcShout> shouts = getNpcShouts(worldId, npcId);
		if (shouts == null) {
			return null;
		}
		List<NpcShout> result = new ArrayList<>();
		for (NpcShout s : shouts) {
			if (s.getWhen() == type) {
				if (pattern != null && !pattern.equals(s.getPattern())) {
					continue;
				}
				if (skillNo != 0 && skillNo != s.getSkillNo()) {
					continue;
				}
				result.add(s);
			}
		}
		shouts.clear();
		return result.size() > 0 ? result : null;
	}
}
