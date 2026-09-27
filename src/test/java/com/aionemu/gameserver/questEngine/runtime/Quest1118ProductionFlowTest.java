package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionXmlCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
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
		CompiledQuestDefinition definition = definition();
		QuestProductionDispatcher dispatcher = dispatcher(definition, status, packedVariables, inventory, plans,
			appliedActions, afterCommit);

		// S3a（quest-native-dispatch）：SimpleTalk 链式行的接取段换 canonical 形——接取 NPC 上的
		// QUEST_SELECT(31) 直发原生接取询问窗（页 4），旧 select1 入口页（1011）与 ASK_QUEST_ACCEPT(1007)
		// 中转随页梯退场。开窗只下发页面：不建档、不发物，发物落在下面两条提交边上。
		// S3a (quest-native-dispatch): the chain accept segment takes the canonical shape — QUEST_SELECT(31)
		// on the accept npc opens the native ask window (page 4) directly; the legacy select1 entry page
		// (1011) and the 1007 relay retire with the page ladder. Opening the window only pushes a page:
		// no start and no grant, since the grant rides the two commit edges asserted below.
		assertHandled(dispatch(dispatcher, 203059, 31));
		assertEquals(QuestStatus.NONE, status.get());
		assertTrue(inventory.isEmpty());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id())), afterCommit);

		afterCommit.clear();
		assertHandled(dispatch(dispatcher, 203059, 1002));
		assertEquals(QuestStatus.START, status.get());
		assertEquals(0, packedVariables.get());
		assertEquals(1, inventory.get(OINTMENT_ITEM_ID));
		assertEquals(List.of(new QuestAction.GiveItem(OINTMENT_ITEM_ID, 1)), appliedActions.getLast());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(1003)), afterCommit);

		// 接取发物落在两条提交边（1002 与 20000）的 actions 上，条件同为 StartEligible：canonical 形里
		// 「接取即发 182200224×1」由提交边承担，而不是接取窗口或已退场的中转边。
		// The accept grant rides both commit edges (1002 and 20000) as route actions under the same
		// StartEligible condition: in the canonical shape the "accept grants 182200224x1" semantics is
		// carried by these edges rather than by the ask window or the retired relay.
		for (int commit : List.of(QuestDialogAction.QUEST_ACCEPT_1.id(),
				QuestDialogAction.QUEST_ACCEPT_SIMPLE.id())) {
			QuestTransition accept = definition.transitionsFor("TALK_TO_NPC").stream()
				.filter(transition -> transition.event().equals(new QuestEvent.TalkToNpc(203059, commit)))
				.findFirst()
				.orElseThrow(() -> new AssertionError("quest 1118 npc 203059 has no commit " + commit));
			assertEquals(List.of(new QuestCondition.StartEligible()), accept.conditions());
			assertEquals(List.of(new QuestAction.GiveItem(OINTMENT_ITEM_ID, 1)), accept.actions());
		}

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

		// S3c（quest-native-dispatch）：交付段也换规范形——交付 NPC 上的 QUEST_SELECT(31) 带整组
		// `HasItem(182200224,1)` 门**直接提交**（扣物 + 直翻 REWARD）并下发档位奖励窗（页 5）；
		// 旧报告页 SELECT5(2375) 与 39/20002 检查对随页链退场（未集齐时零路由，关窗兜底交 DialogService）。
		// S3c: the delivery segment takes the canonical shape — the reward npc's QUEST_SELECT(31) carries the
		// whole HasItem gate, consumes the ointment, flips REWARD directly and pushes the tiered reward
		// window (page 5); the SELECT5 report page and the 39/20002 check pair retire.
		afterCommit.clear();
		assertHandled(dispatch(dispatcher, 203079, 31));
		assertEquals(QuestStatus.REWARD, status.get());
		assertEquals(1, packedVariables.get());
		assertTrue(inventory.isEmpty());
		assertEquals(List.of(new QuestAction.RemoveItem(OINTMENT_ITEM_ID, 1)), appliedActions.getLast());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(5)), afterCommit);
		assertEquals(QuestStatus.REWARD, plans.getLast().nextStatus());

		// 领奖态重开：回收站态的重开载体由 `npc-complete` 预览腿承担（reward 态的 1009 只重发奖励窗，
		// 不再走报告页 / 检查对；QE-083 预览语义）。
		// Reward-state reopen: the carrier is the npc-complete preview leg (a reward-state 1009 only
		// re-pushes the reward window, no report page and no check pair).
		afterCommit.clear();
		assertHandled(dispatch(dispatcher, 203079, 1009));
		assertEquals(QuestStatus.REWARD, status.get());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(5)), afterCommit);
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
			if (action instanceof QuestAction.GiveItem(int itemId1, int count2)) {
				inventory.merge(itemId1, count2, Integer::sum);
			} else if (action instanceof QuestAction.RemoveItem(int id, int count1)) {
				inventory.compute(id, (itemId, count) -> {
					int remaining = count - count1;
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
		return com.aionemu.gameserver.questEngine.definition.ProductionQuestDefinitions.definitionInOverlay(1118);
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
