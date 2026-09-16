package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.configs.main.CraftConfig;
import com.aionemu.gameserver.configs.main.MembershipConfig;
import com.aionemu.gameserver.lifecycle.GameEventServices;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestMembershipPermission;
import com.aionemu.gameserver.questEngine.definition.QuestRewardKind;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** 真实 {@link QuestEventPort}：为单个任务拥有者冻结事件前玩家事实。 / Real {@link QuestEventPort}: freezes the pre-event player facts for one owner. */
public final class PlayerQuestEventPort implements QuestEventPort {
	@FunctionalInterface
	interface EventActivitySource {
		boolean isActive(int questId);
	}

	private final QuestPlayerPort players;
	private final QuestStartEligibilityPort startEligibilityPort;
	private final EventActivitySource eventActivitySource;

	public PlayerQuestEventPort(QuestPlayerPort players) {
		this(players, null, questId -> GameEventServices.eventService().checkQuestIsActive(questId));
	}

	public PlayerQuestEventPort(QuestPlayerPort players, QuestStartEligibilityPort startEligibilityPort) {
		this(players, startEligibilityPort,
			questId -> GameEventServices.eventService().checkQuestIsActive(questId));
	}

	PlayerQuestEventPort(QuestPlayerPort players, QuestStartEligibilityPort startEligibilityPort,
			EventActivitySource eventActivitySource) {
		this.players = Objects.requireNonNull(players, "players");
		this.startEligibilityPort = startEligibilityPort;
		this.eventActivitySource = Objects.requireNonNull(eventActivitySource, "eventActivitySource");
	}

	@Override
	public QuestSnapshot snapshot(Connection connection, int playerId, int questId, QuestEvent event)
			throws SQLException {
		return snapshot(connection, playerId, questId, event, false);
	}

	@Override
	public QuestSnapshot snapshot(Connection connection, int playerId, int questId, QuestEvent event,
			boolean includeStartEligibility) throws SQLException {
		return snapshot(connection, playerId, questId, event, includeStartEligibility, Set.of());
	}

	@Override
	public QuestSnapshot snapshot(Connection connection, int playerId, int questId, QuestEvent event,
			boolean includeStartEligibility, Set<Integer> eventActivityQuestIds) throws SQLException {
		return snapshot(playerId, questId, event, includeStartEligibility, eventActivityQuestIds);
	}

	@Override
	public QuestSnapshot snapshot(int playerId, int questId, QuestEvent event,
			boolean includeStartEligibility, Set<Integer> eventActivityQuestIds) throws SQLException {
		return snapshot(playerId, questId, event, includeStartEligibility, eventActivityQuestIds, true);
	}

	@Override
	public QuestSnapshot snapshot(int playerId, int questId, QuestEvent event,
			boolean includeStartEligibility, Set<Integer> eventActivityQuestIds,
			boolean includeWorldFacts) throws SQLException {
		Objects.requireNonNull(event, "event");
		Objects.requireNonNull(eventActivityQuestIds, "eventActivityQuestIds");
		Player player = players.find(playerId);
		if (player == null) {
			throw new SQLException("player is unavailable: " + playerId);
		}
		QuestSnapshot snapshot = snapshotOf(player, questId, includeWorldFacts);
		Map<Integer, Boolean> eventActivities = eventActivitiesOf(eventActivityQuestIds);
		if (eventActivities != null) {
			snapshot = snapshot.withEventActivities(eventActivities);
		}
		snapshot = enrich(snapshot, event);
		if (includeStartEligibility && startEligibilityPort != null) {
			snapshot = snapshot.withStartEligibility(startEligibilityPort.snapshot(playerId, questId, event));
		}
		PlayerCommonData commonData = player.getCommonData();
		if (commonData != null) {
			if (commonData.getPlayerClass() != null) {
				PlayerClass actualClass = commonData.getPlayerClass();
				snapshot = snapshot.withStartingClass(PlayerClass.getStartingClassFor(actualClass))
					.withPlayerClass(actualClass);
			}
			if (commonData.getGender() != null) {
				snapshot = snapshot.withGender(commonData.getGender());
			}
			if (commonData.getRace() != null) {
				snapshot = snapshot.withRace(commonData.getRace());
			}
		}
		snapshot = snapshot.withTeamFacts(new QuestTeamFacts(player.isInGroup2(), player.isInAlliance2()));
		return switch (event) {
			case QuestEvent.TalkToNpc talk -> snapshot.withInteractionObjectId(talk.interactionObjectId());
			case QuestEvent.AttackNpc attack when attack.facts() != null
					&& attack.facts().attackerId() == playerId -> snapshot
				.withInteractionObjectId(attack.facts().npcObjectId())
				.withTargetlessDialog();
			// 只有 TalkToNpc 携带权威的对话所有者。AttackNpc 可携带权威事件 NPC，供提交后生命周期动作使用，
			// 但仍必须标记为无对话目标；其他事件也不能从玩家当前目标或模板 ID 猜测对话对象。
			// Only TalkToNpc carries an authoritative dialog owner. AttackNpc may carry an
			// authoritative event NPC for post-commit lifecycle actions, but remains dialog-targetless.
			// Other events must not guess a dialog object from the player's current target or a template id.
			default -> snapshot.withTargetlessDialog();
		};
	}

