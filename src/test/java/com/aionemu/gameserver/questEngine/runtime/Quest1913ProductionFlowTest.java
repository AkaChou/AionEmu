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
import static org.junit.jupiter.api.Assertions.assertFalse;
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
		// S2 规范形接取：QUEST_SELECT(31) 从 unaccepted 直发接取窗页 4（SELECT1 页梯与 1007 中转已退场）。
		// S2 canonical accept: QUEST_SELECT(31) pops the ask window page 4 straight from unaccepted.
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(4)), afterCommit);

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
		AtomicInteger packedVariables = new AtomicInteger();
		List<QuestMutationPlan> plans = new ArrayList<>();
		List<AfterCommitAction> afterCommit = new ArrayList<>();
		QuestProductionDispatcher dispatcher = dispatcher(definition, status, packedVariables, plans, afterCommit);

		// 传送前（started，var0=0）：S2 交付边锚在 started1，QUEST_SELECT(31) 零路由 ——
		// "必须先完成传送才可领奖"由交付边源节点承担，不再有 SELECT5 报告页与 1009 中转。
		// Before the transfer (started, var0=0): the S2 delivery edge is anchored at started1, so
		// QUEST_SELECT(31) has no route; the report page and the 1009 relay are gone.
		QuestEventRouter.DispatchResult premature = dispatch(dispatcher, REWARD_NPC_ID, 31);

		assertNotHandled(premature);
		assertEquals(QuestStatus.START, status.get());
		assertEquals(0, packedVariables.get());
		assertTrue(plans.isEmpty());
		assertTrue(afterCommit.isEmpty());

		// 传送后（started1，var0=1）：点 31 即翻 REWARD 并下发本档奖励窗（单档 → 窗 1 页 5）。
		// After the transfer (started1, var0=1): clicking 31 flips REWARD and shows the tiered
		// reward window (one group -> window 1, page 5).
		packedVariables.set(1);
		QuestEventRouter.DispatchResult reward = dispatch(dispatcher, REWARD_NPC_ID, 31);

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

	@Test
	void completeClientDialogSequencePreservesStateAndRewardTiming() throws Exception {
		CompiledQuestDefinition definition = definition();
		AtomicReference<QuestStatus> status = new AtomicReference<>(QuestStatus.NONE);
		AtomicInteger packedVariables = new AtomicInteger();
		List<QuestMutationPlan> plans = new ArrayList<>();
		List<AfterCommitAction> afterCommit = new ArrayList<>();
		QuestProductionDispatcher dispatcher = dispatcher(definition, status, packedVariables, plans, afterCommit);

		assertResponse(dispatch(dispatcher, START_NPC_ID, 31), afterCommit,
			new AfterCommitAction.ShowQuestDialog(4));
		assertEquals(QuestStatus.NONE, status.get());

		// S2 接取段退场：ASK_QUEST_ACCEPT(1007) 中转随 SELECT1 页梯消失，接取窗由 31 直发（页 4）。
		// S2 retired accept relay: ASK_QUEST_ACCEPT(1007) goes with the SELECT1 ladder; 31 pops the
		// ask window (page 4) itself.
		assertNotHandled(dispatch(dispatcher, START_NPC_ID, 1007));
		assertEquals(QuestStatus.NONE, status.get());
		assertTrue(afterCommit.isEmpty());

		assertResponse(dispatch(dispatcher, START_NPC_ID, 1003), afterCommit,
			new AfterCommitAction.ShowQuestDialog(1004));
		assertEquals(QuestStatus.NONE, status.get());

		assertResponse(dispatch(dispatcher, START_NPC_ID, 1002), afterCommit,
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(1003));
		assertEquals(QuestStatus.START, status.get());

		assertResponse(dispatch(dispatcher, TRANSPORT_NPC_ID, 31), afterCommit,
			new AfterCommitAction.ShowQuestDialog(1352));
		assertResponse(dispatch(dispatcher, TRANSPORT_NPC_ID, 10000), afterCommit,
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.TeleportPlayer(210030000, 1643f, 1500f, 120f, (byte) 0),
			new AfterCommitAction.CloseDialog());
		assertEquals(QuestStatus.START, status.get());
		assertEquals(1, packedVariables.get());

		// S2 交付段退场：SELECT_QUEST_REWARD(1009) 中转消失，交付边只认 QUEST_SELECT(31)。
		// S2 retired delivery relay: SELECT_QUEST_REWARD(1009) is gone; only QUEST_SELECT(31)
		// commits the hand-in.
		assertNotHandled(dispatch(dispatcher, REWARD_NPC_ID, 1009));
		assertEquals(QuestStatus.START, status.get());
		assertEquals(1, packedVariables.get());
		assertTrue(afterCommit.isEmpty());

		assertResponse(dispatch(dispatcher, REWARD_NPC_ID, 31), afterCommit,
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(5));
		assertEquals(QuestStatus.REWARD, status.get());

		assertResponse(dispatch(dispatcher, REWARD_NPC_ID, 8), afterCommit,
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(10));
		assertEquals(QuestStatus.COMPLETE, status.get());
		assertEquals(0, packedVariables.get());
		QuestMutationPlan completion = plans.getLast();
		assertEquals(List.of(
			new QuestAction.GrantReward("EXP", 0, 14046, com.aionemu.gameserver.questEngine.definition.QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("ITEM", 160001273, 5, com.aionemu.gameserver.questEngine.definition.QuestRewardAmountMode.EXACT),
			new QuestAction.CompleteQuest(0)), completion.requiredActions());
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
		return com.aionemu.gameserver.questEngine.definition.ProductionQuestDefinitions.definitionInOverlay(1913);
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
		assertNoFailure(result);
		assertTrue(result.handled(), result::toString);
	}

	/** 零路由断言：S2 退场的旧页链中转（1007/1009）不得再被任何 owner 接管。 /
	 * Zero-route assertion: the S2-retired page-chain relays (1007/1009) must not be handled. */
	private static void assertNotHandled(QuestEventRouter.DispatchResult result) {
		assertNoFailure(result);
		assertFalse(result.handled(), result::toString);
	}

	private static void assertNoFailure(QuestEventRouter.DispatchResult result) {
		result.owners().stream().map(QuestEventRouter.OwnerResult::failure)
			.filter(java.util.Objects::nonNull).findFirst().ifPresent(failure -> {
				throw failure;
			});
	}

	private static void assertResponse(QuestEventRouter.DispatchResult result,
			List<AfterCommitAction> actual, AfterCommitAction... expected) {
		assertHandled(result);
		assertEquals(List.of(expected), actual);
		actual.clear();
	}
}
