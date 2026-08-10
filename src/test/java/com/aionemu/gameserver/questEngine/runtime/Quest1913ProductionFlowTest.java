package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionXmlCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Quest1913ProductionFlowTest {
	private static final int PLAYER_ID = 7;
	private static final int QUEST_ID = 1913;
	private static final int START_NPC_ID = 203758;
	private static final int TRANSPORT_NPC_ID = 203726;
	private static final int REWARD_NPC_ID = 203097;
	private static final int NPC_OBJECT_ID = 900_007;

	@Test
	void startDialogKeepsTheQuestUnacceptedAndAcceptDialogStartsIt() throws Exception {
		CompiledQuestDefinition definition = definition();
		AtomicReference<QuestStatus> status = new AtomicReference<>(QuestStatus.NONE);
		AtomicInteger packedVariables = new AtomicInteger();
		List<QuestMutationPlan> plans = new ArrayList<>();
		List<AfterCommitAction> afterCommit = new ArrayList<>();
		QuestProductionDispatcher dispatcher = dispatcher(definition, status, packedVariables, plans, afterCommit);

		QuestEventRouter.DispatchResult offer = dispatch(dispatcher, START_NPC_ID, 31);

		assertHandled(offer);
		assertEquals(QuestStatus.NONE, status.get());
		assertTrue(plans.isEmpty());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(1011)), afterCommit);

		plans.clear();
		afterCommit.clear();
		QuestEventRouter.DispatchResult accept = dispatch(dispatcher, START_NPC_ID, 1002);

		assertHandled(accept);
		assertEquals(QuestStatus.START, status.get());
		assertEquals(0, packedVariables.get());
		assertEquals(QuestStatus.START, plans.getLast().nextStatus());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(1003)), afterCommit);
	}

	@Test
	void stepToOneDialogAdvancesTheStartedQuestAndTeleportsToVerteron() throws Exception {
		CompiledQuestDefinition definition = definition();
		AtomicReference<QuestStatus> status = new AtomicReference<>(QuestStatus.START);
		AtomicInteger packedVariables = new AtomicInteger();
		List<QuestMutationPlan> plans = new ArrayList<>();
		List<AfterCommitAction> afterCommit = new ArrayList<>();
		QuestProductionDispatcher dispatcher = dispatcher(definition, status, packedVariables, plans, afterCommit);

		QuestEventRouter.DispatchResult offer = dispatch(dispatcher, TRANSPORT_NPC_ID, 31);

		assertHandled(offer);
		assertEquals(QuestStatus.START, status.get());
		assertEquals(0, packedVariables.get());
		assertTrue(plans.isEmpty());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(1352)), afterCommit);

		afterCommit.clear();
		QuestEventRouter.DispatchResult teleport = dispatch(dispatcher, TRANSPORT_NPC_ID, 10000);

		assertHandled(teleport);
		assertEquals(QuestStatus.START, status.get());
		assertEquals(1, packedVariables.get());
		assertEquals(QuestStatus.START, plans.getLast().nextStatus());
		assertEquals(1, plans.getLast().nextPackedVariables());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 1)), plans.getLast().requiredActions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.TeleportPlayer(210030000, 1643f, 1500f, 120f, (byte) 0),
			new AfterCommitAction.CloseDialog()), afterCommit);
	}

	@Test
	void rewardDialogIsAvailableOnlyAfterTheVerteronTransfer() throws Exception {
		CompiledQuestDefinition definition = definition();
		AtomicReference<QuestStatus> status = new AtomicReference<>(QuestStatus.START);
		AtomicInteger packedVariables = new AtomicInteger(1);
		List<QuestMutationPlan> plans = new ArrayList<>();
		List<AfterCommitAction> afterCommit = new ArrayList<>();
		QuestProductionDispatcher dispatcher = dispatcher(definition, status, packedVariables, plans, afterCommit);

		QuestEventRouter.DispatchResult offer = dispatch(dispatcher, REWARD_NPC_ID, 31);

		assertHandled(offer);
		assertEquals(QuestStatus.START, status.get());
		assertEquals(1, packedVariables.get());
		assertTrue(plans.isEmpty());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(2375)), afterCommit);

		afterCommit.clear();
		QuestEventRouter.DispatchResult reward = dispatch(dispatcher, REWARD_NPC_ID, 1009);

		assertHandled(reward);
		assertEquals(QuestStatus.REWARD, status.get());
		assertEquals(1, packedVariables.get());
		assertEquals(QuestStatus.REWARD, plans.getLast().nextStatus());
		assertEquals(1, plans.getLast().nextPackedVariables());
		assertTrue(plans.getLast().requiredActions().isEmpty());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(5)), afterCommit);
	}

	private static QuestProductionDispatcher dispatcher(CompiledQuestDefinition definition,
			AtomicReference<QuestStatus> status, AtomicInteger packedVariables, List<QuestMutationPlan> plans,
			List<AfterCommitAction> afterCommit) {
		QuestEventPort eventPort = (connection, playerId, questId, event) ->
			new QuestSnapshot(playerId, questId, status.get(), packedVariables.get(), Map.of())
				.withStartEligibility(QuestStartEligibility.allowed())
				.withCompletedQuestIds(Set.of(1007));
		QuestStatePort statePort = new QuestStatePort() {
			@Override
			public void apply(Connection connection, int playerId, QuestMutationPlan plan) {
				plans.add(plan);
				status.set(plan.nextStatus());
				packedVariables.set(plan.nextPackedVariables());
			}

			@Override
			public void publish(int playerId, QuestMutationPlan plan) {
			}
		};
		return new QuestProductionDispatcher(
			new ImmutableQuestCatalog(List.of(definition)),
			new QuestExecutionCoordinator(new PlayerSerialExecutor()),
			eventPort, noOpActions(), statePort,
			(action, snapshot, plan) -> afterCommit.add(action),
			Quest1913ProductionFlowTest::connection, ignored -> { },
			new QuestRuntimeMetricsCollector());
	}

	private static QuestEventRouter.DispatchResult dispatch(QuestProductionDispatcher dispatcher,
			int npcId, int dialogId) {
		return dispatcher.dispatch(new QuestEvent.TalkToNpc(npcId, dialogId, NPC_OBJECT_ID),
			PLAYER_ID, QUEST_ID, QuestDispatchContract.EXCLUSIVE);
	}

	private static CompiledQuestDefinition definition() throws Exception {
		String resource = "/aion/data/static_data/quest_definition/quests/1913.xml";
		try (InputStream input = Quest1913ProductionFlowTest.class.getResourceAsStream(resource)) {
			if (input == null) {
				throw new IllegalStateException("missing resource " + resource);
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}

	private static QuestActionPort noOpActions() {
		return new QuestActionPort() {
			@Override
			public void preflight(Connection connection, QuestSnapshot snapshot, List<QuestAction> actions) {
			}

			@Override
			public QuestTransactionParticipant apply(Connection connection, QuestSnapshot snapshot,
					List<QuestAction> actions) {
				return QuestTransactionParticipant.none();
			}
		};
	}

	private static Connection connection() {
		return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
			new Class<?>[]{Connection.class}, (proxy, method, args) -> switch (method.getName()) {
				case "getAutoCommit" -> true;
				case "setAutoCommit", "commit", "rollback", "close" -> null;
				default -> method.getReturnType() == boolean.class ? false : null;
			});
	}

	private static void assertHandled(QuestEventRouter.DispatchResult result) {
		result.owners().stream().map(QuestEventRouter.OwnerResult::failure)
			.filter(java.util.Objects::nonNull).findFirst().ifPresent(failure -> {
				throw failure;
			});
		assertTrue(result.handled(), result::toString);
	}
}
