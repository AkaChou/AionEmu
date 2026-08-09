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
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_STATS_INFO;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionXmlCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.definition.QuestRewardAmountMode;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Production-chain proof for quest 1112 from packed a5b5 through completion protocol. */
class Quest1112ProductionFlowTest {
	private static final int PLAYER_ID = 7;
	private static final int QUEST_ID = 1112;
	private static final int NPC_ID = 203072;
	private static final int NPC_OBJECT_ID = 900_007;
	private static final int A5B5 = 5 + (5 << 6);
	private static final String DEFINITION =
		"/aion/data/static_data/quest_definition/quests/1112.xml";

	@BeforeAll
	static void configurePacketProcessor() {
		NetworkConfig.PACKET_PROCESSOR_MIN_THREADS = 1;
		NetworkConfig.PACKET_PROCESSOR_MAX_THREADS = 1;
		NetworkConfig.PACKET_PROCESSOR_THREAD_SPAWN_THRESHOLD = 1;
		NetworkConfig.PACKET_PROCESSOR_THREAD_KILL_THRESHOLD = 1;
	}

	@Test
	void reportDialogsRouteThroughTheAuthoritativeNpcObjectAndPublishRewardState() throws Exception {
		Fixture fixture = fixture(QuestStatus.START, A5B5);

		QuestEventRouter.DispatchResult view = fixture.dispatch(31);
		assertNoFailure(view);
		assertTrue(view.handled(), view::toString);
		assertEquals(QuestStatus.START, fixture.state().getStatus());
		assertDialog(fixture.lastPacket(), NPC_OBJECT_ID, 1352, QUEST_ID);

		fixture.clearPackets();
		QuestEventRouter.DispatchResult report = fixture.dispatch(1009);
		assertNoFailure(report);
		assertTrue(report.handled(), report::toString);
		assertEquals(QuestStatus.REWARD, fixture.state().getStatus());
		assertEquals(A5B5, fixture.state().getQuestVars().getQuestVars());
		assertEquals(List.of(SM_QUEST_ACTION.class, SM_DIALOG_WINDOW.class), fixture.packetTypes());
		assertQuestAction(fixture.packets().get(0), QuestStatus.REWARD, A5B5);
		assertDialog(fixture.packets().get(1), NPC_OBJECT_ID, 5, QUEST_ID);
		assertEquals(List.of(NPC_OBJECT_ID), fixture.resolvedInteractionObjects());

		fixture.clearPackets();
		QuestEventRouter.DispatchResult preview = fixture.dispatch(-1);
		assertNoFailure(preview);
		assertTrue(preview.handled(), preview::toString);
		assertEquals(QuestStatus.REWARD, fixture.state().getStatus());
		assertDialog(fixture.lastPacket(), NPC_OBJECT_ID, 5, QUEST_ID);
	}

	@Test
	void everyCompletionDialogCommitsRewardsPublishesStateThenSendsStatsCompletionAndSelection() throws Exception {
		List<Integer> completionDialogs = IntStream.rangeClosed(8, 23).boxed().toList();
		for (int dialogId : completionDialogs) {
			Fixture fixture = fixture(QuestStatus.REWARD, A5B5);

			QuestEventRouter.DispatchResult result = fixture.dispatch(dialogId);

			assertNoFailure(result);
			assertTrue(result.handled(), () -> "dialog " + dialogId + ": " + result);
			assertEquals(QuestStatus.COMPLETE, fixture.state().getStatus(), "dialog " + dialogId);
			assertEquals(0, fixture.state().getQuestVars().getQuestVars(), "dialog " + dialogId);
			assertEquals(1, fixture.state().getCompleteCount(), "dialog " + dialogId);
			assertEquals(0, fixture.state().getRewardOrNull(), "dialog " + dialogId);
			assertEquals(List.of(
				new QuestAction.GrantReward("GOLD", 0, 1810, QuestRewardAmountMode.QUEST_BASE)),
				fixture.currencyRewards());
			assertEquals(List.of(
				new QuestAction.GrantReward("EXP", 0, 1375, QuestRewardAmountMode.QUEST_BASE),
				new QuestAction.GrantReward("ITEM", 169300002, 30)), fixture.durableRewards());
			assertEquals(List.of(SM_STATS_INFO.class, SM_QUEST_ACTION.class, SM_DIALOG_WINDOW.class),
				fixture.packetTypes(), "dialog " + dialogId);
			assertQuestAction(fixture.packets().get(1), QuestStatus.COMPLETE, 0);
			assertDialog(fixture.packets().get(2), NPC_OBJECT_ID, 10, 0);
			assertOrdered(fixture.calls(), "currency.apply", "reward.apply", "state.persist", "jdbc.commit",
				"currency.afterCommit", "reward.afterCommit", "state.publish", "packet:SM_STATS_INFO",
				"packet:SM_QUEST_ACTION", "zone.refresh", "nearby.refresh", "level.refresh",
				"packet:SM_DIALOG_WINDOW");
			assertEquals(List.of(NPC_OBJECT_ID), fixture.resolvedInteractionObjects());
			assertTrue(fixture.auditEvents().isEmpty());
		}
	}