	/** Applies only server-attached event facts; definition events have no mutable facts. */
	static QuestSnapshot enrich(QuestSnapshot snapshot, QuestEvent event) {
		if (event instanceof QuestEvent.KillRanked ranked && ranked.facts() != null) {
			return snapshot.withPvpFacts(ranked.facts());
		}
		if (event instanceof QuestEvent.KillInWorld world && world.facts() != null) {
			return snapshot.withPvpFacts(world.facts());
		}
		return snapshot;
	}

	/**
	 * 从在线玩家冻结一个任务事件快照，不修改玩家状态。
	 * Freeze one quest-event snapshot from an online player without mutating player state.
	 */
	private QuestSnapshot snapshotOf(Player player, int questId, boolean includeWorldFacts) {
		QuestState state = player.getQuestStateList().getQuestState(questId);
		QuestStatus status = state == null ? QuestStatus.NONE : state.getStatus();
		int packed = state == null ? 0 : state.getQuestVars().getQuestVars();
		Storage inventory = player.getInventory();
		boolean inventoryCaptured = inventory != null;
		// 货币条件要求所有持久化货币来源都已捕获；部分玩家投影不能把未知 AP/DP 当成 0。
		// Currency conditions require every persistent source to be captured; a partial
		// player projection must never turn an unavailable AP/DP balance into zero.
		boolean currenciesCaptured = inventory != null && player.getCommonData() != null
			&& player.getAbyssRank() != null;
		var target = player.getTarget();
		boolean positionCaptured = player.getPosition() != null;
		QuestIdFacts questIds = questIdsOf(player);
		QuestSnapshot snapshot = new QuestSnapshot(player.getObjectId(), questId, status, packed,
			inventoryCaptured ? inventoryOf(player) : null,
			currenciesCaptured ? currenciesOf(player) : null,
			inventoryCaptured, currenciesCaptured, 0, target == null ? 0 : target.getObjectId(),
			positionCaptured ? player.getWorldId() : 0,
			positionCaptured ? player.getInstanceId() : 0,
			positionCaptured ? player.getX() : 0f,
			positionCaptured ? player.getY() : 0f,
			positionCaptured ? player.getZ() : 0f,
			positionCaptured ? player.getHeading() : (byte) 0,
			craftFactsOf(player), null).withWorldFacts(includeWorldFacts ? worldFactsOf(player) : null)
			.withTeamFacts(new QuestTeamFacts(player.isInGroup2(), player.isInAlliance2()))
			.withCompletedQuestIds(questIds.completed())
			.withActiveQuestIds(questIds.active())
			.withCompleteCount(state == null ? 0 : state.getCompleteCount());
		snapshot = snapshot.withEventActive(eventActiveOf(questId));
		QuestEquipmentFacts equipmentFacts = equipmentFactsOf(player);
		if (equipmentFacts != null) {
			snapshot = snapshot.withEquipmentFacts(equipmentFacts);
		}
		Integer maxDp = maxDpOf(player);
		if (maxDp != null) {
			snapshot = snapshot.withMaxDp(maxDp);
		}
		QuestMembershipFacts membershipFacts = membershipFactsOf(player);
		if (membershipFacts != null) {
			snapshot = snapshot.withMembershipFacts(membershipFacts);
		}
		return snapshot;
	}

