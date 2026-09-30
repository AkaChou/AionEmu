package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.templates.QuestTemplate;
import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog;
import com.aionemu.gameserver.questEngine.definition.ProductionQuestDefinitions;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定哈拉梅尔后续任务（18504 消灭全部怪物、18505 巡视头目）的前置契约与接取执行流。
 * Locks prerequisite contracts and accept/delivery execution flows for Haramel subsequent quests (18504 and 18505).
 */
class QuestHaramelSubsequentQuestsProductionFlowTest {
	private static final int PLAYER_ID = 7;
	private static final int START_NPC_ID = 203166;
	private static final int TURN_IN_NPC_ID_18505 = 203106;
	private static final int PREREQUISITE_QUEST_ID = 18507;
	private static final int NPC_OBJECT_ID = 900_007;

	@Test
	void metadataAndTemplateEnforcePrerequisite18507() throws Exception {
		for (int questId : List.of(18504, 18505)) {
			CompiledQuestDefinition compiled = ProductionQuestDefinitions.definitionInOverlay(questId);
			var metadata = compiled.definition().metadata();
			assertEquals(Set.of(PREREQUISITE_QUEST_ID), metadata.prerequisites(),
				() -> "quest " + questId + " must have prerequisite " + PREREQUISITE_QUEST_ID);
			assertEquals(16, metadata.minLevel());
			assertEquals(Set.of("ELYOS"), metadata.permittedRaces());

			QuestTemplate template = QuestTemplate.fromMetadata(questId, metadata);
			assertNotNull(template);
			assertNotNull(template.getXMLStartConditions(), "startConditions must be populated");
			assertFalse(template.getXMLStartConditions().isEmpty(), "startConditions must not be empty");
			var finishedList = template.getXMLStartConditions().getFirst().getFinishedPreconditions();
			assertNotNull(finishedList);
			assertTrue(finishedList.stream().anyMatch(f -> f.getQuestId() == PREREQUISITE_QUEST_ID),
				() -> "QuestTemplate start conditions must contain prerequisite " + PREREQUISITE_QUEST_ID);
		}
	}

	@Test
	void uncompletedPrerequisiteRejectsAcceptance() throws Exception {
		for (int questId : List.of(18504, 18505)) {
			AtomicReference<QuestStatus> status = new AtomicReference<>(QuestStatus.NONE);
			AtomicInteger packedVariables = new AtomicInteger();
			List<QuestMutationPlan> plans = new ArrayList<>();
			List<AfterCommitAction> afterCommit = new ArrayList<>();

			// 快照未完成 18507: 且 startEligibility 判定拒绝
			QuestProductionDispatcher dispatcher = dispatcher(
				ProductionQuestDefinitions.definitionInOverlay(questId), questId, status, packedVariables, plans,
				afterCommit, Set.of(), QuestStartEligibility.rejected("PREREQUISITES_NOT_MET"), Map.of());

			QuestEventRouter.DispatchResult accept = dispatcher.dispatch(
				new QuestEvent.TalkToNpc(START_NPC_ID, QuestDialogAction.QUEST_ACCEPT_1.id(), NPC_OBJECT_ID),
				PLAYER_ID, questId, QuestDispatchContract.EXCLUSIVE);

			assertFalse(accept.handled(), () -> "quest " + questId + " must reject accept when 18507 is uncompleted");
			assertEquals(QuestStatus.NONE, status.get());
			assertTrue(plans.isEmpty());
		}
	}

