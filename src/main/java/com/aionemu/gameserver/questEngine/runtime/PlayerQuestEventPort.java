package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.configs.main.CraftConfig;
import com.aionemu.gameserver.configs.main.MembershipConfig;
import com.aionemu.gameserver.lifecycle.GameEventServices;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestMembershipPermission;
import com.aionemu.gameserver.questEngine.definition.QuestRewardKind;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Real {@link QuestEventPort}: freezes the pre-event player facts for one owner. */
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
		Objects.requireNonNull(connection, "connection");
		Objects.requireNonNull(event, "event");
		Objects.requireNonNull(eventActivityQuestIds, "eventActivityQuestIds");
		Player player = players.find(playerId);
		if (player == null) {
			throw new SQLException("player is unavailable: " + playerId);
		}
		QuestSnapshot snapshot = snapshotOf(player, questId);
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
			// Only TalkToNpc carries an authoritative dialog owner. Every other
			// event must use object 0 for dialog actions rather than guessing from
			// the player's current target or an item/NPC template id.
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
	private QuestSnapshot snapshotOf(Player player, int questId) {
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
			craftFactsOf(player), null).withWorldFacts(worldFactsOf(player))
			.withTeamFacts(new QuestTeamFacts(player.isInGroup2(), player.isInAlliance2()))
			.withCompletedQuestIds(completedQuestIdsOf(player))
			.withActiveQuestIds(activeQuestIdsOf(player))
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

	/** Captures quest states currently in progress (START or REWARD); absent states are not active. */
	private static Set<Integer> activeQuestIdsOf(Player player) {
		Set<Integer> active = new HashSet<>();
		for (QuestState questState : player.getQuestStateList().getAllQuestState()) {
			if (questState != null && questState.getQuestId() > 0) {
				QuestStatus status = questState.getStatus();
				if (status == QuestStatus.START || status == QuestStatus.REWARD) {
					active.add(questState.getQuestId());
				}
			}
		}
		return Set.copyOf(active);
	}

	/** Captures only quest states that are explicitly COMPLETE; absent states are not completed. */
	private static Set<Integer> completedQuestIdsOf(Player player) {
		Set<Integer> completed = new HashSet<>();
		for (QuestState questState : player.getQuestStateList().getAllFinishedQuests()) {
			if (questState != null && questState.getQuestId() > 0) {
				completed.add(questState.getQuestId());
			}
		}
		return Set.copyOf(completed);
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
		Map<Integer, Integer> counts = new HashMap<>();
		for (Item item : items) {
			if (item != null && item.getItemId() > 0 && item.getItemCount() > 0) {
				counts.merge(item.getItemId(), (int) item.getItemCount(), Integer::sum);
			}
		}
		return Map.copyOf(counts);
	}

	/**
	 * 只读投影任务运行时可可靠获取的货币余额。
	 * Project only currency balances that the quest runtime can read reliably.
	 */
	private static Map<QuestRewardKind, Long> currenciesOf(Player player) {
		Map<QuestRewardKind, Long> balances = new HashMap<>();
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
		return Map.copyOf(balances);
	}
}