	/** Event data may be unavailable during partial startup or isolated tests; preserve unknown facts. */
	private Boolean eventActiveOf(int questId) {
		try {
			return eventActivitySource.isActive(questId);
		} catch (RuntimeException | LinkageError unavailable) {
			return null;
		}
	}

	/** Event data may be unavailable during partial startup; preserve unknown facts as uncaptured. */
	private Map<Integer, Boolean> eventActivitiesOf(Set<Integer> questIds) {
		if (questIds.isEmpty()) {
			return Map.of();
		}
		try {
			Map<Integer, Boolean> activities = new HashMap<>();
			for (int questId : questIds) {
				if (questId <= 0) {
					throw new IllegalArgumentException("event activity quest ids must be positive");
				}
				activities.put(questId, eventActivitySource.isActive(questId));
			}
			return Map.copyOf(activities);
		} catch (RuntimeException | LinkageError unavailable) {
			return null;
		}
	}

	/** Captures the equipment-set part counts used by equipment-dependent quests. */
	private static QuestEquipmentFacts equipmentFactsOf(Player player) {
		if (player.getEquipment() == null) {
			return null;
		}
		Map<Integer, Integer> parts = new HashMap<>();
		for (int setId : List.of(6, 7, 8, 9, 378)) {
			parts.put(setId, player.getEquipment().itemSetPartsEquipped(setId));
		}
		Map<Integer, Integer> equippedItems = new HashMap<>();
		for (Item item : player.getEquipment().getEquippedItems()) {
			if (item != null && item.getItemId() > 0) {
				equippedItems.merge(item.getItemId(), 1, Integer::sum);
			}
		}
		return new QuestEquipmentFacts(parts, equippedItems);
	}

	/** Captures only permissions with a typed quest-runtime source. */
	private static QuestMembershipFacts membershipFactsOf(Player player) {
		if (player.getPlayerAccount() == null) {
			return null;
		}
		Set<QuestMembershipPermission> granted = new HashSet<>();
		if (player.havePermission(MembershipConfig.STIGMA_SLOT_QUEST)) {
			granted.add(QuestMembershipPermission.STIGMA_SLOT_QUEST);
		}
		return new QuestMembershipFacts(granted);
	}

	/** Captures the live maximum DP when player stat projection is available. */
	private static Integer maxDpOf(Player player) {
		if (player.getGameStats() == null || player.getGameStats().getMaxDp() == null) {
			return null;
		}
		int maxDp = player.getGameStats().getMaxDp().getCurrent();
		return maxDp < 0 ? null : maxDp;
	}

	/**
	 * 已完成与进行中任务 ID 的不可变事实。
	 * Immutable facts for completed and in-progress quest ids.
	 *
	 * @param completed 已完成任务 ID / completed quest ids
	 * @param active    进行中任务 ID / in-progress quest ids
	 */
	private record QuestIdFacts(Set<Integer> completed, Set<Integer> active) {
	}

