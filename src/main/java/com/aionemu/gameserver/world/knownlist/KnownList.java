package com.aionemu.gameserver.world.knownlist;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import com.aionemu.gameserver.configs.main.SecurityConfig;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.world.MapRegion;

import java.util.LinkedHashMap;

/**
 * 可见对象已知列表：维护“已知”与“可见”两套对象/玩家集合，并负责发现、遗忘与遍历。
 * Visible-object known list: maintains known/visual object and player maps, and handles discovery, forget, and iteration.
 *
 * @author -Nemesiss-
 * @modified kosyachok
 */
@Slf4j
public class KnownList {

	/**
	 * 本已知列表的所有者。
	 * Owner of this known list.
	 */
	protected final VisibleObject owner;

	/**
	 * 所有者已知的对象映射（objectId → 对象）。
	 * Objects known by the owner (objectId → object).
	 */
	protected final Map<Integer, VisibleObject> knownObjects = Collections.synchronizedMap(new LinkedHashMap<Integer, VisibleObject>());

	/**
	 * 所有者已知的玩家映射（懒初始化）。
	 * Players known by the owner (lazily initialized).
	 */
	protected volatile Map<Integer, Player> knownPlayers;

	/**
	 * 所有者当前可见的对象映射（objectId → 对象）。
	 * Objects currently visual to the owner (objectId → object).
	 */
	protected final Map<Integer, VisibleObject> visualObjects = Collections.synchronizedMap(new LinkedHashMap<Integer, VisibleObject>());

	/**
	 * 所有者当前可见的玩家映射（懒初始化）。
	 * Players currently visual to the owner (lazily initialized).
	 */
	protected volatile Map<Integer, Player> visualPlayers;

	/**
	 * 更新锁，串行化 {@link #doUpdate()}。
	 * Update lock serializing {@link #doUpdate()}.
	 */
	private ReentrantLock lock = new ReentrantLock();

	/**
	 * 创建指定所有者的已知列表。
	 * Creates a known list for the given owner.
	 *
	 * @param owner 列表所有者 / list owner
	 */
	public KnownList(VisibleObject owner) {
		this.owner = owner;
	}

