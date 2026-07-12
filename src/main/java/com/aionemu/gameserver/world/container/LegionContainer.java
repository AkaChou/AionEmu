package com.aionemu.gameserver.world.container;

import java.util.Iterator;

import com.aionemu.gameserver.model.team.legion.Legion;
import com.aionemu.gameserver.world.exceptions.DuplicateAionObjectException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 军团容器：按 ID 与名称双向索引缓存在线/加载中的军团。
 * Legion container: caches legions indexed by both ID and name.
 */
public class LegionContainer implements Iterable<Legion> {

	/**
	 * 按军团 ID 索引 / Indexed by legion ID
	 */
	private final Map<Integer, Legion> legionsById = new LinkedHashMap<Integer, Legion>();

	/**
	 * 按军团名称（小写）索引 / Indexed by legion name (lower-case)
	 */
	private final Map<String, Legion> legionsByName = new LinkedHashMap<String, Legion>();

	/**
	 * 添加军团；ID 或名称冲突时抛出 {@link DuplicateAionObjectException}。
	 * Adds a legion; throws {@link DuplicateAionObjectException} on ID or name conflict.
	 *
	 * @param legion 待添加军团 / legion to add
	 */
	public synchronized void add(Legion legion) {
		if (legion == null || legion.getLegionName() == null) {
			return;
		}
		String legionName = legion.getLegionName().toLowerCase();
		if (legionsById.containsKey(legion.getLegionId()) || legionsByName.containsKey(legionName)) {
			throw new DuplicateAionObjectException();
		}
		legionsById.put(legion.getLegionId(), legion);
		legionsByName.put(legionName, legion);
	}

	/**
	 * 从容器中移除军团。
	 * Removes the legion from this container.
	 *
	 * @param legion 待移除军团 / legion to remove
	 */
	public synchronized void remove(Legion legion) {
		legionsById.remove(legion.getLegionId());
		legionsByName.remove(legion.getLegionName().toLowerCase());
	}

	/**
	 * 按军团 ID 查找。
	 * Looks up a legion by ID.
	 *
	 * legion ID
	 *
	 * @param legionId
	 * @return 军团实例，不存在则返回 null / legion instance, or null if absent
	 */
	public synchronized Legion get(int legionId) {
		return legionsById.get(legionId);
	}

	/**
	 * 按军团名称（忽略大小写）查找。
	 * Looks up a legion by name (case-insensitive).
	 *
	 * @param name 军团名称 / legion name
	 * @return 军团实例，不存在则返回 null / legion instance, or null if absent
	 */
	public synchronized Legion get(String name) {
		return legionsByName.get(name.toLowerCase());
	}

	/**
	 * 返回所有军团的快照列表。
	 * Returns a snapshot list of all legions.
	 *
	 * @return 军团列表副本 / copy of the legion list
	 */
	public synchronized List<Legion> getAllLegions() {
		return new ArrayList<Legion>(legionsByName.values());
	}

	/**
	 * 是否包含指定 ID 的军团。
	 * Whether a legion with the given ID is present.
	 *
	 * legion ID
	 *
	 * @param legionId 存在则为 true / true if present
	 */
	public synchronized boolean contains(int legionId) {
		return legionsById.containsKey(legionId);
	}

	/**
	 * 是否包含指定名称（忽略大小写）的军团。
	 * Whether a legion with the given name is present (case-insensitive).
	 *
	 * @param name 军团名称 / legion name
	 * @return 存在则为 true / true if present
	 */
	public synchronized boolean contains(String name) {
		return legionsByName.containsKey(name.toLowerCase());
	}

	/**
	 * 返回按 ID 索引的军团迭代器（快照）。
	 * Returns an iterator over legions by ID (snapshot).
	 *
	 * @return 军团迭代器 / legion iterator
	 */
	@Override
	public synchronized Iterator<Legion> iterator() {
		return new ArrayList<Legion>(legionsById.values()).iterator();
	}

	/**
	 * 清空容器中的全部军团。
	 * Clears all legions from this container.
	 */
	public synchronized void clear() {
		legionsById.clear();
		legionsByName.clear();
	}
}
