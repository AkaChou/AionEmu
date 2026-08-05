package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.QuestTemplate;
import com.aionemu.gameserver.model.templates.quest.InventoryItem;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.services.LimitedQuestService;
import com.aionemu.gameserver.services.QuestService;

import java.sql.SQLException;
import java.util.Objects;
import java.util.function.IntFunction;
import java.util.function.Predicate;

/** Production start-eligibility projection backed by the current authoritative quest data. */
public final class PlayerQuestStartEligibilityPort implements QuestStartEligibilityPort {
	private final QuestPlayerPort players;
	private final IntFunction<QuestTemplate> templates;
	private final Predicate<QuestTemplate> limitedQuestRequiresAcquisition;
	private final Predicate<QuestEnv> startConditions;

	public PlayerQuestStartEligibilityPort(QuestPlayerPort players) {
		this(players,
			questId -> DataManager.QUEST_DATA == null ? null : DataManager.QUEST_DATA.getQuestById(questId),
			LimitedQuestService::requiresAcquisition,
			env -> QuestService.checkStartConditions(env, false));
	}

	PlayerQuestStartEligibilityPort(QuestPlayerPort players, IntFunction<QuestTemplate> templates,
			Predicate<QuestTemplate> limitedQuestRequiresAcquisition, Predicate<QuestEnv> startConditions) {
		this.players = Objects.requireNonNull(players, "players");
		this.templates = Objects.requireNonNull(templates, "templates");
		this.limitedQuestRequiresAcquisition = Objects.requireNonNull(limitedQuestRequiresAcquisition,
			"limitedQuestRequiresAcquisition");
		this.startConditions = Objects.requireNonNull(startConditions, "startConditions");
	}

	@Override
	public QuestStartEligibility snapshot(int playerId, int questId) throws SQLException {
		Player player = players.find(playerId);
		if (player == null) {
			throw new SQLException("player is unavailable: " + playerId);
		}
		QuestTemplate template = templates.apply(questId);
		if (template == null) {
			return QuestStartEligibility.rejected("QUEST_TEMPLATE_MISSING");
		}
		if (template.getNpcFactionId() != 0) {
			var factions = player.getNpcFactions();
			var faction = factions == null ? null : factions.getNpcFactionById(template.getNpcFactionId());
			if (faction == null || !faction.isActive() || faction.getQuestId() != questId) {
				return QuestStartEligibility.rejected("NPC_FACTION_QUEST_NOT_ACTIVE");
			}
			if (!template.isTimeBased() && !factions.canStartQuest(template)) {
				return QuestStartEligibility.rejected("NPC_FACTION_QUEST_COOLDOWN");
			}
		}
		if (limitedQuestRequiresAcquisition.test(template)) {
			return QuestStartEligibility.rejected("LIMITED_QUEST_ACQUISITION_UNSUPPORTED");
		}
		QuestEnv env = new QuestEnv(null, player, questId, 0);
		if (!startConditions.test(env)) {
			return QuestStartEligibility.rejected("START_CONDITION_REJECTED");
		}
		if (!hasRequiredInventoryItems(player, template)) {
			return QuestStartEligibility.rejected("REQUIRED_INVENTORY_ITEM_MISSING");
		}
		if (!template.isNoCount()
				&& player.getQuestStateList().getNormalQuestListSize() + 1 > CustomConfig.BASIC_QUEST_SIZE_LIMIT) {
			return QuestStartEligibility.rejected("QUEST_LIST_FULL");
		}
		return QuestStartEligibility.allowed();
	}

	private static boolean hasRequiredInventoryItems(Player player, QuestTemplate template) {
		if (template.getInventoryItems() == null) {
			return true;
		}
		for (InventoryItem item : template.getInventoryItems().getInventoryItem()) {
			if (player.getInventory().getFirstItemByItemId(item.getItemId()) == null) {
				return false;
			}
		}
		return true;
	}
}
