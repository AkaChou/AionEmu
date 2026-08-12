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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Quest1118ProductionFlowTest {
	private static final int PLAYER_ID = 7;
	private static final int QUEST_ID = 1118;
	private static final int OINTMENT_ITEM_ID = 182200224;
	private static final int NPC_OBJECT_ID = 900_007;

	@Test
	void acceptanceGivesTheOintmentAndFinalDeliveryConsumesIt() throws Exception {
		AtomicReference<QuestStatus> status = new AtomicReference<>(QuestStatus.NONE);
		AtomicInteger packedVariables = new AtomicInteger();
		Map<Integer, Integer> inventory = new HashMap<>();
		List<QuestMutationPlan> plans = new ArrayList<>();
		List<List<QuestAction>> appliedActions = new ArrayList<>();
		List<AfterCommitAction> afterCommit = new ArrayList<>();
		QuestProductionDispatcher dispatcher = dispatcher(
			definition(), status, packedVariables, inventory, plans, appliedActions, afterCommit);

		assertHandled(dispatch(dispatcher, 203059, 31));
		assertEquals(QuestStatus.NONE, status.get());
		assertTrue(inventory.isEmpty());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(1011)), afterCommit);

		afterCommit.clear();
		assertHandled(dispatch(dispatcher, 203059, 1002));
		assertEquals(QuestStatus.START, status.get());
		assertEquals(0, packedVariables.get());
		assertEquals(1, inventory.get(OINTMENT_ITEM_ID));
		assertEquals(List.of(new QuestAction.GiveItem(OINTMENT_ITEM_ID, 1)), appliedActions.getLast());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(1003)), afterCommit);

		afterCommit.clear();
		assertHandled(dispatch(dispatcher, 203070, 31));
		assertEquals(QuestStatus.START, status.get());
		assertEquals(0, packedVariables.get());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(1352)), afterCommit);

		afterCommit.clear();
		assertHandled(dispatch(dispatcher, 203070, 10000));
		assertEquals(QuestStatus.START, status.get());
		assertEquals(1, packedVariables.get());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.ShowQuestSelectionDialog(10)), afterCommit);

		afterCommit.clear();
		assertHandled(dispatch(dispatcher, 203079, 31));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(2375)), afterCommit);

		afterCommit.clear();
		assertHandled(dispatch(dispatcher, 203079, 1009));
		assertEquals(QuestStatus.REWARD, status.get());
		assertEquals(1, packedVariables.get());
		assertTrue(inventory.isEmpty());
		assertEquals(List.of(new QuestAction.RemoveItem(OINTMENT_ITEM_ID, 1)), appliedActions.getLast());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(5)), afterCommit);
		assertEquals(QuestStatus.REWARD, plans.getLast().nextStatus());
	}

	private static QuestProductionDispatcher dispatcher(CompiledQuestDefinition definition,
			AtomicReference<QuestStatus> status, AtomicInteger packedVariables, Map<Integer, Integer> inventory,
			List<QuestMutationPlan> plans, List<List<QuestAction>> appliedActions,
			List<AfterCommitAction> afterCommit) {
		QuestEventPort eventPort = (connection, playerId, questId, event) ->
			new QuestSnapshot(playerId, questId, status.get(), packedVariables.get(), Map.copyOf(inventory))
				.withStartEligibility(QuestStartEligibility.allowed());
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
			eventPort, inventoryActions(inventory, appliedActions), statePort,
			(action, snapshot, plan) -> afterCommit.add(action),
			Quest1118ProductionFlowTest::connection, ignored -> { },
			new QuestRuntimeMetricsCollector());
	}

	private static QuestActionPort inventoryActions(Map<Integer, Integer> inventory,
			List<List<QuestAction>> appliedActions) {
		return new QuestActionPort() {
			@Override
			public void preflight(Connection connection, QuestSnapshot snapshot, List<QuestAction> actions) {
			}

			@Override
			public QuestTransactionParticipant apply(Connection connection, QuestSnapshot snapshot,
					List<QuestAction> actions) {
				List<QuestAction> copy = List.copyOf(actions);
				appliedActions.add(copy);
				return QuestTransactionParticipant.of(() -> applyInventoryActions(inventory, copy), () -> { });
			}
		};
	}

	private static void applyInventoryActions(Map<Integer, Integer> inventory, List<QuestAction> actions) {
		for (QuestAction action : actions) {
			if (action instanceof QuestAction.GiveItem give) {
				inventory.merge(give.itemId(), give.count(), Integer::sum);
			} else if (action instanceof QuestAction.RemoveItem remove) {
				inventory.compute(remove.itemId(), (itemId, count) -> {
					int remaining = count - remove.count();
					return remaining == 0 ? null : remaining;
				});
			}
		}
	}

	private static QuestEventRouter.DispatchResult dispatch(QuestProductionDispatcher dispatcher,
			int npcId, int dialogId) {
		return dispatcher.dispatch(new QuestEvent.TalkToNpc(npcId, dialogId, NPC_OBJECT_ID),
			PLAYER_ID, QUEST_ID, QuestDispatchContract.EXCLUSIVE);
	}

	private static CompiledQuestDefinition definition() throws Exception {
		String resource = "/aion/data/static_data/quest_definition/quests/1118.xml";
		try (InputStream input = Quest1118ProductionFlowTest.class.getResourceAsStream(resource)) {
			if (input == null) {
				throw new IllegalStateException("missing resource " + resource);
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
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
