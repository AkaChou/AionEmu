package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.configs.main.CraftConfig;
import com.aionemu.gameserver.configs.main.MembershipConfig;
import com.aionemu.gameserver.lifecycle.GameEventServices;
import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestMembershipPermission;
import com.aionemu.gameserver.questEngine.definition.QuestPvpKillFacts;
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
		// 旧入口只声明三个门控：其余事实族保守全采，行为与改动前一致。
		// The legacy entry point declares only three gates, so every other family stays captured.
		return snapshot(playerId, questId, event,
			QuestFactRequirements.conservative(includeStartEligibility, eventActivityQuestIds, includeWorldFacts));
	}

	@Override
	public QuestSnapshot snapshot(int playerId, int questId, QuestEvent event,
			QuestFactRequirements requirements) throws SQLException {
		Objects.requireNonNull(event, "event");
		Objects.requireNonNull(requirements, "requirements");
		Player player = players.find(playerId);
		if (player == null) {
			throw new SQLException("player is unavailable: " + playerId);
		}
		// 所有事实先算好再一次构造：既避免每个 withXxx 重建一个 record，也让「本转换不读的事实族」
		// 根本不进入快照。 / Every fact is computed before a single construction: no per-withXxx record
		// rebuild, and families this transition never reads never enter the snapshot at all.
		return freeze(player, questId, event, requirements);
	}

	/**
	 * 从在线玩家冻结一个任务事件快照，不修改玩家状态；只采集 requirements 声明的事实族。
	 * Freezes one quest-event snapshot from an online player without mutating player state, capturing only the
	 * fact families declared by the requirements.
	 *
	 * @param player       在线玩家 / the online player
	 * @param questId      任务 ID / quest id
	 * @param event        事件 / the event
	 * @param requirements 事实需求 / required fact families
	 * @return 冻结快照 / the frozen snapshot
	 */
	private QuestSnapshot freeze(Player player, int questId, QuestEvent event,
			QuestFactRequirements requirements) throws SQLException {
		QuestState state = player.getQuestStateList().getQuestState(questId);
		QuestStatus status = state == null ? QuestStatus.NONE : state.getStatus();
		int packed = state == null ? 0 : state.getQuestVars().getQuestVars();
		Storage inventory = player.getInventory();
		boolean inventoryCaptured = inventory != null && requirements.inventory();
		// 货币条件要求所有持久化货币来源都已捕获；部分玩家投影不能把未知 AP/DP 当成 0。
		// Currency conditions require every persistent source to be captured; a partial
		// player projection must never turn an unavailable AP/DP balance into zero.
		boolean currenciesCaptured = inventory != null && player.getCommonData() != null
			&& player.getAbyssRank() != null;
		var target = player.getTarget();
		boolean positionCaptured = player.getPosition() != null;

		// 事件级事实：PvP 事实只来自服务端附加的击杀事件，对话所有者只由 TalkToNpc/AttackNpc 提供。
		// Event-scoped facts: PvP facts come only from server-attached kill events, and only TalkToNpc or
		// AttackNpc supplies a dialog owner.
		Boolean eventActive = eventActiveOf(questId);
		Map<Integer, Boolean> eventActivities = eventActivitiesOf(requirements.eventActivityQuestIds());
		QuestPvpKillFacts pvpFacts = pvpFactsOf(event, player.getObjectId());
		QuestStartEligibility startEligibility = requirements.startEligibility() && startEligibilityPort != null
			? startEligibilityPort.snapshot(player.getObjectId(), questId, event) : null;
		PlayerCommonData commonData = player.getCommonData();
		PlayerClass playerClass = commonData == null ? null : commonData.getPlayerClass();
		PlayerClass startingClass = playerClass == null ? null : PlayerClass.getStartingClassFor(playerClass);
		Gender gender = commonData == null ? null : commonData.getGender();
		Race race = commonData == null ? null : commonData.getRace();
		boolean targetlessDialog = true;
		int interactionObjectId = 0;
		if (event instanceof QuestEvent.TalkToNpc talk) {
			// 只有 TalkToNpc 携带权威的对话所有者。AttackNpc 可携带权威事件 NPC，供提交后生命周期动作使用，
			// 但仍必须标记为无对话目标；其他事件也不能从玩家当前目标或模板 ID 猜测对话对象。
			// Only TalkToNpc carries an authoritative dialog owner. AttackNpc may carry an authoritative event
			// NPC for post-commit lifecycle actions, but remains dialog-targetless. Other events must not guess
			// a dialog object from the player's current target or a template id.
			interactionObjectId = talk.interactionObjectId();
			targetlessDialog = false;
		} else if (event instanceof QuestEvent.AttackNpc attack && attack.facts() != null
				&& attack.facts().attackerId() == player.getObjectId()) {
			interactionObjectId = attack.facts().npcObjectId();
		}

		// 玩家级事实：只采集本转换真正读取的事实族；未采集的事实族保持未捕获并由读取方 fail-closed。
		// Player-scoped facts: capture only the families this transition reads; skipped families stay
		// uncaptured and fail closed on the reading side.
		Set<Integer> completedQuestIds = null;
		Set<Integer> activeQuestIds = null;
		if (requirements.questIdSets()) {
			QuestIdFacts questIds = questIdsOf(player);
			completedQuestIds = questIds.completed();
			activeQuestIds = questIds.active();
		}
		return new QuestSnapshot(player.getObjectId(), questId, status, packed,
			inventoryCaptured ? inventoryOf(player) : null,
			currenciesCaptured ? currenciesOf(player) : null,
			inventoryCaptured, currenciesCaptured, interactionObjectId,
			target == null ? 0 : target.getObjectId(),
			positionCaptured ? player.getWorldId() : 0,
			positionCaptured ? player.getInstanceId() : 0,
			positionCaptured ? player.getX() : 0f,
			positionCaptured ? player.getY() : 0f,
			positionCaptured ? player.getZ() : 0f,
			positionCaptured ? player.getHeading() : (byte) 0,
			requirements.craft() ? craftFactsOf(player) : null,
			pvpFacts,
			startEligibility,
			startingClass,
			playerClass,
			gender,
			targetlessDialog,
			requirements.worldFacts() ? worldFactsOf(player) : null,
			new QuestTeamFacts(player.isInGroup2(), player.isInAlliance2()),
			state == null ? 0 : state.getCompleteCount(),
			completedQuestIds,
			requirements.equipment() ? equipmentFactsOf(player) : null,
			maxDpOf(player),
			membershipFactsOf(player),
			eventActive,
			activeQuestIds,
			race,
			eventActivities);
	}

	/**
	 * 只接受服务端附加的 PvP 击杀事实；事实不属于该玩家时保持原异常。
	 * Accepts only server-attached PvP kill facts and keeps the original failure when they belong to another player.
	 *
	 * @param event    事件 / the event
	 * @param playerId 拥有者 / the owner
	 * @return PvP 事实或 null / the PvP facts, or null
	 */
	private static QuestPvpKillFacts pvpFactsOf(QuestEvent event, int playerId) {
		QuestPvpKillFacts facts;
		switch (event) {
			case QuestEvent.KillRanked ranked:
				facts = ranked.facts();
				break;
			case QuestEvent.KillInWorld world:
				facts = world.facts();
				break;
			default:
				facts = null;
				break;
		}
		if (facts == null) {
			return null;
		}
		if (facts.recipientId() != playerId) {
			throw new IllegalArgumentException("PvP facts do not belong to this player snapshot");
		}
		return facts;
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
