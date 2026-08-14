package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestItemRequirement;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.definition.QuestStartCondition;
import com.aionemu.gameserver.questEngine.definition.QuestStartConditionGroup;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.LimitedQuestService;
import com.aionemu.gameserver.services.craft.CraftSkillUpdateService;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.IntFunction;

/** 仅由规范目录元数据支撑的生产开始资格投影。 / Production start-eligibility projection backed only by canonical catalog metadata. */
public final class PlayerQuestStartEligibilityPort implements QuestStartEligibilityPort {
	private final QuestPlayerPort players;
	private final IntFunction<QuestMetadata> metadataByQuest;
	private final BiPredicate<Integer, QuestMetadata> limitedQuestRequiresAcquisition;

	public PlayerQuestStartEligibilityPort(QuestPlayerPort players) {
		this(players,
			questId -> GameEngineServices.questEngine().questCatalog().findMetadata(questId).orElse(null),
			(questId, metadata) -> LimitedQuestService.requiresAcquisitionByMetadata(questId,
				metadata.maxCountLimitedQuest()));
	}

	public PlayerQuestStartEligibilityPort(QuestPlayerPort players, IntFunction<QuestMetadata> metadataByQuest) {
		this(players, metadataByQuest,
			(questId, metadata) -> LimitedQuestService.requiresAcquisitionByMetadata(questId,
				metadata.maxCountLimitedQuest()));
	}

	PlayerQuestStartEligibilityPort(QuestPlayerPort players, IntFunction<QuestMetadata> metadataByQuest,
			BiPredicate<Integer, QuestMetadata> limitedQuestRequiresAcquisition) {
		this.players = Objects.requireNonNull(players, "players");
		this.metadataByQuest = Objects.requireNonNull(metadataByQuest, "metadataByQuest");
		this.limitedQuestRequiresAcquisition = Objects.requireNonNull(limitedQuestRequiresAcquisition,
			"limitedQuestRequiresAcquisition");
	}

	@Override
	public QuestStartEligibility snapshot(int playerId, int questId, QuestEvent event) throws SQLException {
		return snapshot(playerId, questId, event, 0);
	}

	/** Evaluates a candidate before the NPC-faction rotation has assigned it to the player. */
	public QuestStartEligibility snapshotNpcFactionRotation(int playerId, int questId, int npcFactionId)
			throws SQLException {
		if (npcFactionId <= 0) {
			throw new IllegalArgumentException("npcFactionId must be positive");
		}
		return snapshot(playerId, questId, new QuestEvent.LevelUp(), npcFactionId);
	}