	/**
	 * 单次遍历任务状态视图，直接构造已完成/进行中任务 ID 的不可变集合。
	 * Builds the immutable completed/in-progress quest-id sets from a single pass over the quest-state view.
	 *
	 * <p>任务状态视图来自 {@link QuestStateList} 的 TreeMap：键唯一且按任务 ID 升序，因此不需要哈希去重。
	 * 旧实现先建可变 {@code HashSet}，再由 {@code QuestSnapshot} 紧凑构造器的 {@code Set.copyOf} 复制一遍
	 * （内部分配第二套哈希表），同一个快照要为同一批 ID 付两份哈希表与节点（JFR 实测 31MB/300s）；这里按
	 * 任务状态总数预留 {@code int} 缓冲、单次遍历收集，装箱成等长数组后交给 {@code Set.of} 建表一次。
	 * The view is a TreeMap (unique keys, ascending), so no hash-based dedup is needed. The old code built a
	 * mutable {@code HashSet} and then let {@code QuestSnapshot}'s canonical constructor {@code Set.copyOf} it
	 * again (a second hash table internally), paying twice for the same ids (31MB/300s measured). This version
	 * pre-sizes {@code int} buffers from the state count, collects in one pass and lets {@code Set.of} lay out
	 * the table once from the boxed ids.</p>
	 *
	 * @param player 玩家 / the player
	 * @return 不可变的已完成/进行中任务 ID / immutable completed and in-progress quest ids
	 */
	private static QuestIdFacts questIdsOf(Player player) {
		QuestStateList questStates = player.getQuestStateList();
		// 状态总数是本次收集的上界（同一线程内视图不变），据此预留避免扩容。
		// The state count bounds this collection (the view does not change on this thread), so the buffers never grow.
		int capacity = Math.max(4, questStates.size());
		int[] completed = new int[capacity];
		int[] active = new int[capacity];
		int completedCount = 0;
		int activeCount = 0;
		for (QuestState questState : questStates.getAllQuestState()) {
			if (questState == null || questState.getQuestId() <= 0) {
				continue;
			}
			QuestStatus status = questState.getStatus();
			if (status == QuestStatus.COMPLETE) {
				completed = ensureQuestIdCapacity(completed, completedCount);
				completed[completedCount++] = questState.getQuestId();
			} else if (status == QuestStatus.START || status == QuestStatus.REWARD) {
				active = ensureQuestIdCapacity(active, activeCount);
				active[activeCount++] = questState.getQuestId();
			}
		}
		// 已不可变，QuestSnapshot 构造器走“已校验”快路径，不会再次复制。 / Already immutable, so QuestSnapshot's constructor takes the validated fast path without copying again.
		return new QuestIdFacts(questIdSet(completed, completedCount), questIdSet(active, activeCount));
	}

	/**
	 * 保证任务 ID 缓冲可写入下一个位置。
	 * Ensures the quest-id buffer can accept the next entry.
	 *
	 * @param ids  当前缓冲 / current buffer
	 * @param size 已写入数量 / number of recorded ids
	 * @return 可写入的缓冲 / a buffer with room for one more id
	 */
	private static int[] ensureQuestIdCapacity(int[] ids, int size) {
		return size < ids.length ? ids : Arrays.copyOf(ids, Math.max(4, ids.length * 2));
	}

	/**
	 * 把已收集的任务 ID 装箱成不可变集合；空集合复用 {@code Set.of()}。
	 * Boxes the collected quest ids into an immutable set; an empty collection reuses {@code Set.of()}.
	 *
	 * @param ids  任务 ID 缓冲 / quest-id buffer
	 * @param size 已写入数量 / number of recorded ids
	 * @return 不可变任务 ID 集合 / immutable quest-id set
	 */
	private static Set<Integer> questIdSet(int[] ids, int size) {
		if (size == 0) {
			return Set.of();
		}
		Integer[] boxed = new Integer[size];
		for (int i = 0; i < size; i++) {
			boxed[i] = ids[i];
		}
		return Set.of(boxed);
	}

