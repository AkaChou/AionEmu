package com.aionemu.gameserver.dataholders;

import java.util.ArrayList;
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

import com.aionemu.commons.utils.collections.IntObjectHashMap;
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

	@XmlTransient
	private IntObjectHashMap<Map<Integer, List<NpcShout>>> shoutsByWorldNpcs = new IntObjectHashMap<Map<Integer, List<NpcShout>>>();

	@XmlTransient
	private int count = 0;

	/**
	 * JAXB 反序列化完成后，按世界与 NPC 建立喊话索引并释放原始分组。
	 * After JAXB unmarshalling, indexes shouts by world and npc, then clears raw groups.
	 */
	public void afterUnmarshal(Unmarshaller u, Object parent) {
		for (ShoutGroup group : shoutGroups) {
			for (int i = group.getShoutNpcs().size() - 1; i >= 0; i--) {
				ShoutList shoutList = group.getShoutNpcs().get(i);
				int worldId = shoutList.getRestrictWorld();

				Map<Integer, List<NpcShout>> worldShouts = shoutsByWorldNpcs.get(worldId);
				if (worldShouts == null) {
					worldShouts = new LinkedHashMap<>();
					this.shoutsByWorldNpcs.put(worldId, worldShouts);
				}

				this.count += shoutList.getNpcShouts().size();
				for (int j = shoutList.getNpcIds().size() - 1; j >= 0; j--) {
					int npcId = shoutList.getNpcIds().get(j);
					List<NpcShout> shouts = new ArrayList<NpcShout>(shoutList.getNpcShouts());
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
		Map<Integer, List<NpcShout>> worldShouts = shoutsByWorldNpcs.get(0);

		if (worldShouts == null || worldShouts.get(npcId) == null) {
			worldShouts = shoutsByWorldNpcs.get(worldId);
			if (worldShouts == null || worldShouts.get(npcId) == null)
				return null;
			return new ArrayList<NpcShout>(worldShouts.get(npcId));
		}

		List<NpcShout> npcShouts = new ArrayList<NpcShout>(worldShouts.get(npcId));
		worldShouts = shoutsByWorldNpcs.get(worldId);
		if (worldShouts == null || worldShouts.get(npcId) == null)
			return npcShouts;
		npcShouts.addAll(worldShouts.get(npcId));

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
		Map<Integer, List<NpcShout>> worldShouts = shoutsByWorldNpcs.get(0);

		if (worldShouts == null || worldShouts.get(npcId) == null) {
			worldShouts = shoutsByWorldNpcs.get(worldId);
			if (worldShouts == null || worldShouts.get(npcId) == null) {
				return false;
			}
		}
		return true;
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
		List<NpcShout> shouts = getNpcShouts(worldId, npcId);
		if (shouts == null) {
			return false;
		}
		for (NpcShout s : shouts) {
			if (s.getWhen() == type) {
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
		List<NpcShout> result = new ArrayList<NpcShout>();
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