	private QuestStartEligibility snapshot(int playerId, int questId, QuestEvent event,
			int selectingNpcFactionId) throws SQLException {
		Objects.requireNonNull(event, "event");
		Player player = players.find(playerId);
		if (player == null) {
			throw new SQLException("player is unavailable: " + playerId);
		}
		QuestMetadata metadata = metadataByQuest.apply(questId);
		if (metadata == null) {
			return QuestStartEligibility.rejected("QUEST_METADATA_MISSING");
		}
		QuestState existing = player.getQuestStateList().getQuestState(questId);
		if (existing != null && existing.getStatus() != QuestStatus.NONE && !existing.canRepeat(metadata)) {
			return QuestStartEligibility.rejected("QUEST_ALREADY_ACTIVE");
		}

		boolean npcFactionRotation = false;
		if (metadata.npcFactionId() != 0) {
			if (selectingNpcFactionId != 0 && metadata.npcFactionId() != selectingNpcFactionId) {
				return QuestStartEligibility.rejected("NPC_FACTION_MISMATCH");
			}
			var factions = player.getNpcFactions();
			var faction = factions == null ? null : factions.getNpcFactionById(metadata.npcFactionId());
			if (faction == null || !faction.isActive()
					|| (selectingNpcFactionId == 0 && faction.getQuestId() != questId)) {
				return QuestStartEligibility.rejected("NPC_FACTION_QUEST_NOT_ACTIVE");
			}
			npcFactionRotation = true;
			if (!isTimeBased(metadata) && !factions.canStartQuest(!"NONE".equals(metadata.mentorType()))) {
				return QuestStartEligibility.rejected("NPC_FACTION_QUEST_COOLDOWN");
			}
		}
		if (metadata.minLevel() == 999 && !npcFactionRotation) {
			return QuestStartEligibility.rejected("SENTINEL_LEVEL_REQUIRES_NPC_FACTION");
		}
		if (metadata.minLevel() != 999 && player.getLevel() < metadata.minLevel()) {
			return QuestStartEligibility.rejected("MIN_LEVEL_NOT_MET");
		}
		if (player.getLevel() > metadata.maxLevel()) {
			return QuestStartEligibility.rejected("MAX_LEVEL_EXCEEDED");
		}
		if (!metadata.permitsRace(player.getRace() == null ? null : player.getRace().name())) {
			return QuestStartEligibility.rejected("RACE_NOT_PERMITTED");
		}
		PlayerClass playerClass = player.getCommonData() == null ? null : player.getCommonData().getPlayerClass();
		if (!metadata.permittedClasses().isEmpty()
				&& (playerClass == null || !metadata.permittedClasses().contains(playerClass.name()))) {
			return QuestStartEligibility.rejected("CLASS_NOT_PERMITTED");
		}
		if (!metadata.permittedGender().isEmpty()
				&& (player.getGender() == null || !metadata.permittedGender().equals(player.getGender().name()))) {
			return QuestStartEligibility.rejected("GENDER_NOT_PERMITTED");
		}
		if (metadata.rank() != 0
				&& (player.getAbyssRank() == null || player.getAbyssRank().getRank().getId() < metadata.rank())) {
			return QuestStartEligibility.rejected("RANK_NOT_MET");
		}
		if (metadata.titleId() != 0
				&& (player.getTitleList() == null || !player.getTitleList().contains(metadata.titleId()))) {
			return QuestStartEligibility.rejected("TITLE_MISSING");
		}
		if (!hasCraftQualification(player, metadata)) {
			return QuestStartEligibility.rejected("CRAFT_QUALIFICATION_NOT_MET");
		}
		if (!matchesCanonicalStartConditions(player, metadata)) {
			return QuestStartEligibility.rejected("START_CONDITION_REJECTED");
		}
		if (!hasRequiredInventoryItems(player, metadata.inventoryItems())) {
			return QuestStartEligibility.rejected("REQUIRED_INVENTORY_ITEM_MISSING");
		}
		if (limitedQuestRequiresAcquisition.test(questId, metadata)) {
			return QuestStartEligibility.rejected("LIMITED_QUEST_ACQUISITION_UNSUPPORTED");
		}
		if (!isNoCount(metadata)
				&& normalQuestCount(player) + 1 > CustomConfig.BASIC_QUEST_SIZE_LIMIT) {
			return QuestStartEligibility.rejected("QUEST_LIST_FULL");
		}
		return QuestStartEligibility.allowed();
	}

	/** Checks canonical prerequisite and grouped start-condition semantics without changing quest state. */
	public boolean matchesCanonicalStartConditions(Player player, QuestMetadata metadata) {
		Objects.requireNonNull(player, "player");
		Objects.requireNonNull(metadata, "metadata");
		return prerequisitesMet(player, metadata) && startConditionGroupsMet(player, metadata);
	}

	private boolean prerequisitesMet(Player player, QuestMetadata metadata) {
		return metadata.prerequisites().stream().allMatch(questId -> {
			QuestState state = player.getQuestStateList().getQuestState(questId);
			return state != null && state.getStatus() == QuestStatus.COMPLETE;
		});
	}

