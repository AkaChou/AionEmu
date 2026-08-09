package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.commons.network.AConnection;
import com.aionemu.commons.network.ConnectionTransport;
import com.aionemu.gameserver.configs.network.NetworkConfig;
import com.aionemu.gameserver.dao.PlayerQuestListDAO;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.model.items.storage.PlayerStorage;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionXmlCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Quest80487ProductionFlowTest {
	private static final int PLAYER_ID = 7;
	private static final int QUEST_ID = 80487;

	@BeforeAll
	static void configurePacketProcessor() {
		NetworkConfig.PACKET_PROCESSOR_MIN_THREADS = 1;
		NetworkConfig.PACKET_PROCESSOR_MAX_THREADS = 1;
		NetworkConfig.PACKET_PROCESSOR_THREAD_SPAWN_THRESHOLD = 1;
		NetworkConfig.PACKET_PROCESSOR_THREAD_KILL_THRESHOLD = 1;
	}

	@Test
	void levelUpDoesNotStartNpcOnlyQuestOrOpenMissingClientPage() throws Exception {
		CompiledQuestDefinition definition = definition();
		List<QuestAuditEvent> auditEvents = new ArrayList<>();
		Player player = player();
		QuestProductionDispatcher dispatcher = dispatcher(definition, player, auditEvents);

		QuestEventRouter.DispatchResult result = dispatcher.dispatch(new QuestEvent.LevelUp(),
			PLAYER_ID, QUEST_ID, QuestDispatchContract.BROADCAST);

		assertNoFailure(result);
		assertFalse(result.handled(), result::toString);
		assertNull(player.getQuestStateList().getQuestState(QUEST_ID));
		assertTrue(packetQueue(player.getClientConnection()).isEmpty());
		assertTrue(auditEvents.isEmpty(), auditEvents::toString);
	}

	@Test
	void legitimateLevelUpQuestStillStartsWithTargetlessClientDialog() throws Exception {
		int questId = 1920;
		CompiledQuestDefinition definition = definition(questId);
		Player player = player();
		QuestProductionDispatcher dispatcher = dispatcher(definition, player, new ArrayList<>());

		QuestEventRouter.DispatchResult result = dispatcher.dispatch(new QuestEvent.LevelUp(),
			PLAYER_ID, questId, QuestDispatchContract.BROADCAST);

		assertNoFailure(result);
		assertTrue(result.handled(), result::toString);
		assertEquals(QuestStatus.START,
			player.getQuestStateList().getQuestState(questId).getStatus());
		List<SM_DIALOG_WINDOW> dialogs = packetQueue(player.getClientConnection()).stream()
			.filter(SM_DIALOG_WINDOW.class::isInstance).map(SM_DIALOG_WINDOW.class::cast).toList();
		assertEquals(1, dialogs.size());
		assertEquals(0, intField(SM_DIALOG_WINDOW.class, dialogs.getFirst(), "targetObjectId"));
		assertEquals(4, intField(SM_DIALOG_WINDOW.class, dialogs.getFirst(), "dialogID"));
		assertEquals(questId, intField(SM_DIALOG_WINDOW.class, dialogs.getFirst(), "questId"));
	}

	@Test
	void growthQuestFamilyRestoresLegacyNpcStartRoutes() throws Exception {
		for (int questId = 80487; questId <= 80538; questId++) {
			var transitions = definition(questId).definition().transitions();
			int npcId = questId <= 80512 ? 831031 : 831029;

			assertFalse(transitions.stream().anyMatch(
				transition -> transition.event() instanceof QuestEvent.LevelUp), "quest " + questId);
			assertTrue(transitions.stream().anyMatch(transition ->
				transition.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.npcId() == npcId
					&& Integer.valueOf(1002).equals(talk.dialogId())
					&& "unaccepted".equals(transition.sourceNode())
					&& "started".equals(transition.targetNode())), "quest " + questId);
		}
	}

	private CompiledQuestDefinition definition() throws Exception {
		return definition(QUEST_ID);
	}

	private CompiledQuestDefinition definition(int questId) throws Exception {
		String resource = "/aion/data/static_data/quest_definition/quests/" + questId + ".xml";
		try (InputStream input = getClass().getResourceAsStream(resource)) {
			if (input == null) {
				throw new IllegalStateException("missing resource " + resource);
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}

	private static QuestProductionDispatcher dispatcher(CompiledQuestDefinition definition, Player player,
			List<QuestAuditEvent> auditEvents) {
		int questId = definition.definition().id();
		PlayerQuestStatePort statePort = new PlayerQuestStatePort(playerId -> player, new RecordingDao());
		QuestMetadata metadata = definition.definition().metadata();
		PlayerQuestStateSyncPort stateSync = new PlayerQuestStateSyncPort(playerId -> player,
			id -> id == questId ? metadata : null, ignored -> null,
			ignored -> { }, ignored -> { }, ignored -> { });
		TypedQuestAfterCommitPort afterCommit = new TypedQuestAfterCommitPort(
			new PlayerQuestDialogPort(playerId -> player), null, null, null, null, null,
			stateSync, null, null, null);
		return new QuestProductionDispatcher(new ImmutableQuestCatalog(List.of(definition)),
			new QuestExecutionCoordinator(new PlayerSerialExecutor()),
			new PlayerQuestEventPort(playerId -> player,
				(playerId, id, event) -> QuestStartEligibility.allowed()),
			new NoOpActionPort(), statePort, afterCommit, Quest80487ProductionFlowTest::transaction,
			auditEvents::add, new QuestRuntimeMetricsCollector());
	}

	private static Player player() throws Exception {
		Player player = new ObjenesisStd().newInstance(Player.class);
		setField(AionObject.class, player, "objectId", PLAYER_ID);
		setField(Player.class, player, "questStateList", new QuestStateList());
		PlayerStorage inventory = new PlayerStorage(StorageType.CUBE);
		inventory.setOwner(player);
		setField(Player.class, player, "inventory", inventory);
		setField(Player.class, player, "clientConnection", packetConnection());
		return player;
	}

	private static AionConnection packetConnection() throws Exception {
		AionConnection connection = new ObjenesisStd().newInstance(AionConnection.class);
		RecordingTransport transport = new RecordingTransport();
		transport.connection = connection;
		setField(AConnection.class, connection, "transport", transport);
		setField(AConnection.class, connection, "guard", new Object());
		setField(AionConnection.class, connection, "sendMsgQueue", new ArrayList<AionServerPacket>());
		return connection;
	}

	private static Connection transaction() {
		return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
			new Class<?>[]{Connection.class}, (proxy, method, args) -> switch (method.getName()) {
				case "getAutoCommit" -> true;
				case "setAutoCommit", "commit", "rollback", "close" -> null;
				case "toString" -> "quest-80487-transaction";
				default -> defaultValue(method.getReturnType());
			});
	}

	private static void assertNoFailure(QuestEventRouter.DispatchResult result) {
		result.owners().stream().map(QuestEventRouter.OwnerResult::failure)
			.filter(java.util.Objects::nonNull).findFirst().ifPresent(failure -> {
				throw failure;
			});
	}

	@SuppressWarnings("unchecked")
	private static List<AionServerPacket> packetQueue(AionConnection connection) {
		try {
			Field field = AionConnection.class.getDeclaredField("sendMsgQueue");
			field.setAccessible(true);
			return (List<AionServerPacket>) field.get(connection);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static void setField(Class<?> declaringClass, Object target, String name, Object value)
			throws Exception {
		Field field = declaringClass.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static int intField(Class<?> declaringClass, Object target, String name) throws Exception {
		Field field = declaringClass.getDeclaredField(name);
		field.setAccessible(true);
		return field.getInt(target);
	}

	private static Object defaultValue(Class<?> type) {
		if (!type.isPrimitive()) return null;
		if (type == boolean.class) return false;
		if (type == byte.class) return (byte) 0;
		if (type == short.class) return (short) 0;
		if (type == int.class) return 0;
		if (type == long.class) return 0L;
		if (type == float.class) return 0F;
		if (type == double.class) return 0D;
		if (type == char.class) return '\0';
		return null;
	}

	private static final class NoOpActionPort implements QuestActionPort {
		@Override
		public void preflight(Connection connection, QuestSnapshot snapshot, List<QuestAction> actions) {
		}

		@Override
		public QuestTransactionParticipant apply(Connection connection, QuestSnapshot snapshot,
				List<QuestAction> actions) {
			return QuestTransactionParticipant.none();
		}
	}

	private static final class RecordingDao extends PlayerQuestListDAO {
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
		public void store(Connection connection, int playerId, Collection<QuestState> states) {
		}

		@Override
		public boolean supports(String databaseName, int majorVersion, int minorVersion) {
			return false;
		}
	}

	private static final class RecordingTransport implements ConnectionTransport {
		private AionConnection connection;

		@Override
		public String getIP() {
			return "127.0.0.1";
		}

		@Override
		public void enableWriteInterest() {
			packetQueue(connection).getLast();
		}

		@Override
		public void close(boolean forced) {
		}

		@Override
		public boolean onlyClose() {
			return true;
		}
	}
}
