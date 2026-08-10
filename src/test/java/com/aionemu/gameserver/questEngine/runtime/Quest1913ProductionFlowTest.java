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
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Quest1913ProductionFlowTest {
	private static final int PLAYER_ID = 7;
	private static final int QUEST_ID = 1913;
	private static final int NPC_ID = 203758;
	private static final int NPC_OBJECT_ID = 900_007;

	@Test
	void startDialogKeepsTheQuestUnacceptedAndAcceptDialogStartsIt() throws Exception {
		CompiledQuestDefinition definition = definition();
		AtomicReference<QuestStatus> status = new AtomicReference<>(QuestStatus.NONE);
		List<QuestMutationPlan> plans = new ArrayList<>();
		List<AfterCommitAction> afterCommit = new ArrayList<>();
		QuestProductionDispatcher dispatcher = dispatcher(definition, status, plans, afterCommit);

		QuestEventRouter.DispatchResult offer = dispatch(dispatcher, 31);

		assertHandled(offer);
		assertEquals(QuestStatus.NONE, status.get());
		assertTrue(plans.isEmpty());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(1011)), afterCommit);

		plans.clear();
		afterCommit.clear();
		QuestEventRouter.DispatchResult accept = dispatch(dispatcher, 1002);

		assertHandled(accept);
		assertEquals(QuestStatus.START, status.get());
		assertEquals(QuestStatus.START, plans.getLast().nextStatus());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(1003)), afterCommit);
	}

	private static QuestProductionDispatcher dispatcher(CompiledQuestDefinition definition,
			AtomicReference<QuestStatus> status, List<QuestMutationPlan> plans,
			List<AfterCommitAction> afterCommit) {
		QuestEventPort eventPort = (connection, playerId, questId, event) ->
			new QuestSnapshot(playerId, questId, status.get(), 0, Map.of())
				.withStartEligibility(QuestStartEligibility.allowed())
				.withCompletedQuestIds(Set.of(1007));
		QuestStatePort statePort = new QuestStatePort() {
			@Override
			public void apply(Connection connection, int playerId, QuestMutationPlan plan) {
				plans.add(plan);
				status.set(plan.nextStatus());
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

	private static QuestEventRouter.DispatchResult dispatch(QuestProductionDispatcher dispatcher, int dialogId) {
		return dispatcher.dispatch(new QuestEvent.TalkToNpc(NPC_ID, dialogId, NPC_OBJECT_ID),
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
