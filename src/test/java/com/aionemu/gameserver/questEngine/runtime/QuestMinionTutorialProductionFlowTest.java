package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.commons.network.AConnection;
import com.aionemu.commons.network.ConnectionTransport;
import com.aionemu.gameserver.configs.network.NetworkConfig;
import com.aionemu.gameserver.dao.PlayerQuestListDAO;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.model.items.storage.PlayerStorage;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies the real XML -> event port -> transaction -> state/packet chain for tutorial contracts. */
class QuestMinionTutorialProductionFlowTest {
	private static final int PLAYER_ID = 7;

	@BeforeAll
	static void configurePacketProcessor() {
		NetworkConfig.PACKET_PROCESSOR_MIN_THREADS = 1;
		NetworkConfig.PACKET_PROCESSOR_MAX_THREADS = 1;
		NetworkConfig.PACKET_PROCESSOR_THREAD_SPAWN_THRESHOLD = 1;
		NetworkConfig.PACKET_PROCESSOR_THREAD_KILL_THRESHOLD = 1;
	}

	@Test
	void successfulContractUsePublishesRewardStepOneAndClientPacket() throws Exception {
		for (Tutorial tutorial : Tutorial.values()) {
			Fixture fixture = fixture(tutorial, QuestStatus.START, 0);

			QuestEventRouter.DispatchResult result = fixture.dispatch(
				new QuestEvent.ItemPlay(tutorial.itemId, 1500), QuestDispatchContract.FIRST_NON_UNKNOWN);

			assertNoFailure(result);
			assertTrue(result.handled(), () -> tutorial + ": " + result);
			assertEquals(QuestStatus.REWARD, fixture.state().getStatus(), tutorial.toString());
			assertEquals(1, fixture.state().getQuestVars().getQuestVars(), tutorial.toString());
			assertEquals(1, fixture.persistedState().getQuestVars().getQuestVars(), tutorial.toString());
			assertQuestAction(fixture.packets().getLast(), tutorial.questId, QuestStatus.REWARD, 1);
			assertTrue(fixture.calls().indexOf("state.persist") < fixture.calls().indexOf("jdbc.commit"));
			assertTrue(fixture.calls().indexOf("jdbc.commit") < fixture.calls().indexOf("state.publish"));
		}
	}

	@Test
	void legacyRewardStepZeroIsMigratedOnLoginWithoutTheConsumedContract() throws Exception {
		for (Tutorial tutorial : Tutorial.values()) {
			Fixture fixture = fixture(tutorial, QuestStatus.REWARD, 0);

			QuestEventRouter.DispatchResult result = fixture.dispatch(
				new QuestEvent.EnterWorld(), QuestDispatchContract.BROADCAST);

			assertNoFailure(result);
			assertTrue(result.handled(), () -> tutorial + ": " + result);
			assertEquals(QuestStatus.REWARD, fixture.state().getStatus(), tutorial.toString());
			assertEquals(1, fixture.state().getQuestVars().getQuestVars(), tutorial.toString());
			assertQuestAction(fixture.packets().getLast(), tutorial.questId, QuestStatus.REWARD, 1);
		}
	}

	private static Fixture fixture(Tutorial tutorial, QuestStatus status, int packedVariables) throws Exception {
		CompiledQuestDefinition definition = definition(tutorial.questId);
		List<String> calls = new ArrayList<>();
		Player player = player(tutorial.questId, status, packedVariables);
		RecordingDao dao = new RecordingDao();
		QuestMetadata metadata = definition.definition().metadata();
		PlayerQuestStatePort stateDelegate = new PlayerQuestStatePort(playerId -> player, dao,
			questId -> questId == tutorial.questId ? metadata : null);
		QuestStatePort statePort = new QuestStatePort() {
			@Override
			public void apply(Connection connection, int playerId, QuestMutationPlan plan) throws java.sql.SQLException {
				stateDelegate.apply(connection, playerId, plan);
				calls.add("state.persist");
			}

			@Override
			public void publish(int playerId, QuestMutationPlan plan) {
				stateDelegate.publish(playerId, plan);
				calls.add("state.publish");
			}

			@Override
			public void rollback(int playerId, QuestMutationPlan plan) {
				stateDelegate.rollback(playerId, plan);
			}
		};
		PlayerQuestStateSyncPort stateSync = new PlayerQuestStateSyncPort(playerId -> player,
			questId -> questId == tutorial.questId ? metadata : null,
			ignored -> null, ignored -> calls.add("zone.refresh"),
			ignored -> calls.add("nearby.refresh"), ignored -> calls.add("level.refresh"));
		TypedQuestAfterCommitPort afterCommit = new TypedQuestAfterCommitPort(
			new PlayerQuestDialogPort(playerId -> player), null, null, null, null, null,
			stateSync, null, null, null);
		QuestProductionDispatcher dispatcher = new QuestProductionDispatcher(
			new ImmutableQuestCatalog(List.of(definition)),
			new QuestExecutionCoordinator(new PlayerSerialExecutor()),
			new PlayerQuestEventPort(playerId -> player), new NoOpActionPort(), statePort,
			afterCommit, () -> transaction(calls), ignored -> { }, new QuestRuntimeMetricsCollector());
		return new Fixture(player, dispatcher, dao, calls);
	}

