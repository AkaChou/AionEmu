package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.configs.main.CraftConfig;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
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
	private final QuestPlayerPort players;
	private final QuestStartEligibilityPort startEligibilityPort;

	public PlayerQuestEventPort(QuestPlayerPort players) {
		this(players, null);
	}

	public PlayerQuestEventPort(QuestPlayerPort players, QuestStartEligibilityPort startEligibilityPort) {
		this.players = Objects.requireNonNull(players, "players");
		this.startEligibilityPort = startEligibilityPort;
	}

	@Override
	public QuestSnapshot snapshot(Connection connection, int playerId, int questId, QuestEvent event)
			throws SQLException {
		Objects.requireNonNull(connection, "connection");
		Objects.requireNonNull(event, "event");
		Player player = players.find(playerId);
		if (player == null) {
			throw new SQLException("player is unavailable: " + playerId);
		}
		QuestSnapshot snapshot = snapshotOf(player, questId);
		snapshot = enrich(snapshot, event);
		if (startEligibilityPort != null) {
			snapshot = snapshot.withStartEligibility(startEligibilityPort.snapshot(playerId, questId));
		}
		PlayerCommonData commonData = player.getCommonData();
		if (commonData != null) {
			snapshot = snapshot.withStartingClass(
				PlayerClass.getStartingClassFor(commonData.getPlayerClass()));
		}
		return switch (event) {
			case QuestEvent.TalkToNpc talk -> snapshot.withInteractionObjectId(talk.interactionObjectId());
			case QuestEvent.UseItem _ -> snapshot.withTargetlessDialog();
			case QuestEvent.QuestDialog _ -> snapshot.withTargetlessDialog();
			default -> snapshot;
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
	private static QuestSnapshot snapshotOf(Player player, int questId) {
		QuestState state = player.getQuestStateList().getQuestState(questId);
		QuestStatus status = state == null ? QuestStatus.NONE : state.getStatus();
		int packed = state == null ? 0 : state.getQuestVars().getQuestVars();
		Storage inventory = player.getInventory();
		boolean inventoryCaptured = inventory != null;
		boolean currenciesCaptured = inventory != null || player.getCommonData() != null
			|| player.getAbyssRank() != null;
		var target = player.getTarget();
		boolean positionCaptured = player.getPosition() != null;
		return new QuestSnapshot(player.getObjectId(), questId, status, packed,
			inventoryCaptured ? inventoryOf(player) : null,
			currenciesCaptured ? currenciesOf(player) : null,
			inventoryCaptured, currenciesCaptured, 0, target == null ? 0 : target.getObjectId(),
			positionCaptured ? player.getWorldId() : 0,
			positionCaptured ? player.getInstanceId() : 0,
			positionCaptured ? player.getX() : 0f,
			positionCaptured ? player.getY() : 0f,
			positionCaptured ? player.getZ() : 0f,
			positionCaptured ? player.getHeading() : (byte) 0,
			craftFactsOf(player), null).withWorldFacts(worldFactsOf(player));
	}

	/** Captures NPC template presence in the player's current world instance. */
	private static QuestWorldFacts worldFactsOf(Player player) {
		var position = player.getPosition();
		if (position == null || !position.isSpawned() || position.getWorldMapInstance() == null) {
			return null;
		}
		Set<Integer> npcTemplateIds = new HashSet<>();
		for (Npc npc : position.getWorldMapInstance().getNpcs()) {
			if (npc != null && npc.getNpcId() > 0) {
				npcTemplateIds.add(npc.getNpcId());
			}
		}
		return new QuestWorldFacts(npcTemplateIds);
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
