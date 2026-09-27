package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.ProductionQuestDefinitions;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.definition.QuestStartCondition;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestDispatchToVerteronFamilyProductionFlowTest {
	private static final int PLAYER_ID = 7;
	private static final int TRANSPORT_NPC_ID = 203726;
	private static final int REWARD_NPC_ID = 203097;
	private static final int NPC_OBJECT_ID = 900_007;
	private static final List<Integer> QUEST_IDS = List.of(1913, 1914, 1915, 1916, 19070, 19071);
	/**
	 * S2 规范段行：同时具备 NPC_START 与 NPC_REPORT 块（retention owner = RETAIL_TABLE）的四行，
	 * 交付边 {@code QUEST_SELECT(31)} 直翻 REWARD 并下发档位奖励窗。
	 * 19070/19071 无 NPC_START 块（retention owner = XML_RETENTION），保留登记的 SELECT5 报告页与 1009 中转。
	 * S2 canonical-segment rows (both NPC_START and NPC_REPORT blocks, RETAIL_TABLE retention): the delivery edge
	 * flips REWARD on {@code QUEST_SELECT(31)} and shows the tiered window. 19070/19071 are XML-retained without an
	 * NPC_START block, so they keep the registered SELECT5 report page and the 1009 relay.
	 */
	private static final Set<Integer> CANONICAL_DELIVERY_QUEST_IDS = Set.of(1913, 1914, 1915, 1916);

	@Test
	void preservesRetailMetadataForTechnistDispatchBranches() throws Exception {
		Map<Integer, Set<String>> classes = Map.of(
			19070, Set.of("GUNSLINGER", "AETHERTECH"),
			19071, Set.of("SONGWEAVER"));
		Map<Integer, Integer> rewardModes = Map.of(19070, 4, 19071, 5);
		for (int questId : List.of(19070, 19071)) {
			var metadata = definition(questId).definition().metadata();
			assertEquals(classes.get(questId), metadata.permittedClasses());
			assertEquals(14046, metadata.rewards().getFirst().amount());
			assertEquals(List.of(new QuestStartCondition("finished", 1007, rewardModes.get(questId))),
				metadata.startConditions());
			assertEquals(List.of(), metadata.questWorkItems());
		}
	}

	@Test
	void everyClassBranchTeleportsBeforeRewardingAtHyacinte() throws Exception {
		for (int questId : QUEST_IDS) {
			AtomicReference<QuestStatus> status = new AtomicReference<>(QuestStatus.START);
			AtomicInteger packedVariables = new AtomicInteger();
			List<QuestMutationPlan> plans = new ArrayList<>();
			List<AfterCommitAction> afterCommit = new ArrayList<>();
			QuestProductionDispatcher dispatcher = dispatcher(
				definition(questId), questId, status, packedVariables, plans, afterCommit);

			boolean canonicalDelivery = CANONICAL_DELIVERY_QUEST_IDS.contains(questId);

			QuestEventRouter.DispatchResult prematureReward = dispatch(
				dispatcher, questId, REWARD_NPC_ID, 1009);

			assertNotHandled(prematureReward, questId + " rewarded before transfer");
			assertEquals(QuestStatus.START, status.get());
			assertEquals(0, packedVariables.get());
			assertTrue(plans.isEmpty());
			assertTrue(afterCommit.isEmpty());

			if (canonicalDelivery) {
				// 规范段行的交付边锚在传送后的 started1：传送前（started）连 QUEST_SELECT(31) 也零路由。
				// The canonical delivery edge is anchored at the post-transfer started1, so even
				// QUEST_SELECT(31) has no route before the transfer.
				assertNotHandled(dispatch(dispatcher, questId, REWARD_NPC_ID, 31),
					questId + " delivered before the transfer");
				assertEquals(QuestStatus.START, status.get());
				assertEquals(0, packedVariables.get());
				assertTrue(plans.isEmpty());
				assertTrue(afterCommit.isEmpty());
			}

			QuestEventRouter.DispatchResult teleport = dispatch(
				dispatcher, questId, TRANSPORT_NPC_ID, 10000);

			assertHandled(teleport, questId + " did not handle the transfer dialog");
			assertEquals(QuestStatus.START, status.get());
			assertEquals(1, packedVariables.get());
			assertEquals(QuestStatus.START, plans.getLast().nextStatus());
			assertEquals(1, plans.getLast().nextPackedVariables());
			assertEquals(List.of(new QuestAction.SetVariable("var0", 1)), plans.getLast().requiredActions());
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.TeleportPlayer(210030000, 1643f, 1500f, 120f, (byte) 0),
				new AfterCommitAction.CloseDialog()), afterCommit);

			plans.clear();
			afterCommit.clear();
			QuestEventRouter.DispatchResult wrongRewardNpc = dispatch(
				dispatcher, questId, TRANSPORT_NPC_ID, 1009);

			assertNotHandled(wrongRewardNpc, questId + " still rewards at Polyidus");
			assertTrue(plans.isEmpty());
			assertTrue(afterCommit.isEmpty());

			if (canonicalDelivery) {
				// S2 规范形交付：QUEST_SELECT(31) 带整组交付门（本族空门）直翻 REWARD，
				// 并下发本档奖励窗（单档 → 窗 1 页 5）；SELECT5 报告页与 1009 中转已退场。
				// S2 canonical delivery: QUEST_SELECT(31) carries the whole hand-in gate (empty for this
				// family) and flips REWARD with the tiered window (one group -> window 1, page 5).
				QuestEventRouter.DispatchResult reward = dispatch(
					dispatcher, questId, REWARD_NPC_ID, 31);

				assertHandled(reward, questId + " did not enter reward state at Hyacinte");
				assertEquals(QuestStatus.REWARD, status.get());
				assertEquals(1, packedVariables.get());
				assertEquals(QuestStatus.REWARD, plans.getLast().nextStatus());
				assertEquals(1, plans.getLast().nextPackedVariables());
				assertTrue(plans.getLast().requiredActions().isEmpty());
				assertEquals(List.of(
					new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
					new AfterCommitAction.ShowQuestDialog(5)), afterCommit);
			} else {
				// 保留 XML 行（γ 面）：31 只下发 SELECT5 报告页，1009 才提交并翻 REWARD。
				// XML-retained rows (the gamma face) keep the registered report page: 31 shows SELECT5
				// and 1009 commits the hand-in.
				QuestEventRouter.DispatchResult rewardOffer = dispatch(
					dispatcher, questId, REWARD_NPC_ID, 31);

				assertHandled(rewardOffer, questId + " did not open the Hyacinte reward dialog");
				assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(2375)), afterCommit);

				afterCommit.clear();
				QuestEventRouter.DispatchResult reward = dispatch(
					dispatcher, questId, REWARD_NPC_ID, 1009);

				assertHandled(reward, questId + " did not enter reward state at Hyacinte");
				assertEquals(QuestStatus.REWARD, status.get());
				assertEquals(1, packedVariables.get());
				assertEquals(QuestStatus.REWARD, plans.getLast().nextStatus());
				assertEquals(1, plans.getLast().nextPackedVariables());
				assertTrue(plans.getLast().requiredActions().isEmpty());
				assertEquals(List.of(
					new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
					new AfterCommitAction.ShowQuestDialog(5)), afterCommit);
			}
		}
	}

	private static QuestProductionDispatcher dispatcher(CompiledQuestDefinition definition, int questId,
			AtomicReference<QuestStatus> status, AtomicInteger packedVariables, List<QuestMutationPlan> plans,
			List<AfterCommitAction> afterCommit) {
		QuestEventPort eventPort = (connection, playerId, ignoredQuestId, event) ->
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
			QuestDispatchToVerteronFamilyProductionFlowTest::connection, ignored -> { },
			new QuestRuntimeMetricsCollector());
	}

	private static QuestEventRouter.DispatchResult dispatch(QuestProductionDispatcher dispatcher,
			int questId, int npcId, int dialogId) {
		return dispatcher.dispatch(new QuestEvent.TalkToNpc(npcId, dialogId, NPC_OBJECT_ID),
			PLAYER_ID, questId, QuestDispatchContract.EXCLUSIVE);
	}

	private static CompiledQuestDefinition definition(int questId) throws Exception {
		return ProductionQuestDefinitions.definitionInOverlay(questId);
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

	private static void assertHandled(QuestEventRouter.DispatchResult result, String message) {
		assertNoFailure(result);
		assertTrue(result.handled(), () -> message + ": " + result);
	}

	private static void assertNotHandled(QuestEventRouter.DispatchResult result, String message) {
		assertNoFailure(result);
		assertFalse(result.handled(), () -> message + ": " + result);
	}

	private static void assertNoFailure(QuestEventRouter.DispatchResult result) {
		result.owners().stream().map(QuestEventRouter.OwnerResult::failure)
			.filter(java.util.Objects::nonNull).findFirst().ifPresent(failure -> {
				throw failure;
			});
	}
}
