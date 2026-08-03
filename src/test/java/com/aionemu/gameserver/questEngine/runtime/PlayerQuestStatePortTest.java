package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.dao.PlayerQuestListDAO;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlayerQuestStatePortTest {
	private static final int PLAYER_ID = 7;
	private static final int QUEST_ID = 1001;

	@Test
	void missingPlayerFailsBeforeAnyDaoWrite() throws Exception {
		RecordingDao dao = new RecordingDao();
		PlayerQuestStatePort port = new PlayerQuestStatePort(playerId -> null, dao);
		QuestMutationPlan plan = plan(QuestStatus.REWARD, 1);

		assertThrows(IllegalStateException.class, () -> port.apply(connection(), PLAYER_ID, plan));
		assertEquals(0, dao.stores.size());
	}

	@Test
	void applyWritesStagedProjectionWithoutAdvancingLiveState() throws Exception {
		Player player = playerWithState(QuestStatus.START, 0);
		// 模拟已持久化的现有任务状态(非 NEW),推进应走 UPDATE 而非重复 INSERT。
		player.getQuestStateList().getQuestState(QUEST_ID).setPersistentState(PersistentState.UPDATED);
		RecordingDao dao = new RecordingDao();
		PlayerQuestStatePort port = new PlayerQuestStatePort(playerId -> player, dao);

		port.apply(connection(), PLAYER_ID, plan(QuestStatus.REWARD, 1));

		// 暂存投影写入 DB，但不推进 live 状态。 The staged projection is persisted without advancing live state.
		assertEquals(1, dao.stores.size());
		QuestState stagedProjection = dao.stores.get(0).get(0);
		assertEquals(QuestStatus.REWARD, stagedProjection.getStatus());
		assertEquals(1, stagedProjection.getQuestVars().getQuestVars());
		assertEquals(PersistentState.UPDATE_REQUIRED, stagedProjection.getPersistentState());
		// live 内存未被触碰
		QuestState live = player.getQuestStateList().getQuestState(QUEST_ID);
		assertEquals(QuestStatus.START, live.getStatus());
		assertEquals(0, live.getQuestVars().getQuestVars());
	}

	@Test
	void publishUpdatesLiveStateOnlyAfterCommit() throws Exception {
		Player player = playerWithState(QuestStatus.START, 0);
		PlayerQuestStatePort port = new PlayerQuestStatePort(playerId -> player, new RecordingDao());
		QuestMutationPlan plan = plan(QuestStatus.REWARD, 1);

		port.apply(connection(), PLAYER_ID, plan);
		port.publish(PLAYER_ID, plan);

		QuestState live = player.getQuestStateList().getQuestState(QUEST_ID);
		assertEquals(QuestStatus.REWARD, live.getStatus());
		assertEquals(1, live.getQuestVars().getQuestVars());
		// 已持久化:下一次 store 不重复写
		assertEquals(PersistentState.UPDATED, live.getPersistentState());
	}

	@Test
	void publishSkipsPlayerLoggedOutAfterCommit() throws Exception {
		AtomicReference<Player> live = new AtomicReference<>(playerWithState(QuestStatus.START, 0));
		PlayerQuestStatePort port = new PlayerQuestStatePort(playerId -> live.get(), new RecordingDao());
		QuestMutationPlan plan = plan(QuestStatus.REWARD, 1);
		port.apply(connection(), PLAYER_ID, plan);
		live.set(null);
		// 不抛:提交已成功,玩家登出则内存无可发布对象,重登从 DB 恢复。
		port.publish(PLAYER_ID, plan);
	}

	@Test
	void newQuestStateStaysInsertableInStagedProjection() throws Exception {
		Player player = playerWithState(QuestStatus.NONE, 0);
		player.getQuestStateList().getQuestState(QUEST_ID).setPersistentState(PersistentState.NEW);
		RecordingDao dao = new RecordingDao();
		PlayerQuestStatePort port = new PlayerQuestStatePort(playerId -> player, dao);

		port.apply(connection(), PLAYER_ID, plan(QuestStatus.START, 0));

		QuestState stagedProjection = dao.stores.get(0).get(0);
		// NEW 保持 NEW,由 DAO 走 INSERT(而非 UPDATE),不产生重复/丢失。
		assertEquals(PersistentState.NEW, stagedProjection.getPersistentState());
	}

	@Test
	void noneToStartCreatesStateOnlyAfterCommitPublish() throws Exception {
		Player player = emptyPlayer();
		RecordingDao dao = new RecordingDao();
		PlayerQuestStatePort port = new PlayerQuestStatePort(playerId -> player, dao);
		QuestMutationPlan plan = plan(QuestStatus.START, 0);

		port.apply(connection(), PLAYER_ID, plan);

		assertEquals(null, player.getQuestStateList().getQuestState(QUEST_ID));
		assertEquals(PersistentState.NEW, dao.stores.get(0).get(0).getPersistentState());

		port.publish(PLAYER_ID, plan);
		QuestState live = player.getQuestStateList().getQuestState(QUEST_ID);
		assertEquals(QuestStatus.START, live.getStatus());
		assertEquals(PersistentState.UPDATED, live.getPersistentState());
	}

	@Test
	void completeQuestPersistsAndPublishesOneIdenticalLifecycleProjection() throws Exception {
		Player player = playerWithState(QuestStatus.REWARD, 1);
		QuestState before = player.getQuestStateList().getQuestState(QUEST_ID);
		before.setCompleteCount(2);
		before.setPersistentState(PersistentState.UPDATED);
		RecordingDao dao = new RecordingDao();
		PlayerQuestStatePort port = new PlayerQuestStatePort(playerId -> player, dao);
		QuestMutationPlan plan = new QuestMutationPlan(QUEST_ID, QuestStatus.COMPLETE, 0,
			List.of(new com.aionemu.gameserver.questEngine.definition.QuestAction.CompleteQuest(3)), List.of());

		port.apply(connection(), PLAYER_ID, plan);

		QuestState stagedProjection = dao.stores.get(0).get(0);
		assertEquals(QuestStatus.COMPLETE, stagedProjection.getStatus());
		assertEquals(0, stagedProjection.getQuestVars().getQuestVars());
		assertEquals(3, stagedProjection.getCompleteCount());
		assertEquals(3, stagedProjection.getRewardOrNull());
		assertEquals(QuestStatus.REWARD, before.getStatus());
		Timestamp persistedCompleteTime = stagedProjection.getCompleteTime();

		port.publish(PLAYER_ID, plan);

		QuestState committed = player.getQuestStateList().getQuestState(QUEST_ID);
		assertEquals(QuestStatus.COMPLETE, committed.getStatus());
		assertEquals(3, committed.getCompleteCount());
		assertEquals(3, committed.getRewardOrNull());
		assertEquals(persistedCompleteTime, committed.getCompleteTime());
	}

	@Test
	void rollbackDiscardsPreparedProjection() throws Exception {
		Player player = playerWithState(QuestStatus.START, 0);
		PlayerQuestStatePort port = new PlayerQuestStatePort(playerId -> player, new RecordingDao());
		QuestMutationPlan plan = plan(QuestStatus.REWARD, 1);
		port.apply(connection(), PLAYER_ID, plan);

		port.rollback(PLAYER_ID, plan);

		assertThrows(IllegalStateException.class, () -> port.publish(PLAYER_ID, plan));
		assertEquals(QuestStatus.START, player.getQuestStateList().getQuestState(QUEST_ID).getStatus());
	}

	private static QuestMutationPlan plan(QuestStatus status, int packed) {
		return new QuestMutationPlan(QUEST_ID, status, packed, List.of(), List.of());
	}

	private static Player playerWithState(QuestStatus status, int packedVariables) throws Exception {
		Player player = emptyPlayer();
		QuestStateList states = player.getQuestStateList();
		states.addQuest(QUEST_ID, new QuestState(QUEST_ID, status, packedVariables, 0,
				(Timestamp) null, null, null));
		return player;
	}

	private static Player emptyPlayer() throws Exception {
		Player player = new ObjenesisStd().newInstance(Player.class);
		setField(AionObject.class, player, "objectId", PLAYER_ID);
		setField(Player.class, player, "questStateList", new QuestStateList());
		return player;
	}

	private static void setField(Class<?> declaringClass, Object target, String name, Object value) throws Exception {
		Field field = declaringClass.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static Connection connection() {
		return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
				new Class<?>[]{Connection.class}, (proxy, method, args) -> switch (method.getName()) {
					case "getAutoCommit" -> false;
					case "toString" -> "test-connection";
					default -> defaultValue(method.getReturnType());
				});
	}

	private static Object defaultValue(Class<?> type) {
		if (!type.isPrimitive()) {
			return null;
		}
		if (type == boolean.class) {
			return false;
		}
		if (type == int.class || type == short.class || type == byte.class) {
			return 0;
		}
		if (type == long.class) {
			return 0L;
		}
		if (type == float.class) {
			return 0F;
		}
		if (type == double.class) {
			return 0D;
		}
		if (type == char.class) {
			return '\0';
		}
		return null;
	}

	private static final class RecordingDao extends PlayerQuestListDAO {
		private final List<List<QuestState>> stores = new ArrayList<>();

		@Override
		public QuestStateList load(Player player) {
			return new QuestStateList();
		}

		@Override
		public void store(Player player) {
			throw new AssertionError("unexpected player-based store");
		}

		@Override
		public void store(Connection connection, Player player) {
			throw new AssertionError("unexpected player-based store");
		}

		@Override
		public void store(Connection connection, int playerId, java.util.Collection<QuestState> states) {
			stores.add(List.copyOf(states));
		}

		@Override
		public boolean supports(String databaseName, int majorVersion, int minorVersion) {
			return false;
		}
	}
}
