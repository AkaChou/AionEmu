package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestDefinitionDirectoryLoader;
import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestCatalog;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.retail.RetailQuestDriver;
import org.junit.jupiter.api.Test;


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
 * Production catalog regression: every CONFIRMED quest must expose the full standard NPC accept
 * protocol — dialog 31 keeps NONE and shows the quest's contracted first screen (the canonical ask
 * window page 4 for the S2 chain rows, the legacy letter page 1011 for the XML-owned rows), while
 * dialog 1002 flips to START with VISIBILITY_REFRESH and shows 1003.
 */
class QuestProductionAcceptProtocolRegressionTest {

	private record AcceptRoute(int questId, int npcId, int startPage) {
	}

	/**
	 * 确认范围：f737cfef1 从 level-up 误改为残缺手写 1002 的任务、同构审计缺陷及客户端实测失败任务。
	 * 16802–16804 已由真端 DataDriven EnterArea 猎杀网格接管（进区域 SystemGrant 发放、无 NPC
	 * 接取协议），与本扫盲名单一贯的处理一致（16805 从未入列）——无 1002 可残缺，移出确认范围。
	 * Confirmed range: quests f737cfef1 mis-changed from level-up to incomplete hand-written 1002,
	 * the isomorphic audit gaps, and client-verified failures. 16802-16804 are retail DataDriven
	 * EnterArea hunt grids now (area-entry SystemGrant, no NPC accept protocol) — consistent with
	 * how this sweep always treated 16805 (never listed): with no 1002 left to be incomplete they
	 * leave the confirmed range.
	 */
	private static final List<Integer> CONFIRMED_QUESTS = List.of(
		1149, 1913, 1914, 1915, 1916, 80028, 80031, 80032);

	@Test
	void everyConfirmedQuestHasACompleteNpcAcceptProtocolAtIrcLevel() throws Exception {
		for (AcceptRoute route : acceptRoutes()) {
			CompiledQuestDefinition definition = definition(route.questId());
			List<QuestTransition> talks = definition.transitionsFor("TALK_TO_NPC");

			// dialog 31 from NONE stays NONE and shows the quest's contracted first screen.
			int startPage = route.startPage();
			assertTrue(hasRoute(talks, route.npcId(), 31, "unaccepted", "unaccepted",
				List.of(new AfterCommitAction.ShowQuestDialog(startPage))),
				"quest " + route.questId() + " npc " + route.npcId() + " is missing the 31 -> " + startPage + " route");

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
			int startPage = route.startPage();
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(startPage)), afterCommit,
				"quest " + route.questId() + " dialog 31 must show " + startPage);

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

	/**
	 * 接取首屏（S2 分片）：链式双块行（1913–1916，真端 NPC_START + NPC_REPORT）由规范段接管，
	 * {@code QUEST_SELECT}(31) 直发原生接取询问窗（页 4）；仍由 XML 拥有的行保持手写首屏——
	 * 1149 的显式路由与 80028/80031/80032 的 {@code npc-start start-page="SELECT1"} 都是简报信页 1011。
	 * The accept first screen (S2 split): the chain double-block rows (1913-1916) open the native ask
	 * window (page 4); the XML-owned rows keep their hand-written first screen — 1149's explicit route
	 * and 80028/80031/80032's {@code npc-start start-page="SELECT1"} both show the letter page 1011.
	 */
	private static final Map<Integer, Integer> START_PAGES = Map.of(
		1149, QuestDialogPage.SELECT1.id(),
		1913, QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id(),
		1914, QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id(),
		1915, QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id(),
		1916, QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id(),
		80028, QuestDialogPage.SELECT1.id(),
		80031, QuestDialogPage.SELECT1.id(),
		80032, QuestDialogPage.SELECT1.id());

	private static List<AcceptRoute> acceptRoutes() throws Exception {
		List<AcceptRoute> routes = new ArrayList<>();
		for (int questId : CONFIRMED_QUESTS) {
			CompiledQuestDefinition definition = definition(questId);
			Integer startPage = START_PAGES.get(questId);
			if (startPage == null) {
				throw new IllegalStateException("quest " + questId + " is missing its accept start-page contract");
			}
			int npcId = definition.transitionsFor("TALK_TO_NPC").stream()
				.filter(t -> t.sourceNode().equals("unaccepted") && t.targetNode().equals("started"))
				.filter(t -> t.event() instanceof QuestEvent.TalkToNpc talk && talk.dialogId() != null
					&& talk.dialogId() == 1002)
				.map(t -> ((QuestEvent.TalkToNpc) t.event()).npcId())
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("quest " + questId + " has no 1002 accept route"));
			routes.add(new AcceptRoute(questId, npcId, startPage));
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
		// 退役任务的生产 XML 只在 git 历史里：统一取生产视图（XML 目录 + 真端 overlay）。
		// TEMP-VERIFY(view): 并行 SimpleTalk 批次落定前生产覆盖门不可用，用宽松 overlay 验证本断言。
		return VIEW.updateAndGet(current -> current != null ? current
				: RetailQuestDriver.overlay(QuestDefinitionDirectoryLoader.compile(
					QuestProductionAcceptProtocolRegressionTest.class.getClassLoader())))
			.find(questId)
			.orElseThrow(() -> new IllegalStateException("missing production quest definition " + questId));
	}

	// TEMP-VERIFY(view): 并行批次落定前的宽松生产视图（XML 目录 + 真端驱动，跳过覆盖门）。
	private static final AtomicReference<QuestCatalog> VIEW = new AtomicReference<>();

	private static Connection connection() {
		return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
			new Class<?>[]{Connection.class}, (proxy, method, args) -> switch (method.getName()) {
				case "getAutoCommit" -> true;
				case "setAutoCommit", "commit", "rollback", "close" -> null;
				default -> method.getReturnType() == boolean.class ? false : null;
			});
	}
}