	/**
	 * 执行已知列表更新：先遗忘超距对象，再发现可见对象。
	 * Performs a known-list update: forgets out-of-range objects, then discovers visible ones.
	 */
	public void doUpdate() {
		lock.lock();
		try {
			forgetObjects();
			findVisibleObjects();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 清空已知列表（对象消失时使用），并通知对方删除本所有者。
	 * Clears the known list (used on despawn) and tells counterparts to drop this owner.
	 */
	public void clear() {
		clear(false);
	}

	public void clear(boolean isOutOfRange) {
		for (VisibleObject object : knownObjectsSnapshot()) {
			object.getKnownList().del(owner, isOutOfRange);
		}
		knownObjects.clear();
		if (knownPlayers != null) {
			knownPlayers.clear();
		}
		visualObjects.clear();
		if (visualPlayers != null) {
			visualPlayers.clear();
		}
	}

	/**
	 * 判断对象是否已被本列表知晓。
	 * Checks whether the object is already known.
	 *
	 * @param object 待检查对象 / object to check
	 * @return 已知则返回 {@code true} / {@code true} if known
	 */
	public boolean knowns(AionObject object) {
		return knownObjects.containsKey(object.getObjectId());
	}

	/**
	 * 将可见对象加入本已知列表。
	 * Adds a visible object to this known list.
	 *
	 * @param object 待添加对象 / object to add
	 * @return 首次加入成功返回 {@code true} / {@code true} if newly added
	 */
	protected boolean add(VisibleObject object) {
		if (!isAwareOf(object))
			return false;

		if (knownObjects.put(object.getObjectId(), object) == null) {
			if (object instanceof Player) {
				checkKnownPlayersInitialized();
				knownPlayers.put(object.getObjectId(), (Player) object);
			}
			addVisualObject(object);
			return true;
		}
		return false;
	}

	/**
	 * 将对象加入可见集合，并通知控制器 {@code see}。
	 * Adds the object to the visual set and notifies the controller via {@code see}.
	 *
	 * visual object
	 */
	public void addVisualObject(VisibleObject object) {
		if (object instanceof Creature) {
			if (SecurityConfig.INVIS && object instanceof Player) {
				if (!owner.canSee((Player) object)) {
					return;
				}
			}

			if (visualObjects.put(object.getObjectId(), object) == null) {
				if (object instanceof Player) {
					checkVisiblePlayersInitialized();
					visualPlayers.put(object.getObjectId(), (Player) object);
				}
				owner.getController().see(object);
			}
		} else if (visualObjects.put(object.getObjectId(), object) == null) {
			owner.getController().see(object);
		}
	}

	/**
	 * 从本已知列表删除可见对象。
	 * Removes a visible object from this known list.
	 *
	 * @param object 待删除对象 / object to remove
	 * @param isOutOfRange 是否因超出范围 / whether removal is due to range
	 */
	private void del(VisibleObject object, boolean isOutOfRange) {
		/**
	 * 对象已知 / object was known
	 */
		if (knownObjects.remove(object.getObjectId()) != null) {
			if (knownPlayers != null) {
				knownPlayers.remove(object.getObjectId());
			}
			delVisualObject(object, isOutOfRange);
		}
	}

	/**
	 * 从可见集合删除对象，并通知控制器 {@code notSee}。
	 * Removes the object from the visual set and notifies the controller via {@code notSee}.
	 *
	 * visual object
	 * @param isOutOfRange 是否因超出范围 / whether removal is due to range
	 */
	public void delVisualObject(VisibleObject object, boolean isOutOfRange) {
		if (visualObjects.remove(object.getObjectId()) != null) {
			if (visualPlayers != null) {
				visualPlayers.remove(object.getObjectId());
			}
			owner.getController().notSee(object, isOutOfRange);
		}
	}

	/**
	 * 遗忘超出距离的对象，并双向删除。
	 * Forgets objects that are out of range, removing both sides.
	 */
	private void forgetObjects() {
		for (VisibleObject object : knownObjectsSnapshot()) {
			if (!checkObjectInRange(object) && !object.getKnownList().checkReversedObjectInRange(owner)) {
				del(object, true);
				object.getKnownList().del(owner, true);
			}
		}
	}

	/**
	 * 在邻近区域中发现可见范围内的对象，并建立双向已知关系。
	 * Discovers objects within visibility range in neighbour regions and establishes mutual knowledge.
	 */
	protected void findVisibleObjects() {
		if (owner == null || !owner.isSpawned())
			return;

		MapRegion[] regions = owner.getActiveRegion().getNeighbours();
		for (int i = 0; i < regions.length; i++) {
			MapRegion r = regions[i];
			for (VisibleObject newObject : r.getObjectsSnapshot()) {
				if (newObject == owner || newObject == null) {
					continue;
				}
				if (!isAwareOf(newObject)) {
					continue;
				}
				if (knownObjects.containsKey(newObject.getObjectId())) {
					continue;
				}
				if (!checkObjectInRange(newObject) && !newObject.getKnownList().checkReversedObjectInRange(owner)) {
					continue;
				}
				/**
	 * New object is not known
	 */
				if (add(newObject)) {
					newObject.getKnownList().add(owner);
				}
			}
		}
	}

	/**
	 * 判断所有者是否应感知该对象（是否保留在已知列表中）。
	 * Whether the known-list owner is aware of the found object (should keep it in the list).
	 *
	 * candidate object
	 *
	 * @param newObject
	 * @return 应感知则返回 {@code true} / {@code true} if aware
	 */
	protected boolean isAwareOf(VisibleObject newObject) {
		return true;
	}

	/**
	 * 检查对象是否在所有者的可见距离内（含 Z 轴上限）。
	 * Checks whether the object is within the owner's visibility distance (including max Z).
	 *
	 * candidate object
	 *
	 * @param newObject
	 * @return 在范围内返回 {@code true} / {@code true} if in range
	 */
	protected boolean checkObjectInRange(VisibleObject newObject) {
		// 检查 Z 距离是否大于 maxZvisibleDistance / check if Z distance is greater than maxZvisibleDistance
		if (Math.abs(owner.getZ() - newObject.getZ()) > owner.getMaxZVisibleDistance()) {
			return false;
		}
		return MathUtil.isInRange(owner, newObject, owner.getVisibilityDistance());
	}

	/**
	 * 反向范围检查；若新对象使用不同的感知半径，可覆盖此方法。
	 * Reverse range check; override when the new object uses a different awareness radius.
	 *
	 * candidate object
	 *
	 * @param newObject {@code false} by default。
	 */
	protected boolean checkReversedObjectInRange(VisibleObject newObject) {
		return false;
	}

	/**
	 * 对所有已知 NPC 执行访问回调（无数量上限）。
	 * Visits all known NPCs without an iteration limit.
	 *
	 * visitor callback
	 */
	public void doOnAllNpcs(Visitor<Npc> visitor) {
		doOnAllNpcs(visitor, Integer.MAX_VALUE);
	}

	/**
	 * 对所有已知 NPC 执行访问回调，可限制最大遍历数。
	 * Visits known NPCs, optionally capped by an iteration limit.
	 *
	 * visitor callback
	 *
	 * @param iterationLimit 最大遍历数 / maximum iterations
	 * @param iterationLimit
	 * @return 实际遍历数量 / number of NPCs visited
	 */
	public int doOnAllNpcs(Visitor<Npc> visitor, int iterationLimit) {
		int counter = 0;
		try {
			for (VisibleObject newObject : knownObjectsSnapshot()) {
				if (newObject != null && newObject instanceof Npc) {
					if ((++counter) == iterationLimit) {
						break;
					}
					visitor.visit((Npc) newObject);
				}
			}
		} catch (Exception ex) {
			log.error(I18n.get("log.70e363d7c042", ex), ex);
		}
		return counter;
	}

	/**
	 * 对所有已知 NPC 执行携带所有者的访问回调（无数量上限）。
	 * Visits all known NPCs with owner context, without an iteration limit.
	 *
	 * visitor callback
	 */
	public void doOnAllNpcsWithOwner(VisitorWithOwner<Npc, VisibleObject> visitor) {
		doOnAllNpcsWithOwner(visitor, Integer.MAX_VALUE);
	}

	/**
	 * 对所有已知 NPC 执行携带所有者的访问回调，可限制最大遍历数。
	 * Visits known NPCs with owner context, optionally capped by an iteration limit.
	 *
	 * visitor callback
	 *
	 * @param iterationLimit 最大遍历数 / maximum iterations
	 * @param iterationLimit
	 * @return 实际遍历数量 / number of NPCs visited
	 */
	public int doOnAllNpcsWithOwner(VisitorWithOwner<Npc, VisibleObject> visitor, int iterationLimit) {
		int counter = 0;
		try {
			for (VisibleObject newObject : knownObjectsSnapshot()) {
				if (newObject != null && newObject instanceof Npc) {
					if ((++counter) == iterationLimit) {
						break;
					}
					visitor.visit((Npc) newObject, owner);
				}
			}
		} catch (Exception ex) {
			log.error(I18n.get("log.70e363d7c042", ex), ex);
		}
		return counter;
	}

	/**
	 * 对所有已知玩家执行访问回调。
	 * Visits all known players.
	 *
	 * visitor callback
	 */
	public void doOnAllPlayers(Visitor<Player> visitor) {
		if (knownPlayers == null) {
			return;
		}
		try {
			for (Player player : knownPlayersSnapshot()) {
				if (player != null) {
					visitor.visit(player);
				}
			}
		} catch (Exception ex) {
			log.error(I18n.get("log.77c1d3d24013", ex), ex);
		}
	}

	/**
	 * 对所有已知对象执行访问回调。
	 * Visits all known objects.
	 *
	 * visitor callback
	 */
	public void doOnAllObjects(Visitor<VisibleObject> visitor) {
		try {
			for (VisibleObject newObject : knownObjectsSnapshot()) {
				if (newObject != null) {
					visitor.visit(newObject);
				}
			}
		} catch (Exception ex) {
			log.error(I18n.get("log.e15440de12ca", ex), ex);
		}
	}

	/**
	 * 返回已知对象映射（实时视图）。
	 * Returns the known-objects map (live view).
	 *
	 * @return 已知对象映射 / known objects map
	 */
	public Map<Integer, VisibleObject> getKnownObjects() {
		return knownObjects;
	}

	/**
	 * 返回已知对象快照列表。
	 * Returns a snapshot list of known objects.
	 *
	 * @return 已知对象快照 / known objects snapshot
	 */
	public List<VisibleObject> getKnownObjectsSnapshot() {
		return knownObjectsSnapshot();
	}

	/**
	 * 返回可见对象快照列表。
	 * Returns a snapshot list of visual objects.
	 *
	 * @return 可见对象快照 / visual objects snapshot
	 */
	public List<VisibleObject> getVisibleObjectsSnapshot() {
		synchronized (visualObjects) {
			return new ArrayList<>(visualObjects.values());
		}
	}

	/**
	 * 已知对象值的线程安全快照。
	 * Thread-safe snapshot of known-object values.
	 *
	 * snapshot list
	 */
	private List<VisibleObject> knownObjectsSnapshot() {
		synchronized (knownObjects) {
			return new ArrayList<>(knownObjects.values());
		}
	}

	/**
	 * 已知玩家值的线程安全快照。
	 * Thread-safe snapshot of known-player values.
	 *
	 * snapshot list
	 */
	private List<Player> knownPlayersSnapshot() {
		if (knownPlayers == null) {
			return Collections.emptyList();
		}
		synchronized (knownPlayers) {
			return new ArrayList<>(knownPlayers.values());
		}
	}

	/**
	 * 返回可见对象映射（实时视图）。
	 * Returns the visual-objects map (live view).
	 *
	 * @return 可见对象映射 / visual objects map
	 */
	public Map<Integer, VisibleObject> getVisibleObjects() {
		return visualObjects;
	}

	/**
	 * 返回已知玩家映射的副本；未初始化时返回空映射。
	 * Returns a copy of the known-players map; empty if not initialized.
	 *
	 * @return 已知玩家映射副本 / known players map copy
	 */
	public Map<Integer, Player> getKnownPlayers() {
		if (knownPlayers == null) {
			return Collections.emptyMap();
		}
		synchronized (knownPlayers) {
			return new LinkedHashMap<Integer, Player>(knownPlayers);
		}
	}

	/**
	 * 返回所有者到最近已知玩家的水平距离平方；没有玩家时返回正无穷。
	 * Returns the squared horizontal distance to the nearest known player, or positive infinity when none are known.
	 */
	public float getNearestKnownPlayerDistanceSquared() {
		Map<Integer, Player> players = knownPlayers;
		if (players == null) {
			return Float.POSITIVE_INFINITY;
		}
		float ownerX = owner.getX();
		float ownerY = owner.getY();
		float nearest = Float.POSITIVE_INFINITY;
		synchronized (players) {
			for (Player player : players.values()) {
				if (player == null) {
					continue;
				}
				float dx = player.getX() - ownerX;
				float dy = player.getY() - ownerY;
				nearest = Math.min(nearest, dx * dx + dy * dy);
			}
		}
		return nearest;
	}

	/**
	 * 返回可见玩家映射的副本；未初始化时返回空映射。
	 * Returns a copy of the visual-players map; empty if not initialized.
	 *
	 * @return 可见玩家映射副本 / visual players map copy
	 */
	public Map<Integer, Player> getVisiblePlayers() {
		if (visualPlayers == null) {
			return Collections.emptyMap();
		}
		synchronized (visualPlayers) {
			return new LinkedHashMap<Integer, Player>(visualPlayers);
		}
	}

	/**
	 * 懒初始化已知玩家映射。
	 * Lazily initializes the known-players map.
	 */
	final void checkKnownPlayersInitialized() {
		if (knownPlayers == null) {
			synchronized (this) {
				if (knownPlayers == null) {
					knownPlayers = Collections.synchronizedMap(new LinkedHashMap<Integer, Player>());
				}
			}
		}
	}

	/**
	 * 懒初始化可见玩家映射。
	 * Lazily initializes the visual-players map.
	 */
	final void checkVisiblePlayersInitialized() {
		if (visualPlayers == null) {
			synchronized (this) {
				if (visualPlayers == null) {
					visualPlayers = Collections.synchronizedMap(new LinkedHashMap<Integer, Player>());
				}
			}
		}
	}

	/**
	 * 按对象 ID 获取已知对象。
	 * Returns a known object by object id.
	 *
	 * target object id
	 *
	 * @param targetObjectId
	 * @return 已知对象，不存在则为 {@code null} / known object, or {@code null}
	 */
	public VisibleObject getObject(int targetObjectId) {
		return this.knownObjects.get(targetObjectId);
	}
}