	private static CompiledQuestDefinition definition(int questId) throws Exception {
		String resource = "/aion/data/static_data/quest_definition/quests/" + questId + ".xml";
		try (InputStream input = QuestMinionTutorialProductionFlowTest.class.getResourceAsStream(resource)) {
			if (input == null) {
				throw new IllegalStateException("missing resource " + resource);
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}

	private static Player player(int questId, QuestStatus status, int packedVariables) throws Exception {
		Player player = new ObjenesisStd().newInstance(Player.class);
		setField(AionObject.class, player, "objectId", PLAYER_ID);
		QuestStateList states = new QuestStateList();
		QuestState state = new QuestState(questId, status, packedVariables, 0, null, null, null);
		state.setPersistentState(PersistentState.UPDATED);
		states.addQuest(questId, state);
		setField(Player.class, player, "questStateList", states);
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

	private static Connection transaction(List<String> calls) {
		return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
			new Class<?>[]{Connection.class}, (proxy, method, args) -> switch (method.getName()) {
				case "getAutoCommit" -> true;
				case "setAutoCommit" -> {
					calls.add("jdbc.autoCommit:" + args[0]);
					yield null;
				}
				case "commit" -> {
					calls.add("jdbc.commit");
					yield null;
				}
				case "rollback" -> {
					calls.add("jdbc.rollback");
					yield null;
				}
				case "close" -> null;
				case "toString" -> "minion-tutorial-transaction";
				default -> defaultValue(method.getReturnType());
			});
	}

	private static void assertQuestAction(AionServerPacket packet, int questId,
		QuestStatus status, int packed) throws Exception {
		SM_QUEST_ACTION action = assertInstanceOf(SM_QUEST_ACTION.class, packet);
		assertEquals(2, intField(SM_QUEST_ACTION.class, action, "action"));
		assertEquals(questId, intField(SM_QUEST_ACTION.class, action, "questId"));
		assertEquals(status.value(), intField(SM_QUEST_ACTION.class, action, "status"));
		assertEquals(packed, intField(SM_QUEST_ACTION.class, action, "step"));
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

	private enum Tutorial {
		ELYOS(19900, 190080020),
		ASMODIANS(29900, 190080021);

		private final int questId;
		private final int itemId;

		Tutorial(int questId, int itemId) {
			this.questId = questId;
			this.itemId = itemId;
		}
	}

	private record Fixture(Player player, QuestProductionDispatcher dispatcher,
		RecordingDao dao, List<String> calls) {
		private QuestEventRouter.DispatchResult dispatch(QuestEvent event, QuestDispatchContract contract) {
			return dispatcher.dispatch(event, PLAYER_ID, 0, contract);
		}

		private QuestState state() {
			return player.getQuestStateList().getQuestState(dao.questId);
		}

		private QuestState persistedState() {
			return dao.stored.getLast();
		}

		private List<AionServerPacket> packets() {
			return packetQueue(player.getClientConnection());
		}
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
		private int questId;
		private final List<QuestState> stored = new ArrayList<>();

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
			stored.addAll(states);
			if (!stored.isEmpty()) {
				questId = stored.getLast().getQuestId();
			}
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
