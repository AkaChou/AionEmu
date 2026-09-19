package com.aionemu.gameserver.world.knownlist;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
import lombok.RequiredArgsConstructor;

/**
 * 可见对象已知列表：维护“已知”与“可见”两套对象/玩家集合，并负责发现、遗忘与遍历。
 * Visible-object known list: maintains known/visual object and player maps, and handles discovery, forget, and iteration.
 *
 * @author -Nemesiss-
 * @modified kosyachok
 */
@Slf4j
@RequiredArgsConstructor
public class KnownList {

	/**
	 * 本已知列表的所有者。
	 * Owner of this known list.
	 */
	protected final VisibleObject owner;

	/**
	 * 所有者已知的对象映射（objectId → 对象，懒初始化）。
	 * Objects known by the owner (objectId → object), lazily initialized.
	 *
	 * <p>与玩家映射同一范式：{@code volatile} 字段 + {@code synchronized (this)} 双检创建，
	 * 读取路径先取局部引用、为 null 时直接返回而不加任何锁——这样既不会有"全服共用一个空表监视器"
	 * 的争用，也不会出现两个线程各自持有一张映射而丢对象。创建后引用不再变化，因此
	 * 对快照块的 {@code synchronized (map)} 仍然钉在同一个对象上。
	 * Same idiom as the player maps: a volatile field created under a double-checked
	 * {@code synchronized (this)}, while readers take a local reference and return early on null without
	 * locking. That avoids both a shared empty-map monitor across every creature and two threads ending up
	 * with different maps. The reference never changes once created, so the snapshot blocks keep
	 * synchronizing on one stable object.</p>
	 */
	protected volatile Map<Integer, VisibleObject> knownObjects;

	/**
	 * 所有者已知的玩家映射（懒初始化）。
	 * Players known by the owner (lazily initialized).
	 */
	protected volatile Map<Integer, Player> knownPlayers;

	/**
	 * 所有者当前可见的对象映射（objectId → 对象，懒初始化，范式同 {@link #knownObjects}）。
	 * Objects currently visual to the owner (objectId → object), lazily initialized like {@link #knownObjects}.
	 */
	protected volatile Map<Integer, VisibleObject> visualObjects;

	/**
	 * 所有者当前可见的玩家映射（懒初始化）。
	 * Players currently visual to the owner (lazily initialized).
	 */
	protected volatile Map<Integer, Player> visualPlayers;

	/**
	 * 更新锁，串行化 {@link #doUpdate()}。
	 * Update lock serializing {@link #doUpdate()}.
	 */
	private final ReentrantLock lock = new ReentrantLock();

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
		if (knownObjects != null) {
			knownObjects.clear();
		}
		if (knownPlayers != null) {
			knownPlayers.clear();
		}
		if (visualObjects != null) {
			visualObjects.clear();
		}
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
		Map<Integer, VisibleObject> objects = knownObjects;
		return objects != null && objects.containsKey(object.getObjectId());
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