	@Test
	void oneAfterCommitFailureIsAuditedWithoutReplayingRewardsOrBlockingRemainingProtocol() throws Exception {
		Fixture fixture = fixture(QuestStatus.REWARD, A5B5, true);

		QuestEventRouter.DispatchResult result = fixture.dispatch(8);

		assertNoFailure(result);
		assertTrue(result.handled(), result::toString);
		assertEquals(QuestStatus.COMPLETE, fixture.state().getStatus());
		assertEquals(1, fixture.currency.applyCount);
		assertEquals(1, fixture.rewards.applyCount);
		assertEquals(List.of(SM_QUEST_ACTION.class, SM_DIALOG_WINDOW.class), fixture.packetTypes());
		assertQuestAction(fixture.packets().get(0), QuestStatus.COMPLETE, 0);
		assertDialog(fixture.packets().get(1), NPC_OBJECT_ID, 10, 0);
		assertOrdered(fixture.calls(), "jdbc.commit", "state.publish", "stats.fail",
			"packet:SM_QUEST_ACTION", "packet:SM_DIALOG_WINDOW");
		QuestAuditEvent audit = fixture.auditEvents().getFirst();
		assertEquals(QuestRouteResult.HANDLED, audit.result());
		assertEquals(QuestFailureStage.AFTER_COMMIT, audit.failureStage());
		assertTrue(audit.committed());
		assertEquals("reward", audit.sourceNode());
		assertEquals("complete", audit.targetNode());
		assertEquals(NPC_ID, audit.npcId());
		assertEquals(8, audit.dialogId());
		assertEquals("stats unavailable", audit.failure().getMessage());
	}

	private Fixture fixture(QuestStatus status, int packedVariables) throws Exception {
		return fixture(status, packedVariables, false);
	}