	/** Captures NPC template presence in the player's current world instance. */
	private static QuestWorldFacts worldFactsOf(Player player) {
		var position = player.getPosition();
		if (position == null || !position.isSpawned() || position.getWorldMapInstance() == null
				|| position.getMapRegion() == null) {
			return null;
		}
		Set<Integer> npcTemplateIds = new HashSet<>();
		for (Npc npc : position.getWorldMapInstance().getNpcs()) {
			if (npc != null && npc.getNpcId() > 0) {
				npcTemplateIds.add(npc.getNpcId());
			}
		}
		Set<String> zoneNames = new HashSet<>();
		for (var zone : position.getMapRegion().getZones(player)) {
			if (zone == null || zone.getAreaTemplate() == null) {
				continue;
			}
			var zoneName = zone.getAreaTemplate().getZoneName();
			if (zoneName != null) {
				zoneNames.add(zoneName.name());
			}
		}
		return new QuestWorldFacts(npcTemplateIds, zoneNames);
	}

	/**
	 * 只采集玩家持有的制作事实；静态配方模板仍由制作端口负责。
	 * Capture only player-owned craft facts; static recipe templates remain owned by the craft port.
	 */
	private static QuestCraftSnapshot craftFactsOf(Player player) {
		if (player.getRecipeList() == null || player.getSkillList() == null) {
			return null;
		}
		Map<Integer, Integer> skillLevels = new HashMap<>();
		for (var skill : player.getSkillList().getAllSkills()) {
			if (skill != null && player.getSkillList().isCraftSkill(skill.getSkillId())) {
				skillLevels.put(skill.getSkillId(), skill.getSkillLevel());
			}
		}
		return new QuestCraftSnapshot(player.getRecipeList().getRecipeList(), skillLevels, 1600,
			CraftConfig.MAX_EXPERT_CRAFTING_SKILLS, CraftConfig.MAX_MASTER_CRAFTING_SKILLS);
	}

	/**
	 * 将玩家背包只读投影为“物品 ID 到总数量”的映射。
	 * Project the player's inventory read-only as item id to total count.
	 */
	private static Map<Integer, Integer> inventoryOf(Player player) {
		Storage inventory = player.getInventory();
		return inventory == null ? Map.of() : toInventoryMap(inventory.getItems());
	}

	/**
	 * 汇总有效物品条目；跳过空值及非正数量。
	 * Aggregate valid item entries while skipping nulls and non-positive counts.
	 */
	private static Map<Integer, Integer> toInventoryMap(List<Item> items) {
		if (items == null) {
			return Map.of();
		}
		// 预分配容量按条目数估算，避免逐次扩容。 / Pre-size from the entry count so the table never has to grow.
		Map<Integer, Integer> counts = new HashMap<>((int) (items.size() / 0.75f) + 1);
		for (Item item : items) {
			if (item != null && item.getItemId() > 0 && item.getItemCount() > 0) {
				counts.merge(item.getItemId(), (int) item.getItemCount(), Integer::sum);
			}
		}
		// 返回可变 Map，交由 QuestSnapshot 构造器统一校验与不可变化。 / Return the mutable map; QuestSnapshot validates and freezes it once.
		return counts;
	}

	/**
	 * 只读投影任务运行时可可靠获取的货币余额。
	 * Project only currency balances that the quest runtime can read reliably.
	 */
	private static Map<QuestRewardKind, Long> currenciesOf(Player player) {
		// 至多 GOLD/DP/AP/GP 四项，固定小容量避免扩容。 / At most GOLD/DP/AP/GP, so a fixed small capacity avoids growth.
		Map<QuestRewardKind, Long> balances = new HashMap<>(8);
		Storage inventory = player.getInventory();
		if (inventory != null && inventory.getKinah() > 0) {
			balances.put(QuestRewardKind.GOLD, inventory.getKinah());
		}
		if (player.getCommonData() != null && player.getCommonData().getDp() > 0) {
			balances.put(QuestRewardKind.DP, (long) player.getCommonData().getDp());
		}
		if (player.getAbyssRank() != null) {
			if (player.getAbyssRank().getAp() > 0) {
				balances.put(QuestRewardKind.AP, (long) player.getAbyssRank().getAp());
			}
			if (player.getAbyssRank().getGp() > 0) {
				balances.put(QuestRewardKind.GP, (long) player.getAbyssRank().getGp());
			}
		}
		// 同上：避免在端口侧再复制一份。 / As above: no extra copy on the port side.
		return balances;
	}
}
