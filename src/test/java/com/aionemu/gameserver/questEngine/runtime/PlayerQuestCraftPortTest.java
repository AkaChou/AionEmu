package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.dao.PlayerRecipesDAO;
import com.aionemu.gameserver.dao.PlayerSkillListDAO;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.gameobjects.player.RecipeList;
import com.aionemu.gameserver.model.skill.PlayerSkillEntry;
import com.aionemu.gameserver.model.skill.PlayerSkillList;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestRecipeOwnership;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerQuestCraftPortTest {
	private static final int PLAYER_ID = 7;
	private static final int QUEST_ID = 5000;
	private static final int RECIPE = 155004001;
	private static final int AUTO_RECIPE = 155004099;
	private static final int SKILL = 40002;

	@Test
	void preflightValidatesAuthoritativeRecipeRequirements() throws Exception {
		Player player = player(1, Set.of());
		RecordingRecipesDao recipesDao = new RecordingRecipesDao();
		PlayerQuestCraftPort port = port(player, recipesDao, new RecordingSkillsDao(), new RecordingPublisher(), true);

		port.preflight(connection(), snapshot(1, Set.of()),
			List.of(new QuestAction.LearnRecipe(RECIPE, QuestRecipeOwnership.QUEST_OWNED)));
	}

	@Test
	void preflightFailsClosedForMissingSkillAndSlotLimit() throws Exception {
		Player player = player(0, Set.of());
		PlayerQuestCraftPort missingSkill = port(player, new RecordingRecipesDao(),
			new RecordingSkillsDao(), new RecordingPublisher(), true);
		assertThrows(SQLException.class, () -> missingSkill.preflight(connection(), snapshot(0, Set.of()),
			List.of(new QuestAction.LearnRecipe(RECIPE, QuestRecipeOwnership.QUEST_OWNED))));

		PlayerQuestCraftPort noSlot = port(player, new RecordingRecipesDao(),
			new RecordingSkillsDao(), new RecordingPublisher(), false);
		assertThrows(SQLException.class, () -> noSlot.preflight(connection(), snapshot(399, Set.of()),
			List.of(new QuestAction.GrantCraftSkill(SKILL, 400, true))));
	}

	@Test
	void applyUsesCallerTransactionAndPublishesOnlyAfterCommit() throws Exception {
		Player player = player(399, Set.of());
		RecordingRecipesDao recipesDao = new RecordingRecipesDao();
		RecordingSkillsDao skillsDao = new RecordingSkillsDao();
		RecordingPublisher publisher = new RecordingPublisher();
		PlayerQuestCraftPort port = port(player, recipesDao, skillsDao, publisher, true);
		Connection connection = connection();
		List<QuestAction> actions = List.of(
			new QuestAction.LearnRecipe(RECIPE, QuestRecipeOwnership.QUEST_OWNED),
			new QuestAction.GrantCraftSkill(SKILL, 400, true));

		port.preflight(connection, snapshot(399, Set.of()), actions);
		QuestTransactionParticipant participant = port.apply(connection, snapshot(399, Set.of()), actions);

		assertTrue(publisher.calls.isEmpty());
		assertEquals(Set.of(RECIPE, AUTO_RECIPE), recipesDao.added.stream().map(Call::id).collect(java.util.stream.Collectors.toSet()));
		assertTrue(recipesDao.added.stream().allMatch(call -> call.connection() == connection));
		assertEquals(List.of(new Call(connection, SKILL, 400)), skillsDao.stored);
		participant.afterCommit();
		assertEquals(1, publisher.calls.size());
		assertEquals(Set.of(RECIPE, AUTO_RECIPE), publisher.calls.get(0).learned());
		assertEquals(Map.of(SKILL, 400), publisher.calls.get(0).skills());
	}

	@Test
	void rollbackAndOfflinePlayerNeverPublishOrWrite() throws Exception {
		RecordingRecipesDao recipesDao = new RecordingRecipesDao();
		RecordingSkillsDao skillsDao = new RecordingSkillsDao();
		RecordingPublisher publisher = new RecordingPublisher();
		Player player = player(1, Set.of());
		PlayerQuestCraftPort port = port(player, recipesDao, skillsDao, publisher, true);
		QuestTransactionParticipant participant = port.apply(connection(), snapshot(1, Set.of()),
			List.of(new QuestAction.LearnRecipe(RECIPE, QuestRecipeOwnership.QUEST_OWNED)));
		participant.afterRollback();
		assertTrue(publisher.calls.isEmpty());

		PlayerQuestCraftPort offline = port(null, recipesDao, skillsDao, publisher, true);
		assertThrows(SQLException.class, () -> offline.apply(connection(), snapshot(1, Set.of()),
			List.of(new QuestAction.ForgetRecipe(RECIPE))));
		assertEquals(1, recipesDao.added.size());
		assertTrue(recipesDao.deleted.isEmpty());
	}

	@Test
	void learnForgetAndSkillGrantAreIdempotentConvergenceOperations() throws Exception {
		Player player = player(450, Set.of(RECIPE));
		RecordingRecipesDao recipesDao = new RecordingRecipesDao();
		RecordingSkillsDao skillsDao = new RecordingSkillsDao();
		RecordingPublisher publisher = new RecordingPublisher();
		PlayerQuestCraftPort port = port(player, recipesDao, skillsDao, publisher, true);
		Connection connection = connection();

		QuestTransactionParticipant participant = port.apply(connection, snapshot(450, Set.of(RECIPE)), List.of(
			new QuestAction.LearnRecipe(RECIPE, QuestRecipeOwnership.QUEST_OWNED),
			new QuestAction.GrantCraftSkill(SKILL, 400, false),
			new QuestAction.ForgetRecipe(RECIPE)));
		participant.afterCommit();

		assertEquals(List.of(new Call(connection, RECIPE, 0)), recipesDao.deleted);
		assertEquals(List.of(new Call(connection, SKILL, 450)), skillsDao.stored);
		assertEquals(Set.of(RECIPE), publisher.calls.get(0).forgotten());
		assertTrue(publisher.calls.get(0).learned().isEmpty());
	}

	private static PlayerQuestCraftPort port(Player player, RecordingRecipesDao recipesDao,
			RecordingSkillsDao skillsDao, RecordingPublisher publisher, boolean eligible) {
		return new PlayerQuestCraftPort(playerId -> player, recipesDao, skillsDao,
			recipeId -> new PlayerQuestCraftPort.RecipeSpec(recipeId, Race.ELYOS, SKILL, 1),
			(p, skillId, level) -> Set.of(AUTO_RECIPE),
			(p, skillId, level) -> eligible, publisher);
	}

	private static QuestSnapshot snapshot(int skillLevel, Set<Integer> recipes) {
		Map<Integer, Integer> skills = skillLevel > 0 ? Map.of(SKILL, skillLevel) : Map.of();
		return new QuestSnapshot(PLAYER_ID, QUEST_ID, QuestStatus.START, 0, Map.of())
			.withCraftFacts(new QuestCraftSnapshot(recipes, skills, 1600, 2, 1));
	}

	private static Player player(int skillLevel, Set<Integer> recipes) throws Exception {
		Player player = new ObjenesisStd().newInstance(Player.class);
		setField(AionObject.class, player, "objectId", PLAYER_ID);
		PlayerCommonData commonData = new ObjenesisStd().newInstance(PlayerCommonData.class);
		setField(PlayerCommonData.class, commonData, "race", Race.ELYOS);
		setField(Player.class, player, "playerCommonData", commonData);
		player.setRecipeList(new RecipeList(new java.util.HashSet<>(recipes)));
		List<PlayerSkillEntry> skills = skillLevel > 0 ? List.of(new PlayerSkillEntry(SKILL, false, false,
			skillLevel, 0, null, 0, false, PersistentState.UPDATED)) : List.of();
		player.setSkillList(new PlayerSkillList(skills));
		return player;
	}

	private static Connection connection() {
		return (Connection) Proxy.newProxyInstance(PlayerQuestCraftPortTest.class.getClassLoader(),
			new Class<?>[] { Connection.class }, (proxy, method, args) -> {
				if (method.getName().equals("isWrapperFor")) return false;
				if (method.getName().equals("unwrap")) return null;
				Class<?> type = method.getReturnType();
				if (type == boolean.class) return false;
				if (type == int.class) return 0;
				if (type == long.class) return 0L;
				return null;
			});
	}

	private static void setField(Class<?> owner, Object target, String name, Object value) throws Exception {
		Field field = owner.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	private record Call(Connection connection, int id, int value) {
	}

	private record PublishCall(Set<Integer> learned, Set<Integer> forgotten, Map<Integer, Integer> skills) {
	}

	private static final class RecordingPublisher implements PlayerQuestCraftPort.CraftPublisher {
		private final List<PublishCall> calls = new ArrayList<>();

		@Override
		public void publish(Player player, Set<Integer> learnedRecipes, Set<Integer> forgottenRecipes,
				Map<Integer, Integer> grantedSkills) {
			calls.add(new PublishCall(Set.copyOf(learnedRecipes), Set.copyOf(forgottenRecipes),
				Map.copyOf(grantedSkills)));
		}
	}

	private static final class RecordingRecipesDao extends PlayerRecipesDAO {
		private final List<Call> added = new ArrayList<>();
		private final List<Call> deleted = new ArrayList<>();

		@Override public RecipeList load(int playerId) { return new RecipeList(); }
		@Override public boolean addRecipe(int playerId, int recipeId) { return true; }
		@Override public boolean delRecipe(int playerId, int recipeId) { return true; }
		@Override public void addRecipeInTransaction(Connection connection, int playerId, int recipeId) {
			added.add(new Call(connection, recipeId, 0));
		}
		@Override public void delRecipeInTransaction(Connection connection, int playerId, int recipeId) {
			deleted.add(new Call(connection, recipeId, 0));
		}
		@Override public boolean supports(String databaseName, int majorVersion, int minorVersion) { return true; }
	}

	private static final class RecordingSkillsDao extends PlayerSkillListDAO {
		private final List<Call> stored = new ArrayList<>();

		@Override public PlayerSkillList loadSkillList(int playerId) { return new PlayerSkillList(); }
		@Override public boolean storeSkills(Player player) { return true; }
		@Override public void storeSkillInTransaction(Connection connection, int playerId, int skillId, int targetLevel) {
			stored.add(new Call(connection, skillId, targetLevel));
		}
		@Override public Timestamp getSkinSkillActiveDateById(int playerObjId, int skillId) { return null; }
		@Override public int getSkinExpireTime(int playerObjId, int skillId) { return 0; }
		@Override public boolean supports(String databaseName, int majorVersion, int minorVersion) { return true; }
	}
}
