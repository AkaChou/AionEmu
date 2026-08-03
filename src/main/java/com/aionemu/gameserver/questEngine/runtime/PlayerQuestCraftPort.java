package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.dao.PlayerRecipesDAO;
import com.aionemu.gameserver.dao.PlayerSkillListDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.recipe.RecipeTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEARN_RECIPE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_RECIPE_DELETE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SKILL_LIST;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.services.craft.CraftSkillUpdateService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Production craft port: durable recipe/skill effects commit atomically with quest state. */
@Slf4j
public final class PlayerQuestCraftPort implements QuestCraftPort {
	private final QuestPlayerPort players;
	private final PlayerRecipesDAO recipesDao;
	private final PlayerSkillListDAO skillsDao;
	private final RecipeResolver recipes;
	private final AutoRecipeResolver autoRecipes;
	private final CraftEligibility eligibility;
	private final CraftPublisher publisher;

	public PlayerQuestCraftPort(QuestPlayerPort players, PlayerRecipesDAO recipesDao,
			PlayerSkillListDAO skillsDao) {
		this(players, recipesDao, skillsDao,
			recipeId -> recipeSpec(DataManager.RECIPE_DATA.getRecipeTemplateById(recipeId)),
			(player, skillId, targetLevel) -> DataManager.RECIPE_DATA
				.getAutolearnRecipes(player.getRace(), skillId, targetLevel).stream()
				.map(RecipeTemplate::getId).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)),
			PlayerQuestCraftPort::canGrant,
			PlayerQuestCraftPort::publish);
	}

	PlayerQuestCraftPort(QuestPlayerPort players, PlayerRecipesDAO recipesDao, PlayerSkillListDAO skillsDao,
			RecipeResolver recipes, AutoRecipeResolver autoRecipes, CraftEligibility eligibility,
			CraftPublisher publisher) {
		this.players = Objects.requireNonNull(players, "players");
		this.recipesDao = Objects.requireNonNull(recipesDao, "recipesDao");
		this.skillsDao = Objects.requireNonNull(skillsDao, "skillsDao");
		this.recipes = Objects.requireNonNull(recipes, "recipes");
		this.autoRecipes = Objects.requireNonNull(autoRecipes, "autoRecipes");
		this.eligibility = Objects.requireNonNull(eligibility, "eligibility");
		this.publisher = Objects.requireNonNull(publisher, "publisher");
	}

	@Override
	public void preflight(Connection connection, QuestSnapshot snapshot, List<QuestAction> actions)
			throws SQLException {
		Objects.requireNonNull(connection, "connection");
		Objects.requireNonNull(snapshot, "snapshot");
		Objects.requireNonNull(actions, "actions");
		if (actions.isEmpty()) {
			return;
		}
		Player player = requirePlayer(snapshot.playerId());
		if (!snapshot.craftFactsCaptured()) {
			throw new SQLException("craft facts are not captured for player " + snapshot.playerId());
		}
		Set<Integer> projectedRecipes = new HashSet<>(snapshot.craftFacts().knownRecipes());
		Map<Integer, Integer> projectedSkills = new HashMap<>(snapshot.craftFacts().craftingSkillLevels());
		for (QuestAction action : actions) {
			if (action instanceof QuestAction.LearnRecipe learn) {
				preflightRecipe(player, snapshot, projectedRecipes, projectedSkills, learn.recipeId());
				projectedRecipes.add(learn.recipeId());
			} else if (action instanceof QuestAction.ForgetRecipe forget) {
				projectedRecipes.remove(forget.recipeId());
			} else if (action instanceof QuestAction.GrantCraftSkill grant) {
				int current = projectedSkills.getOrDefault(grant.skillId(), 0);
				if (!player.getSkillList().isCraftSkill(grant.skillId())) {
					throw new SQLException("not a crafting skill: " + grant.skillId());
				}
				if (current < grant.targetLevel() && !eligibility.canGrant(player, grant.skillId(), grant.targetLevel())) {
					throw new SQLException("craft skill slot limit prevents " + grant.skillId() + " level "
						+ grant.targetLevel());
				}
				projectedSkills.put(grant.skillId(), Math.max(current, grant.targetLevel()));
				if (grant.autoLearnRecipes()) {
					projectedRecipes.addAll(autoRecipes.resolve(player, grant.skillId(), grant.targetLevel()));
				}
			}
		}
	}

	private void preflightRecipe(Player player, QuestSnapshot snapshot, Set<Integer> projectedRecipes,
			Map<Integer, Integer> projectedSkills, int recipeId) throws SQLException {
		if (projectedRecipes.contains(recipeId)) {
			return;
		}
		if (projectedRecipes.size() >= snapshot.craftFacts().maxRecipes()) {
			throw new SQLException("recipe limit reached for player " + snapshot.playerId());
		}
		RecipeSpec recipe = recipes.resolve(recipeId);
		if (recipe == null) {
			throw new SQLException("unknown recipe " + recipeId);
		}
		if (recipe.race() != Race.PC_ALL && recipe.race() != player.getRace()) {
			throw new SQLException("recipe race mismatch for " + recipeId);
		}
		if (projectedSkills.getOrDefault(recipe.skillId(), 0) < recipe.requiredSkillLevel()) {
			throw new SQLException("craft skill requirement not met for recipe " + recipeId);
		}
	}

	@Override
	public QuestTransactionParticipant apply(Connection connection, QuestSnapshot snapshot, List<QuestAction> actions)
			throws SQLException {
		Objects.requireNonNull(connection, "connection");
		Objects.requireNonNull(snapshot, "snapshot");
		Objects.requireNonNull(actions, "actions");
		if (actions.isEmpty()) {
			return QuestTransactionParticipant.none();
		}
		Player player = requirePlayer(snapshot.playerId());
		Set<Integer> originalRecipes = new HashSet<>(player.getRecipeList().getRecipeList());
		Set<Integer> targetRecipes = new HashSet<>(originalRecipes);
		Set<Integer> touchedRecipes = new LinkedHashSet<>();
		Map<Integer, Integer> targetSkills = new HashMap<>();
		for (QuestAction action : actions) {
			if (action instanceof QuestAction.LearnRecipe learn) {
				targetRecipes.add(learn.recipeId());
				touchedRecipes.add(learn.recipeId());
			} else if (action instanceof QuestAction.ForgetRecipe forget) {
				targetRecipes.remove(forget.recipeId());
				touchedRecipes.add(forget.recipeId());
			} else if (action instanceof QuestAction.GrantCraftSkill grant) {
				int current = player.getSkillList().isSkillPresent(grant.skillId())
					? player.getSkillList().getSkillLevel(grant.skillId()) : 0;
				targetSkills.merge(grant.skillId(), Math.max(current, grant.targetLevel()), Math::max);
				if (grant.autoLearnRecipes()) {
					Set<Integer> learned = autoRecipes.resolve(player, grant.skillId(), grant.targetLevel());
					targetRecipes.addAll(learned);
					touchedRecipes.addAll(learned);
				}
			}
		}
		for (int recipeId : touchedRecipes) {
			if (targetRecipes.contains(recipeId)) {
				recipesDao.addRecipeInTransaction(connection, snapshot.playerId(), recipeId);
			} else {
				recipesDao.delRecipeInTransaction(connection, snapshot.playerId(), recipeId);
			}
		}
		for (Map.Entry<Integer, Integer> skill : targetSkills.entrySet()) {
			skillsDao.storeSkillInTransaction(connection, snapshot.playerId(), skill.getKey(), skill.getValue());
		}
		Set<Integer> learned = new HashSet<>(targetRecipes);
		learned.removeAll(originalRecipes);
		Set<Integer> forgotten = new HashSet<>(originalRecipes);
		forgotten.removeAll(targetRecipes);
		return QuestTransactionParticipant.of(() -> {
			try {
				publisher.publish(player, learned, forgotten, targetSkills);
			} catch (RuntimeException failure) {
				// The database is already authoritative. Relog or an idempotent retry reconciles live state.
				log.error(I18n.get("log.quest_engine.craft_publish_failed", snapshot.playerId()), failure);
			}
		}, () -> { });
	}

	private Player requirePlayer(int playerId) throws SQLException {
		Player player = players.find(playerId);
		if (player == null || player.getRecipeList() == null || player.getSkillList() == null) {
			throw new SQLException("player craft state is unavailable: " + playerId);
		}
		return player;
	}

	private static boolean canGrant(Player player, int skillId, int targetLevel) {
		if (targetLevel == 400) {
			return CraftSkillUpdateService.canLearnMoreExpertCraftingSkill(player);
		}
		if (targetLevel == 500) {
			return CraftSkillUpdateService.canLearnMoreMasterCraftingSkill(player);
		}
		return true;
	}

	private static RecipeSpec recipeSpec(RecipeTemplate recipe) {
		return recipe == null ? null
			: new RecipeSpec(recipe.getId(), recipe.getRace(), recipe.getSkillid(), recipe.getSkillpoint());
	}

	private static void publish(Player player, Set<Integer> learned, Set<Integer> forgotten,
			Map<Integer, Integer> skills) {
		for (int recipeId : learned.stream().sorted().toList()) {
			if (player.getRecipeList().getRecipeList().add(recipeId)) {
				PacketSendUtility.sendPacket(player, new SM_LEARN_RECIPE(recipeId));
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CRAFT_RECIPE_LEARN(recipeId, player.getName()));
			}
		}
		for (int recipeId : forgotten.stream().sorted().toList()) {
			if (player.getRecipeList().getRecipeList().remove(recipeId)) {
				PacketSendUtility.sendPacket(player, new SM_RECIPE_DELETE(recipeId));
			}
		}
		for (Map.Entry<Integer, Integer> skill : skills.entrySet().stream()
				.sorted(Map.Entry.comparingByKey()).toList()) {
			int current = player.getSkillList().isSkillPresent(skill.getKey())
				? player.getSkillList().getSkillLevel(skill.getKey()) : 0;
			if (current < skill.getValue()) {
				player.getSkillList().addSkillWithoutSave(player, skill.getKey(), skill.getValue());
				player.getSkillList().getSkillEntry(skill.getKey()).setPersistentState(PersistentState.UPDATED);
				PacketSendUtility.sendPacket(player,
					new SM_SKILL_LIST(player.getSkillList().getSkillEntry(skill.getKey()), 1330064, false));
			}
		}
	}

	record RecipeSpec(int id, Race race, int skillId, int requiredSkillLevel) {
	}

	@FunctionalInterface
	interface RecipeResolver {
		RecipeSpec resolve(int recipeId);
	}

	@FunctionalInterface
	interface AutoRecipeResolver {
		Set<Integer> resolve(Player player, int skillId, int targetLevel);
	}

	@FunctionalInterface
	interface CraftEligibility {
		boolean canGrant(Player player, int skillId, int targetLevel);
	}

	@FunctionalInterface
	interface CraftPublisher {
		void publish(Player player, Set<Integer> learnedRecipes, Set<Integer> forgottenRecipes,
			Map<Integer, Integer> grantedSkills);
	}
}