	private Fixture fixture(QuestStatus status, int packedVariables, boolean failStats) throws Exception {
		CompiledQuestDefinition definition = definition();
		List<String> calls = new ArrayList<>();
		List<Integer> resolvedInteractionObjects = new ArrayList<>();
		List<QuestAuditEvent> auditEvents = new ArrayList<>();
		Player player = player(status, packedVariables, calls);
		RecordingDao dao = new RecordingDao(calls);
		PlayerQuestStatePort stateDelegate = new PlayerQuestStatePort(playerId -> player, dao);
		QuestStatePort statePort = new QuestStatePort() {
			@Override
			public void apply(Connection connection, int playerId, QuestMutationPlan plan) throws java.sql.SQLException {
				stateDelegate.apply(connection, playerId, plan);
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
		RecordingInventoryPort inventory = new RecordingInventoryPort(calls);
		RecordingCurrencyPort currency = new RecordingCurrencyPort(calls);
		RecordingRewardPort rewards = new RecordingRewardPort(calls);
		QuestActionPort actionPort = new CompositeQuestActionPort(inventory, currency, rewards);
		QuestMetadata metadata = definition.definition().metadata();
		PlayerQuestStateSyncPort stateSync = new PlayerQuestStateSyncPort(playerId -> player,
			questId -> questId == QUEST_ID ? metadata : null,
			objectId -> {
				resolvedInteractionObjects.add(objectId);
				return null;
			},
			ignored -> calls.add("zone.refresh"),
			ignored -> calls.add("nearby.refresh"),
			ignored -> calls.add("level.refresh"));
		QuestStatsPort stats = failStats ? (snapshot, plan) -> {
			calls.add("stats.fail");
			throw new IllegalStateException("stats unavailable");
		} : new PlayerQuestStatsPort(playerId -> player);
		TypedQuestAfterCommitPort afterCommit = new TypedQuestAfterCommitPort(
			new PlayerQuestDialogPort(playerId -> player), null, null, null, null, null,
			stateSync, stats, null, null);
		QuestProductionDispatcher dispatcher = new QuestProductionDispatcher(
			new ImmutableQuestCatalog(List.of(definition)),
			new QuestExecutionCoordinator(new PlayerSerialExecutor()),
			new PlayerQuestEventPort(playerId -> player), actionPort, statePort, afterCommit,
			() -> transaction(calls), auditEvents::add, new QuestRuntimeMetricsCollector());
		return new Fixture(player, dispatcher, currency, rewards, calls,
			resolvedInteractionObjects, auditEvents);
	}

	private CompiledQuestDefinition definition() throws Exception {
		try (InputStream input = getClass().getResourceAsStream(DEFINITION)) {
			if (input == null) {
				throw new IllegalStateException("missing resource " + DEFINITION);
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}

	private static Player player(QuestStatus status, int packedVariables, List<String> calls) throws Exception {
		Player player = new ObjenesisStd().newInstance(Player.class);
		setField(AionObject.class, player, "objectId", PLAYER_ID);
		QuestStateList states = new QuestStateList();
		QuestState state = new QuestState(QUEST_ID, status, packedVariables, 0,
			(Timestamp) null, null, null);
		state.setPersistentState(PersistentState.UPDATED);
		states.addQuest(QUEST_ID, state);
		setField(Player.class, player, "questStateList", states);
		PlayerStorage inventory = new PlayerStorage(StorageType.CUBE);
		inventory.setOwner(player);
		setField(Player.class, player, "inventory", inventory);
		AionConnection connection = packetConnection(calls);
		setField(Player.class, player, "clientConnection", connection);
		return player;
	}

	private static AionConnection packetConnection(List<String> calls) throws Exception {
		AionConnection connection = new ObjenesisStd().newInstance(AionConnection.class);
		RecordingTransport transport = new RecordingTransport(calls);
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
				case "close" -> {
					calls.add("jdbc.close");
					yield null;
				}
				case "toString" -> "quest-1112-transaction";
				default -> defaultValue(method.getReturnType());
			});
	}

	private static void assertQuestAction(AionServerPacket packet, QuestStatus status, int packed) throws Exception {
		SM_QUEST_ACTION action = assertInstanceOf(SM_QUEST_ACTION.class, packet);
		assertEquals(2, intField(SM_QUEST_ACTION.class, action, "action"));
		assertEquals(QUEST_ID, intField(SM_QUEST_ACTION.class, action, "questId"));
		assertEquals(status.value(), intField(SM_QUEST_ACTION.class, action, "status"));
		assertEquals(packed, intField(SM_QUEST_ACTION.class, action, "step"));
	}

	private static void assertDialog(AionServerPacket packet, int objectId, int dialogId, int questId)
			throws Exception {
		SM_DIALOG_WINDOW dialog = assertInstanceOf(SM_DIALOG_WINDOW.class, packet);
		assertEquals(objectId, intField(SM_DIALOG_WINDOW.class, dialog, "targetObjectId"));
		assertEquals(dialogId, intField(SM_DIALOG_WINDOW.class, dialog, "dialogID"));
		assertEquals(questId, intField(SM_DIALOG_WINDOW.class, dialog, "questId"));
	}

	private static void assertOrdered(List<String> calls, String... expected) {
		int previous = -1;
		for (String call : expected) {
			int index = calls.indexOf(call);
			assertTrue(index > previous, () -> call + " is out of order in " + calls);
			previous = index;
		}
	}

	private static void assertNoFailure(QuestEventRouter.DispatchResult result) {
		result.owners().stream().map(QuestEventRouter.OwnerResult::failure)
			.filter(java.util.Objects::nonNull).findFirst().ifPresent(failure -> {
				throw failure;
			});
	}

	private static int intField(Class<?> declaringClass, Object target, String name) throws Exception {
		Field field = declaringClass.getDeclaredField(name);
		field.setAccessible(true);
		return field.getInt(target);
	}

	private static void setField(Class<?> declaringClass, Object target, String name, Object value)
			throws Exception {
		Field field = declaringClass.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
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

	private record Fixture(Player player, QuestProductionDispatcher dispatcher,
			RecordingCurrencyPort currency, RecordingRewardPort rewards, List<String> calls,
			List<Integer> resolvedInteractionObjects, List<QuestAuditEvent> auditEvents) {
		private QuestEventRouter.DispatchResult dispatch(int dialogId) {
			return dispatcher.dispatch(new QuestEvent.TalkToNpc(NPC_ID, dialogId, NPC_OBJECT_ID),
				PLAYER_ID, QUEST_ID, QuestDispatchContract.EXCLUSIVE);
		}

		private QuestState state() {
			return player.getQuestStateList().getQuestState(QUEST_ID);
		}

		private List<AionServerPacket> packets() {
			return packetQueue(player.getClientConnection());
		}

		private AionServerPacket lastPacket() {
			return packets().getLast();
		}

		private List<Class<?>> packetTypes() {
			List<Class<?>> types = new ArrayList<>();
			packets().forEach(packet -> types.add(packet.getClass()));
			return List.copyOf(types);
		}

		private void clearPackets() {
			packets().clear();
			resolvedInteractionObjects.clear();
		}

		private List<QuestAction.GrantReward> currencyRewards() {
			return currency.applied;
		}

		private List<QuestAction.GrantReward> durableRewards() {
			return rewards.applied;
		}
	}

	private static final class RecordingTransport implements ConnectionTransport {
		private final List<String> calls;
		private AionConnection connection;

		private RecordingTransport(List<String> calls) {
			this.calls = calls;
		}

		@Override
		public String getIP() {
			return "127.0.0.1";
		}

		@Override
		public void enableWriteInterest() {
			calls.add("packet:" + packetQueue(connection).getLast().getClass().getSimpleName());
		}

		@Override
		public void close(boolean forced) {
		}

		@Override
		public boolean onlyClose() {
			return true;
		}
	}

	private static final class RecordingDao extends PlayerQuestListDAO {
		private final List<String> calls;

		private RecordingDao(List<String> calls) {
			this.calls = calls;
		}

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
			calls.add("state.persist");
		}

		@Override
		public boolean supports(String databaseName, int majorVersion, int minorVersion) {
			return false;
		}
	}

	private static final class RecordingInventoryPort implements QuestInventoryPort {
		private final List<String> calls;

		private RecordingInventoryPort(List<String> calls) {
			this.calls = calls;
		}

		@Override
		public void preflight(Connection connection, QuestSnapshot snapshot,
				List<QuestAction.RemoveItem> removals, List<QuestAction.GiveItem> gives) {
			calls.add("inventory.preflight");
		}

		@Override
		public QuestTransactionParticipant apply(Connection connection, QuestSnapshot snapshot,
				List<QuestAction.RemoveItem> removals, List<QuestAction.GiveItem> gives) {
			calls.add("inventory.apply");
			return QuestTransactionParticipant.none();
		}
	}

	private static final class RecordingCurrencyPort implements QuestCurrencyPort {
		private final List<String> calls;
		private List<QuestAction.GrantReward> applied = List.of();
		private int applyCount;

		private RecordingCurrencyPort(List<String> calls) {
			this.calls = calls;
		}

		@Override
		public void preflight(Connection connection, QuestSnapshot snapshot,
				List<QuestAction.GrantReward> rewards) {
			calls.add("currency.preflight");
		}

		@Override
		public QuestTransactionParticipant apply(Connection connection, QuestSnapshot snapshot,
				List<QuestAction.GrantReward> rewards) {
			calls.add("currency.apply");
			applyCount++;
			applied = List.copyOf(rewards);
			return QuestTransactionParticipant.of(() -> calls.add("currency.afterCommit"),
				() -> calls.add("currency.rollback"));
		}
	}

	private static final class RecordingRewardPort implements QuestRewardPort {
		private final List<String> calls;
		private List<QuestAction.GrantReward> applied = List.of();
		private int applyCount;

		private RecordingRewardPort(List<String> calls) {
			this.calls = calls;
		}

		@Override
		public void preflight(Connection connection, QuestSnapshot snapshot,
				List<QuestAction.GrantReward> rewards) {
			calls.add("reward.preflight");
		}

		@Override
		public QuestTransactionParticipant apply(Connection connection, QuestSnapshot snapshot,
				List<QuestAction.GrantReward> rewards) {
			calls.add("reward.apply");
			applyCount++;
			applied = List.copyOf(rewards);
			return QuestTransactionParticipant.of(() -> calls.add("reward.afterCommit"),
				() -> calls.add("reward.rollback"));
		}
	}
}