	private int normalQuestCount(Player player) {
		return (int) player.getQuestStateList().getAllQuestState().stream()
			.filter(state -> state.getStatus() != QuestStatus.COMPLETE && state.getStatus() != QuestStatus.LOCKED
				&& state.getStatus() != QuestStatus.NONE)
			.filter(state -> {
				QuestMetadata metadata = metadataByQuest.apply(state.getQuestId());
				return metadata == null || "QUEST".equals(metadata.category());
			})
			.count();
	}

	private boolean startConditionGroupsMet(Player player, QuestMetadata metadata) {
		List<QuestStartConditionGroup> groups = metadata.startConditionGroups();
		return groups.isEmpty() || groups.stream().anyMatch(group -> group.conditions().stream()
			.allMatch(condition -> startConditionMet(player, condition)));
	}

	private boolean startConditionMet(Player player, QuestStartCondition condition) {
		QuestState state = player.getQuestStateList().getQuestState(condition.questId());
		return switch (condition.type().toLowerCase(java.util.Locale.ROOT)) {
			case "finished" -> state != null && state.getStatus() == QuestStatus.COMPLETE
				&& rewardMatches(condition, state) && repeatCompletionMatches(condition.questId(), state);
			case "unfinished" -> state == null || state.getStatus() != QuestStatus.COMPLETE;
			case "noacquired" -> state == null || (state.getStatus() != QuestStatus.START
				&& state.getStatus() != QuestStatus.REWARD && state.getStatus() != QuestStatus.COMPLETE);
			case "acquired" -> state != null && state.getStatus() != QuestStatus.NONE
				&& state.getStatus() != QuestStatus.LOCKED;
			case "equipped" -> player.getEquipment() != null
				&& player.getEquipment().getEquippedItemIds().contains(condition.questId());
			default -> false;
		};
	}

	private boolean repeatCompletionMatches(int questId, QuestState state) {
		QuestMetadata prerequisite = metadataByQuest.apply(questId);
		return prerequisite == null || prerequisite.repeatPolicy().maxRepeatCount() == 1
			|| prerequisite.repeatPolicy().maxRepeatCount() == 255
			|| state.getCompleteCount() == prerequisite.repeatPolicy().maxRepeatCount();
	}

	private static boolean rewardMatches(QuestStartCondition condition, QuestState state) {
		return condition.rewardMode() == state.getReward()
			|| condition.questId() == 2947 || condition.questId() == 1922;
	}

	private static boolean hasRequiredInventoryItems(Player player, List<QuestItemRequirement> required) {
		if (player.getInventory() == null) {
			return required.isEmpty();
		}
		return required.stream().allMatch(item ->
			player.getInventory().getItemCountByItemId(item.itemId()) >= item.count());
	}

	private static boolean hasCraftQualification(Player player, QuestMetadata metadata) {
		Integer skillId = metadata.combineSkill();
		if (skillId == null) {
			return true;
		}
		int required = metadata.combineSkillPoint() == null ? 0 : metadata.combineSkillPoint();
		if ((required == 499 && !CraftSkillUpdateService.canLearnMoreMasterCraftingSkill(player))
				|| (required == 399 && !CraftSkillUpdateService.canLearnMoreExpertCraftingSkill(player))) {
			return false;
		}
		if (player.getSkillList() == null) {
			return false;
		}
		List<Integer> skillIds = skillId == -1
			? List.of(30002, 30003, 40001, 40002, 40003, 40004, 40007, 40008, 40010)
			: List.of(skillId);
		return skillIds.stream().anyMatch(id -> {
			var skill = player.getSkillList().getSkillEntry(id);
			return skill != null && skill.getSkillLevel() >= required
				&& (!("TASK".equals(metadata.category())) || skill.getSkillLevel() - 40 <= required);
		});
	}

	private static boolean isTimeBased(QuestMetadata metadata) {
		return metadata.repeatPolicy().daily() || metadata.repeatPolicy().weekly()
			|| !metadata.repeatCycles().isEmpty();
	}

	private static boolean isNoCount(QuestMetadata metadata) {
		return "NON_COUNT".equals(metadata.category()) || "EVENT".equals(metadata.category());
	}
}
