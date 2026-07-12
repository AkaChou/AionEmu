package com.aionemu.gameserver.controllers.movement;

import com.aionemu.gameserver.model.gameobjects.Creature;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 已看见的生物列表，按 objectId 去重维护可见集合。
 * List of seen creatures, maintained as a de-duplicated set keyed by objectId.
 */
public class SeenCreatureList {

	/** 已看见生物映射 / Map of seen creatures */
	private Map<Integer, Creature> seenCreatures;

	/**
	 * 添加生物到已看见列表。
	 * Add a creature to the seen list.
	 *
	 * Creature
	 *
	 * @param creature
	 * @return 是否新加入 / Whether newly added
	 */
	public boolean add(Creature creature) {
		if (seenCreatures == null) {
			seenCreatures = new LinkedHashMap<>();
		}
		return seenCreatures.putIfAbsent(creature.getObjectId(), creature) == null;
	}

	/**
	 * 从已看见列表移除生物。
	 * Remove a creature from the seen list.
	 *
	 * Creature
	 *
	 * @param creature
	 * @return 是否移除成功 / Whether removed
	 */
	public boolean remove(Creature creature) {
		if (seenCreatures == null) {
			return false;
		}
		return seenCreatures.remove(creature.getObjectId()) != null;
	}

	/**
	 * 清空已看见列表。
	 * Clear the seen list.
	 */
	public void clear() {
		if (seenCreatures != null) {
			seenCreatures.clear();
		}
	}

	/**
	 * 是否包含指定生物。
	 * Whether the list contains the given creature.
	 *
	 * Creature
	 * Whether contained
	 */
	public boolean contains(Creature creature) {
		if (seenCreatures == null) {
			return false;
		}
		return seenCreatures.containsKey(creature.getObjectId());
	}
}