		checkKnownObjectsInitialized();
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
	 * @param object 待添加的可见对象 / visual object to add
	 */
	public void addVisualObject(VisibleObject object) {
		checkVisibleObjectsInitialized();
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
		Map<Integer, VisibleObject> objects = knownObjects;
		if (objects != null && objects.remove(object.getObjectId()) != null) {
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
	 * @param object 待删除的可见对象 / visual object to remove
	 * @param isOutOfRange 是否因超出范围 / whether removal is due to range
	 */
	public void delVisualObject(VisibleObject object, boolean isOutOfRange) {
		Map<Integer, VisibleObject> objects = visualObjects;
		if (objects != null && objects.remove(object.getObjectId()) != null) {
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
			for (VisibleObject newObject : r.getObjects().values()) {
				if (newObject == owner || newObject == null) {
					continue;
				}
				if (!isAwareOf(newObject)) {
					continue;
				}
				Map<Integer, VisibleObject> objects = knownObjects;
				if (objects != null && objects.containsKey(newObject.getObjectId())) {
					continue;
				}
				if (!checkObjectInRange(newObject) && !newObject.getKnownList().checkReversedObjectInRange(owner)) {
					continue;
				}
				/**
				 * 新对象尚未被知晓 / New object is not known
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
	 * @param newObject 待检查的对象 / candidate object
	 * @return 应感知则返回 {@code true} / {@code true} if aware
	 */
	protected boolean isAwareOf(VisibleObject newObject) {
		return true;
	}

	/**
	 * 检查对象是否在所有者的可见距离内（含 Z 轴上限）。
	 * Checks whether the object is within the owner's visibility distance (including max Z).
	 *
	 * @param newObject 待检查的对象 / candidate object
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
	 * @param newObject 待检查的对象 / candidate object
	 * @return 默认返回 {@code false} / {@code false} by default
	 */
	protected boolean checkReversedObjectInRange(VisibleObject newObject) {
		return false;
	}

	/**
	 * 对所有已知 NPC 执行访问回调（无数量上限）。
	 * Visits all known NPCs without an iteration limit.
	 *
	 * @param visitor 访问回调 / visitor callback
	 */
	public void doOnAllNpcs(Visitor<Npc> visitor) {
		doOnAllNpcs(visitor, Integer.MAX_VALUE);
	}

	/**
	 * 对所有已知 NPC 执行访问回调，可限制最大遍历数。
	 * Visits known NPCs, optionally capped by an iteration limit.
	 *
	 * @param visitor 访问回调 / visitor callback
	 * @param iterationLimit 最大遍历数 / maximum iterations
	 * @return 实际遍历数量 / number of NPCs visited
	 */
	public int doOnAllNpcs(Visitor<Npc> visitor, int iterationLimit) {
		int counter = 0;
		// 记录当前访问对象，异常时才能定位到具体 NPC。 / Track the current target so a failure names the exact NPC.
		Npc current = null;
		try {
			for (VisibleObject newObject : knownObjectsSnapshot()) {
				if (newObject instanceof Npc npc) {
					current = npc;
					if ((++counter) == iterationLimit) {
						break;
					}
					visitor.visit(npc);
				}
			}
		} catch (Exception ex) {
			log.error(I18n.get("log.70e363d7c042", describe(owner), describe(current), counter), ex);
		}
		return counter;
	}

	/**
	 * 对所有已知 NPC 执行携带所有者的访问回调（无数量上限）。
	 * Visits all known NPCs with owner context, without an iteration limit.
	 *
	 * @param visitor 访问回调 / visitor callback
	 */
	public void doOnAllNpcsWithOwner(VisitorWithOwner<Npc, VisibleObject> visitor) {
		doOnAllNpcsWithOwner(visitor, Integer.MAX_VALUE);
	}

	/**
	 * 对所有已知 NPC 执行携带所有者的访问回调，可限制最大遍历数。
	 * Visits known NPCs with owner context, optionally capped by an iteration limit.
	 *
	 * @param visitor 访问回调 / visitor callback
	 * @param iterationLimit 最大遍历数 / maximum iterations
	 * @return 实际遍历数量 / number of NPCs visited
	 */
	public int doOnAllNpcsWithOwner(VisitorWithOwner<Npc, VisibleObject> visitor, int iterationLimit) {
		int counter = 0;
		// 同上，带所有者的遍历单独使用一条消息，便于区分两条报错路径。
		// As above; the owner variant keeps its own message so the two failure paths stay distinguishable.
		Npc current = null;
		try {
			for (VisibleObject newObject : knownObjectsSnapshot()) {
				if (newObject instanceof Npc npc) {
					current = npc;
					if ((++counter) == iterationLimit) {
						break;
					}
					visitor.visit(npc, owner);
				}
			}
		} catch (Exception ex) {
			log.error(I18n.get("log.113eb26bcfad", describe(owner), describe(current), counter), ex);
		}
		return counter;
	}

	/**
	 * 生成诊断用的对象标识：类型、对象 ID、名称与所在地图。
	 * Builds a diagnostic object identity: type, object id, name, and world id.
	 *
	 * <p>该方法只服务于异常兜底日志，自身不得再抛异常；名称不可用时降级为 ID。
	 * It only serves the failure-path log and must not throw, degrading to the id when the name is unusable.</p>
	 *
	 * @param object 目标对象，可为 null / target object, may be null
	 * @return 形如 {@code Npc#12345 Foo@210050000} 的标识 / identity such as {@code Npc#12345 Foo@210050000}
	 */
	private static String describe(VisibleObject object) {
		if (object == null) {
			return "null";
		}
		String identity = object.getClass().getSimpleName() + "#" + object.getObjectId();
		try {
			return identity + " " + object.getName() + "@" + object.getWorldId();
		} catch (RuntimeException nameUnavailable) {
			// 名字取不到时保留 ID，避免诊断日志再次失败。 / Keep the id when the name is unavailable.
			return identity;
		}
	}

	/**
	 * 对所有已知玩家执行访问回调。
	 * Visits all known players.
	 *
	 * @param visitor 访问回调 / visitor callback
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
			log.error(I18n.get("log.77c1d3d24013"), ex);
		}
	}

	/**
	 * 对所有已知对象执行访问回调。
	 * Visits all known objects.
	 *
	 * @param visitor 访问回调 / visitor callback
	 */
	public void doOnAllObjects(Visitor<VisibleObject> visitor) {
		try {
			for (VisibleObject newObject : knownObjectsSnapshot()) {
				if (newObject != null) {
					visitor.visit(newObject);
				}
			}
		} catch (Exception ex) {
			log.error(I18n.get("log.e15440de12ca"), ex);
		}
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
		Map<Integer, VisibleObject> objects = visualObjects;
		if (objects == null) {
			return new ArrayList<>();
		}
		synchronized (objects) {
			return new ArrayList<>(objects.values());
		}
	}

	/**
	 * 已知对象值的线程安全快照。
	 * Thread-safe snapshot of known-object values.
	 *
	 * <p>遍历已知列表必须走快照：容器虽为 {@link ConcurrentHashMap}（弱一致迭代器），
	 * 但访问回调会在遍历过程中增删对象，弱一致迭代会让“遍历期间新加入的对象”被立即访问，
	 * 破坏既有快照语义。该约定由 {@code KnownListIterationSafetyTest} 静态闸门守护。
	 * Iteration must go through a snapshot: although the container is a {@link ConcurrentHashMap}
	 * (weakly consistent iterator), visitors add/remove entries while iterating, and a weakly
	 * consistent iterator would immediately visit objects added during the visit. The rule is
	 * enforced by the {@code KnownListIterationSafetyTest} gate.</p>
	 *
	 * @return 快照列表 / snapshot list
	 */
	private List<VisibleObject> knownObjectsSnapshot() {
		Map<Integer, VisibleObject> objects = knownObjects;
		if (objects == null) {
			return new ArrayList<>();
		}
		synchronized (objects) {
			return new ArrayList<>(objects.values());
		}
	}

	/**
	 * 已知玩家值的线程安全快照。
	 * Thread-safe snapshot of known-player values.
	 *
	 * @return 快照列表 / snapshot list
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
		checkVisibleObjectsInitialized();
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
	final void checkKnownObjectsInitialized() {
		if (knownObjects == null) {
			synchronized (this) {
				if (knownObjects == null) {
					knownObjects = new ConcurrentHashMap<>();
				}
			}
		}
	}

	final void checkVisibleObjectsInitialized() {
		if (visualObjects == null) {
			synchronized (this) {
				if (visualObjects == null) {
					visualObjects = new ConcurrentHashMap<>();
				}
			}
		}
	}

	final void checkKnownPlayersInitialized() {
		if (knownPlayers == null) {
			synchronized (this) {
				if (knownPlayers == null) {
					knownPlayers = new ConcurrentHashMap<>();
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
					visualPlayers = new ConcurrentHashMap<>();
				}
			}
		}
	}

	/**
	 * 按对象 ID 获取已知对象。
	 * Returns a known object by object id.
	 *
	 * @param targetObjectId 目标对象 ID / target object id
	 * @return 已知对象，不存在则为 {@code null} / known object, or {@code null}
	 */
	public VisibleObject getObject(int targetObjectId) {
		Map<Integer, VisibleObject> objects = knownObjects;
		return objects == null ? null : objects.get(targetObjectId);
	}

	/**
	 * 返回已知对象映射（实时视图）；未初始化时按需创建，调用方拿到的永远是可写映射。
	 * Returns the known-objects map (live view), creating it on demand so callers never see null.
	 *
	 * @return 已知对象映射 / known objects map
	 */
	public Map<Integer, VisibleObject> getKnownObjects() {
		checkKnownObjectsInitialized();
		return knownObjects;
	}
}