	@Test
	void completedPrerequisiteAllowsAcceptanceAndTransitionsToStarted() throws Exception {
		// 18504 真端规范接取流: 31 -> 4 (SHOW_ASK_QUEST_ACCEPT_WINDOW) -> 1002 (QUEST_ACCEPT_1) -> 1003
		{
			int questId = 18504;
			AtomicReference<QuestStatus> status = new AtomicReference<>(QuestStatus.NONE);
			AtomicInteger packedVariables = new AtomicInteger();
			List<QuestMutationPlan> plans = new ArrayList<>();
			List<AfterCommitAction> afterCommit = new ArrayList<>();

			QuestProductionDispatcher dispatcher = dispatcher(
				ProductionQuestDefinitions.definitionInOverlay(questId), questId, status, packedVariables, plans,
				afterCommit, Set.of(PREREQUISITE_QUEST_ID), QuestStartEligibility.allowed(), Map.of());

			// 1. 点选任务直接下发接取确认窗 (4)
			QuestEventRouter.DispatchResult select = dispatcher.dispatch(
				new QuestEvent.TalkToNpc(START_NPC_ID, QuestDialogAction.QUEST_SELECT.id(), NPC_OBJECT_ID),
				PLAYER_ID, questId, QuestDispatchContract.EXCLUSIVE);
			assertTrue(select.handled());
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id())), afterCommit);

			// 3. 点击接受任务
			afterCommit.clear();
			QuestEventRouter.DispatchResult accept = dispatcher.dispatch(
				new QuestEvent.TalkToNpc(START_NPC_ID, QuestDialogAction.QUEST_ACCEPT_1.id(), NPC_OBJECT_ID),
				PLAYER_ID, questId, QuestDispatchContract.EXCLUSIVE);
			assertTrue(accept.handled());
			assertEquals(QuestStatus.START, status.get());
			assertEquals(0, packedVariables.get());
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.QUEST_ACCEPT_1.id())), afterCommit);

			// 4. 击杀怪第 64 只计数加到 64 (仍然在 started)
			packedVariables.set(63);
			afterCommit.clear();
			QuestEventRouter.DispatchResult kill64 = dispatcher.dispatch(
				new QuestEvent.KillNpc(216897), PLAYER_ID, questId, QuestDispatchContract.EXCLUSIVE);
			assertTrue(kill64.handled());
			assertEquals(QuestStatus.START, status.get());
			assertEquals(64, packedVariables.get());

			// 5. 击杀第 65 只怪，翻入 REWARD 状态且 var0=65
			afterCommit.clear();
			QuestEventRouter.DispatchResult kill65 = dispatcher.dispatch(
				new QuestEvent.KillNpc(216897), PLAYER_ID, questId, QuestDispatchContract.EXCLUSIVE);
			assertTrue(kill65.handled());
			assertEquals(QuestStatus.REWARD, status.get());
			assertEquals(65, packedVariables.get());
		}

		// 18505 接取流: 31 -> 4 (SHOW_ASK_QUEST_ACCEPT_WINDOW) -> 1002 (QUEST_ACCEPT_1) -> 1003
		{
			int questId = 18505;
			AtomicReference<QuestStatus> status = new AtomicReference<>(QuestStatus.NONE);
			AtomicInteger packedVariables = new AtomicInteger();
			List<QuestMutationPlan> plans = new ArrayList<>();
			List<AfterCommitAction> afterCommit = new ArrayList<>();

			Map<Integer, Integer> items = Map.of(
				182212005, 1,
				182212006, 1,
				182212007, 1);

			QuestProductionDispatcher dispatcher = dispatcher(
				ProductionQuestDefinitions.definitionInOverlay(questId), questId, status, packedVariables, plans,
				afterCommit, Set.of(PREREQUISITE_QUEST_ID), QuestStartEligibility.allowed(), items);

			// 1. 点选任务直接进入 SHOW_ASK_QUEST_ACCEPT_WINDOW (页 4)
			QuestEventRouter.DispatchResult select = dispatcher.dispatch(
				new QuestEvent.TalkToNpc(START_NPC_ID, QuestDialogAction.QUEST_SELECT.id(), NPC_OBJECT_ID),
				PLAYER_ID, questId, QuestDispatchContract.EXCLUSIVE);
			assertTrue(select.handled());
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id())), afterCommit);

			// 2. 点击接受任务
			afterCommit.clear();
			QuestEventRouter.DispatchResult accept = dispatcher.dispatch(
				new QuestEvent.TalkToNpc(START_NPC_ID, QuestDialogAction.QUEST_ACCEPT_1.id(), NPC_OBJECT_ID),
				PLAYER_ID, questId, QuestDispatchContract.EXCLUSIVE);
			assertTrue(accept.handled());
			assertEquals(QuestStatus.START, status.get());
			assertEquals(0, packedVariables.get());
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.QUEST_ACCEPT_1.id())), afterCommit);

			// 3. 拥有收集品向交付 NPC (203106) 对话 QUEST_SELECT(31)，翻入 REWARD 状态
			afterCommit.clear();
			QuestEventRouter.DispatchResult deliver = dispatcher.dispatch(
				new QuestEvent.TalkToNpc(TURN_IN_NPC_ID_18505, QuestDialogAction.QUEST_SELECT.id(), NPC_OBJECT_ID),
				PLAYER_ID, questId, QuestDispatchContract.EXCLUSIVE);
			assertTrue(deliver.handled());
			assertEquals(QuestStatus.REWARD, status.get());
			assertEquals(0, packedVariables.get());
		}
	}

	private static QuestProductionDispatcher dispatcher(CompiledQuestDefinition definition, int questId,
			AtomicReference<QuestStatus> status, AtomicInteger packedVariables, List<QuestMutationPlan> plans,
			List<AfterCommitAction> afterCommit, Set<Integer> completedQuests,
			QuestStartEligibility startEligibility, Map<Integer, Integer> inventoryItems) {
		QuestEventPort eventPort = (connection, playerId, ignoredQuestId, event) ->
			new QuestSnapshot(playerId, questId, status.get(), packedVariables.get(), inventoryItems)
				.withStartEligibility(startEligibility)
				.withCompletedQuestIds(completedQuests);
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
			QuestHaramelSubsequentQuestsProductionFlowTest::connection, ignored -> { },
			new QuestRuntimeMetricsCollector());
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
}
