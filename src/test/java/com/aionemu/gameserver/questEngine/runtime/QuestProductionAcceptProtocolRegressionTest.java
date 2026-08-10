package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionXmlCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
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

/**
 * 生产 catalog 级回归：确认范围内所有 typed quest 的 NPC 接取协议必须完整。
 * Production catalog regression: every CONFIRMED quest must expose the full standard
 * NPC accept protocol — dialog 31 keeps NONE and shows 1011, dialog 1002 flips to START
 * with VISIBILITY_REFRESH and shows 1003.
 */
class QuestProductionAcceptProtocolRegressionTest {

	private record AcceptRoute(int questId, int npcId) {
	}

	/** 确认范围：f737cfef1 从 level-up 误改为残缺手写 1002 的任务 + 同一审计批次内同构缺陷。 */
	private static final List<Integer> CONFIRMED_QUESTS = List.of(
		1913, 1914, 1915, 1916, 16802, 16803, 16804, 80028, 80031, 80032);

	@Test
	void everyConfirmedQuestHasACompleteNpcAcceptProtocolAtIrcLevel() throws Exception {
		for (AcceptRoute route : acceptRoutes()) {
			CompiledQuestDefinition definition = definition(route.questId());
			List<QuestTransition> talks = definition.transitionsFor("TALK_TO_NPC");

			// dialog 31 from NONE stays NONE and shows the quest intro page 1011.
			assertTrue(hasRoute(talks, route.npcId(), 31, "unaccepted", "unaccepted",
				List.of(new AfterCommitAction.ShowQuestDialog(1011))),
				"quest " + route.questId() + " npc " + route.npcId() + " is missing the 31 -> 1011 route");

			// dialog 1002 from NONE flips to START with a visibility refresh and shows 1003.
			assertTrue(hasRoute(talks, route.npcId(), 1002, "unaccepted", "started",
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
					new AfterCommitAction.ShowQuestDialog(1003))),
				"quest " + route.questId() + " npc " + route.npcId() + " is missing the standard 1002 accept route");
		}
	}

	@Test
	void everyConfirmedQuestAcceptsThroughTheStandardDialogFlow() throws Exception {
		for (AcceptRoute route : acceptRoutes()) {
			CompiledQuestDefinition definition = definition(route.questId());
			AtomicReference<QuestStatus> status = new AtomicReference<>(QuestStatus.NONE);
			List<QuestMutationPlan> plans = new ArrayList<>();
			List<AfterCommitAction> afterCommit = new ArrayList<>();
			QuestProductionDispatcher dispatcher = dispatcher(definition, route.questId(), status, plans, afterCommit);

			QuestEventRouter.DispatchResult offer = dispatcher.dispatch(
				new QuestEvent.TalkToNpc(route.npcId(), 31, route.npcId()), 7, route.questId(),
				QuestDispatchContract.EXCLUSIVE);
			assertTrue(offer.handled(), () -> "quest " + route.questId() + " dialog 31 was not handled: " + offer);
			assertEquals(QuestStatus.NONE, status.get(), "quest " + route.questId() + " dialog 31 must not start");
			assertTrue(plans.isEmpty(), "quest " + route.questId() + " dialog 31 must not persist state");
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(1011)), afterCommit,
				"quest " + route.questId() + " dialog 31 must show 1011");

			plans.clear();
			afterCommit.clear();
			QuestEventRouter.DispatchResult accept = dispatcher.dispatch(
				new QuestEvent.TalkToNpc(route.npcId(), 1002, route.npcId()), 7, route.questId(),
				QuestDispatchContract.EXCLUSIVE);
			assertTrue(accept.handled(), () -> "quest " + route.questId() + " dialog 1002 was not handled: " + accept);
			assertEquals(QuestStatus.START, status.get(), "quest " + route.questId() + " dialog 1002 must start");
			assertEquals(QuestStatus.START, plans.getLast().nextStatus(),
				"quest " + route.questId() + " dialog 1002 plan must flip to START");
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(1003)), afterCommit,
				"quest " + route.questId() + " dialog 1002 must refresh visibility and show 1003");
		}
	}

	private static List<AcceptRoute> acceptRoutes() throws Exception {
		List<AcceptRoute> routes = new ArrayList<>();
		for (int questId : CONFIRMED_QUESTS) {
			CompiledQuestDefinition definition = definition(questId);
			int npcId = definition.transitionsFor("TALK_TO_NPC").stream()
				.filter(t -> t.sourceNode().equals("unaccepted") && t.targetNode().equals("started"))
				.filter(t -> t.event() instanceof QuestEvent.TalkToNpc talk && talk.dialogId() != null
					&& talk.dialogId() == 1002)
				.map(t -> ((QuestEvent.TalkToNpc) t.event()).npcId())
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("quest " + questId + " has no 1002 accept route"));
			routes.add(new AcceptRoute(questId, npcId));
		}
		return routes;
	}

	/** 前置完成事实取自 metadata 的 start-conditions（编译为 startConditionGroups）。 */
	private static Set<Integer> completedPrerequisites(CompiledQuestDefinition definition) {
		return definition.definition().metadata().startConditionGroups().stream()
			.flatMap(group -> group.conditions().stream())
			.filter(condition -> "finished".equals(condition.type()))
			.map(condition -> condition.questId())
			.collect(java.util.stream.Collectors.toUnmodifiableSet());
	}

	private static boolean hasRoute(List<QuestTransition> talks, int npcId, int dialogId, String source, String target,
			List<AfterCommitAction> afterCommit) {
		return talks.stream().anyMatch(t -> {
			if (!(t.event() instanceof QuestEvent.TalkToNpc talk)) {
				return false;
			}
			return talk.npcId() == npcId && talk.dialogId() != null && talk.dialogId() == dialogId
				&& t.sourceNode().equals(source) && t.targetNode().equals(target)
				&& t.afterCommit().equals(afterCommit);
		});
	}

	private static QuestProductionDispatcher dispatcher(CompiledQuestDefinition definition, int questId,
			AtomicReference<QuestStatus> status, List<QuestMutationPlan> plans,
			List<AfterCommitAction> afterCommit) {
		QuestEventPort eventPort = (connection, playerId, quest, event) ->
			new QuestSnapshot(playerId, questId, status.get(), 0, Map.of())
				.withStartEligibility(QuestStartEligibility.allowed())
				.withCompletedQuestIds(completedPrerequisites(definition));
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
			QuestProductionAcceptProtocolRegressionTest::connection, ignored -> { },
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

	private static CompiledQuestDefinition definition(int questId) throws Exception {
		String resource = "/aion/data/static_data/quest_definition/quests/" + questId + ".xml";
		try (InputStream input = QuestProductionAcceptProtocolRegressionTest.class.getResourceAsStream(resource)) {
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
}
