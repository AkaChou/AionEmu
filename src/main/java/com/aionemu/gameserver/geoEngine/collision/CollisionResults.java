package com.aionemu.gameserver.geoEngine.collision;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

/**
 * 碰撞结果集合，按距离排序收集命中，并携带意图掩码、实例 id 与忽略属性。
 * Ordered collection of collision hits carrying intention mask, instance id and ignore properties.
 */
public class CollisionResults implements Iterable<CollisionResult> {

	/** 命中结果列表。 / Hit result list. */
	private final ArrayList<CollisionResult> results = new ArrayList<CollisionResult>();
	/** 列表是否已按距离排序。 / Whether the list is sorted by distance. */
	private boolean sorted = true;
	/** 是否只取第一个命中。 / Whether only the first hit is required. */
	private final boolean onlyFirst;
	/** 碰撞意图位掩码。 / Collision intention bitmask. */
	private final byte intentions;
	/** 映射副本 ID / Map instance id */
	private int instanceId;
	/** 忽略属性；可为 {@code null} / Ignore properties; may be {@code null} */
	private final IgnoreProperties ignoreProperties;
	/** 是否使斜坡表面无效（不计入有效命中）。 / Whether sloping surfaces should be invalidated. */
	private boolean invalidateSlopingSurface;

	/**
	 * 以意图、仅首命中与实例 id 构造（无忽略属性）。
	 * Constructs with intentions, first-only flag and instance id (no ignore properties).
	 *
	 * intention mask
	 * @param searchFirst 是否仅搜索首个命中 / search first hit only
	 * instance id
	 */
	public CollisionResults(byte intentions, boolean searchFirst, int instanceId) {
		this(intentions, searchFirst, instanceId, null);
	}

	/**
	 * 以意图、仅首命中、实例 id 与忽略属性构造。
	 * Constructs with intentions, first-only flag, instance id and ignore properties.
	 *
	 * intention mask
	 * @param searchFirst 是否仅搜索首个命中 / search first hit only
	 * instance id
	 * ignore properties
	 */
	public CollisionResults(byte intentions, boolean searchFirst, int instanceId, IgnoreProperties ignoreProperties) {
		this.intentions = intentions;
		this.onlyFirst = searchFirst;
		this.instanceId = instanceId;
		this.ignoreProperties = ignoreProperties;
	}

	/**
	 * 以意图、实例 id 与忽略属性构造（收集全部命中）。
	 * Constructs with intentions, instance id and ignore properties (collects all hits).
	 *
	 * intention mask
	 * instance id
	 * ignore properties
	 */
	public CollisionResults(byte intentions, int instanceId, IgnoreProperties ignoreProperties) {
		this(intentions, false, instanceId, ignoreProperties);
	}

	/**
	 * 以意图与实例 id 构造（收集全部命中，无忽略属性）。
	 * Constructs with intentions and instance id (collects all hits, no ignore properties).
	 *
	 * intention mask
	 * instance id
	 */
	public CollisionResults(byte intentions, int instanceId) {
		this(intentions, false, instanceId, null);
	}

	/**
	 * 清空全部命中。
	 * Clears all hits.
	 */
	public void clear() {
		results.clear();
	}

	/**
	 * 返回按距离升序的迭代器（必要时先排序）。
	 * Returns an iterator ordered by ascending distance (sorts lazily when needed).
	 *
	 * @return 结果迭代器 / result iterator
	 */
	@Override
	public Iterator<CollisionResult> iterator() {
		if (!sorted) {
			Collections.sort(results);
			sorted = true;
		}

		return results.iterator();
	}

	/**
	 * 追加一次命中；距离为 NaN 时忽略。非仅首命中模式下标记为未排序。
	 * Appends a hit; ignores NaN distance. Marks unsorted when not first-only mode.
	 *
	 * collision result
	 */
	public void addCollision(CollisionResult result) {
		if (Float.isNaN(result.getDistance())) {
			return;
		}
		results.add(result);
		if (!onlyFirst) {
			sorted = false;
		}
	}

	/**
	 * 返回命中数量。
	 * Returns the number of hits.
	 *
	 * hit count
	 */
	public int size() {
		return results.size();
	}

	/**
	 * 返回最近的命中；无结果时返回 {@code null}。
	 * Returns the closest hit, or {@code null} if empty.
	 *
	 * closest hit
	 */
	public CollisionResult getClosestCollision() {
		if (size() == 0) {
			return null;
		}

		if (!sorted) {
			Collections.sort(results);
			sorted = true;
		}

		return results.get(0);
	}

	/**
	 * 返回最远的命中；无结果时返回 {@code null}。
	 * Returns the farthest hit, or {@code null} if empty.
	 *
	 * farthest hit
	 */
	public CollisionResult getFarthestCollision() {
		if (size() == 0) {
			return null;
		}

		if (!sorted) {
			Collections.sort(results);
			sorted = true;
		}

		return results.get(size() - 1);
	}

	/**
	 * 按排序后下标取命中。
	 * Returns the hit at the given sorted index.
	 *
	 * index
	 * collision result
	 */
	public CollisionResult getCollision(int index) {
		if (!sorted) {
			Collections.sort(results);
			sorted = true;
		}
		return results.get(index);
	}

	/**
	 * 不排序直接按下标取命中（仅内部使用）。
	 * Returns the hit at the given index without sorting (internal use only).
	 *
	 * index
	 * collision result
	 */
	public CollisionResult getCollisionDirect(int index) {
		return results.get(index);
	}

	/**
	 * 调试字符串。
	 * Debug string representation.
	 *
	 * @return 描述字符串 / description string
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("CollisionResults[");
		for (CollisionResult result : results) {
			sb.append(result).append(", ");
		}
		if (results.size() > 0) {
			sb.setLength(sb.length() - 2);
		}

		sb.append("]");
		return sb.toString();
	}

	/**
	 * 是否仅取第一个命中。
	 * Whether only the first hit is required.
	 *
	 * first-only flag
	 */
	public boolean isOnlyFirst() {
		return onlyFirst;
	}

	/**
	 * 返回碰撞意图位掩码。
	 * Returns the collision intention bitmask.
	 *
	 * intention mask
	 */
	public byte getIntentions() {
		return intentions;
	}

	/**
	 * 返回地图实例 id。
	 * Returns the map instance id.
	 *
	 * instance id
	 */
	public int getInstanceId() {
		return instanceId;
	}

	/**
	 * 返回忽略属性。
	 * Returns the ignore properties.
	 *
	 * ignore properties
	 */
	public IgnoreProperties getIgnoreProperties() {
		return ignoreProperties;
	}

	/**
	 * 是否应使斜坡表面无效。
	 * Whether sloping surfaces should be invalidated.
	 *
	 * @return 斜坡无效标志 / sloping-surface invalidation flag
	 */
	public boolean shouldInvalidateSlopingSurface() {
		return invalidateSlopingSurface;
	}

	/**
	 * 设置是否使斜坡表面无效。
	 * Sets whether sloping surfaces should be invalidated.
	 *
	 * @param invalidateSlopingSurface 斜坡无效标志 / sloping-surface invalidation flag
	 */
	public void setInvalidateSlopingSurface(boolean invalidateSlopingSurface) {
		this.invalidateSlopingSurface = invalidateSlopingSurface;
	}

	/**
	 * 设置地图实例 id。
	 * Sets the map instance id.
	 *
	 * instance id
	 */
	public void setInstanceId(int instanceId) {
		this.instanceId = instanceId;
	}
}
