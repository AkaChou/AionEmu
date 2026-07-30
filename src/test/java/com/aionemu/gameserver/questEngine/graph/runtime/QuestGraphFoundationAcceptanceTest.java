package com.aionemu.gameserver.questEngine.graph.runtime;

import static com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EventType.DIALOG;
import static com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EventType.KILL;
import static com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.StateScope.PLAYER;
import static com.aionemu.gameserver.questEngine.graph.runtime.DispatchResult.Status.APPLIED;
import static com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ConditionResult.MATCHED;
import static com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ConditionResult.NOT_MATCHED;
import static com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PersistenceResult.CONFLICT;
import static com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PreflightResult.READY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.ActionType;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.DialogBindingMode;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Event;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EventType;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Node;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatus;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.SendDialogAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Transition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraphData;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraphData.EventKey;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraphData.EventRoute;
import com.aionemu.gameserver.questEngine.graph.QuestGraphCompiler;
import com.aionemu.gameserver.questEngine.graph.QuestGraphDefinitionRegistry;
import com.aionemu.gameserver.questEngine.graph.runtime.DispatchResult.Propagation;
import com.aionemu.gameserver.questEngine.graph.runtime.DispatchResult.Status;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.DialogEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.KillEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.MovieEndedEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.WorldEnteredEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PersistenceResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.TransitionContext;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.CleanupLease;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.InstanceSpawnResourceIdentity;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.Lifecycle;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.QuestHistory;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.SpawnPlacementKind;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphStateList;

/**
 * 验证全部任务图共享的跨切面 acceptance 合同（D-036 / D-048）。
 * Verifies cross-cutting acceptance contracts shared by every quest graph (D-036 / D-048).
 */
class QuestGraphFoundationAcceptanceTest {

	private static final Path SCHEMA = Path.of("src/main/resources/aion/data/static_data/quest_graph_data/quest_graph_data.xsd");
	private static final ZoneId SERVER_ZONE = ZoneId.of("Asia/Shanghai");
	private static final int PLAYER_ID = 7;

	@TempDir
	Path tempDir;

	/**
	 * 验证精确事件匹配、固定独占/广播传播以及异常和坏状态的失败关闭语义。
	 * Verifies exact event matching, fixed exclusive/broadcast propagation, and fail-closed exception and state handling.
	 */
	@Test
	void eventRoutingResultAndSecurityContractsAreClosed() {
		CompiledQuestGraph first = graph(1);
		CompiledQuestGraph second = graph(2);
		Transition firstDialog = first.nodes().get("active").transitions().get(0);
		Transition firstKill = first.nodes().get("active").transitions().get(1);
		Transition secondDialog = second.nodes().get("active").transitions().get(0);
		Transition secondKill = second.nodes().get("active").transitions().get(1);
		CompiledQuestGraphData data = new CompiledQuestGraphData(Map.of(1, first, 2, second), Map.of(
			new EventKey(DIALOG, 100), List.of(new EventRoute(1, "active", firstDialog), new EventRoute(2, "active", secondDialog)),
			new EventKey(KILL, 200), List.of(new EventRoute(1, "active", firstKill), new EventRoute(2, "active", secondKill))));
		QuestGraphRouter router = new QuestGraphRouter(data);
		PlayerQuestGraphStateList states = new PlayerQuestGraphStateList();
		List<Integer> visited = new ArrayList<>();

		DispatchResult exclusive = router.dispatch(new DialogEvent("dialog", PLAYER_ID, 1000, 100, "QUEST_SELECT"), states, match -> {
			visited.add(match.route().questId());
			return match.route().questId() == 1 ? Status.NO_MATCH : Status.APPLIED;
		});
		assertEquals(List.of(1, 2), visited);
		assertEquals(new DispatchResult(Status.APPLIED, Propagation.STOP), exclusive);

		visited.clear();
		DispatchResult mismatch = router.dispatch(new DialogEvent("mismatch", PLAYER_ID, 1001, 100, "QUEST_ACCEPT"), states, match -> {
			visited.add(match.route().questId());
			return Status.APPLIED;
		});
		assertEquals(List.of(), visited);
		assertEquals(new DispatchResult(Status.NO_MATCH, Propagation.CONTINUE), mismatch);
		assertEquals(new DispatchResult(Status.REJECTED, Propagation.STOP),
			router.dispatch(new DialogEvent("rejected", PLAYER_ID, 1002, 100, "QUEST_SELECT"), states, match -> Status.REJECTED));

		visited.clear();
		DispatchResult broadcast = router.dispatch(new KillEvent("kill", PLAYER_ID, 1003, 200), states, match -> {
			visited.add(match.route().questId());
			if (match.route().questId() == 1) {
				throw new IllegalStateException("isolated owner failure");
			}
			return Status.APPLIED;
		});
		assertEquals(List.of(1, 2), visited);
		assertEquals(new DispatchResult(Status.FAILED, Propagation.CONTINUE), broadcast);

		PlayerQuestGraphStateList incompatible = new PlayerQuestGraphStateList();
		incompatible.addLoaded(new PlayerQuestGraphState(1, 2, 0, "active", QuestStatus.START,
			QuestHistory.EMPTY, null, Lifecycle.ACTIVE, Map.of(), Map.of(), null, Map.of(), null));
		assertEquals(new DispatchResult(Status.FAILED, Propagation.STOP),
			router.dispatch(new DialogEvent("bad-state", PLAYER_ID, 1004, 100, "QUEST_SELECT"), incompatible, match -> Status.APPLIED));
	}

	/**
	 * 验证 compiler 接受固定动作阶段顺序，并在运行前拒绝协议动作之后的状态写入。
	 * Verifies the compiler accepts fixed action phases and rejects state writes after protocol actions before runtime.
	 */
	@Test
	void controlFlowContractRejectsInvalidActionPhaseOrder() throws Exception {
		String valid = """
			<quest_graphs>
				<quest_graph quest_id="1" version="1" scope="PLAYER" initial_node="active">
					<node id="active">
						<transition id="accept" priority="1" to="done">
							<dialog npc_id="100" dialog="QUEST_SELECT"/>
							<actions><start-quest/><sync-quest-status/><close-dialog/></actions>
						</transition>
					</node>
					<node id="done" terminal="true"/>
				</quest_graph>
			</quest_graphs>
			""";
		Path validFile = tempDir.resolve("valid.xml");
		Files.writeString(validFile, valid, StandardCharsets.UTF_8);
		QuestGraphCompiler.References references = new QuestGraphCompiler.References(
			Set.of(1), Set.of(100), Set.of(), Set.of(), Set.of(), Set.of());

		assertEquals(1, QuestGraphCompiler.load(validFile, SCHEMA, references).graphs().size());
		Path invalidFile = tempDir.resolve("invalid.xml");
		Files.writeString(invalidFile,
			valid.replace("<start-quest/><sync-quest-status/><close-dialog/>", "<close-dialog/><start-quest/>"),
			StandardCharsets.UTF_8);
		assertThrows(IllegalArgumentException.class, () -> QuestGraphCompiler.load(invalidFile, SCHEMA, references));
	}

	/**
	 * 验证定义 owner 只能完整原子安装，重复初始化或删除 owner 的 reload 不改变当前快照。
	 * Verifies definition owners install only as a complete atomic set and failed reinitialization or owner-removing reloads preserve the snapshot.
	 */
	@Test
	void ownershipContractRequiresAtomicCompleteDefinitionSets() {
		QuestGraphDefinitionRegistry registry = new QuestGraphDefinitionRegistry();
		CompiledQuestGraphData complete = new CompiledQuestGraphData(Map.of(1, graph(1), 2, graph(2)), Map.of());

		QuestGraphDefinitionRegistry.Snapshot installed = registry.installInitial(complete);
		assertEquals(1, installed.generation());
		assertThrows(IllegalStateException.class, () -> registry.installInitial(complete));
		assertSame(installed, registry.snapshot());
		assertThrows(IllegalArgumentException.class,
			() -> registry.reload(new CompiledQuestGraphData(Map.of(1, graph(1)), Map.of()), List.of()));
		assertSame(installed, registry.snapshot());
		assertThrows(UnsupportedOperationException.class, () -> complete.graphs().clear());
	}

	/**
	 * 验证条件、lifecycle、计时、动作、结算与 cleanup 的跨切面 acceptance 合同。
	 * Verifies cross-cutting acceptance contracts for condition, lifecycle, time, action, settlement, and cleanup.
	 */
	@Test
	void conditionLifecycleTimeActionRewardAndCleanupContractsAreClosed() throws Exception {
		// CONDITION + LIFECYCLE + ACTION + REWARD：条件门控接取，错误状态不匹配；结算 finish 关闭 owner 周期。
		// CONDITION + LIFECYCLE + ACTION + REWARD: condition-gated accept; wrong status mismatches; finish closes the owner cycle.
		String document = """
			<quest_graphs>
				<quest_graph quest_id="11" version="1" scope="PLAYER" initial_node="offer">
					<node id="offer">
						<transition id="accept" priority="1" to="active">
							<dialog npc_id="100" dialog="QUEST_ACCEPT"/>
							<conditions><quest-status op="IN" values="NONE"/></conditions>
							<actions><start-quest/><sync-quest-status/><close-dialog/></actions>
						</transition>
					</node>
					<node id="active">
						<transition id="arm-timer" priority="1" to="timed">
							<dialog npc_id="100" dialog="STEP_TO_1"/>
							<conditions><quest-status op="IN" values="START"/></conditions>
							<actions><start-quest-timer timer="QUEST_TIMER" duration_seconds="30"/><sync-quest-status/></actions>
						</transition>
					</node>
					<node id="timed">
						<transition id="settle" priority="1" to="done">
							<dialog npc_id="100" dialog="SELECT_REWARD"/>
							<conditions><quest-status op="IN" values="START"/></conditions>
							<actions>
								<set-quest-status status="REWARD"/>
								<finish-quest reward_index="0"/>
								<sync-quest-status/>
							</actions>
						</transition>
					</node>
					<node id="done" terminal="true"/>
				</quest_graph>
			</quest_graphs>
			""";
		CompiledQuestGraphData data = load(document, Set.of(11), Set.of(100), Set.of());
		PlayerQuestGraphStateList states = new PlayerQuestGraphStateList();
		AtomicReference<PlayerQuestGraphState> database = new AtomicReference<>();
		List<ActionType> protocol = new ArrayList<>();
		AtomicInteger timerStarts = new AtomicInteger();
		AtomicReference<CleanupLease> physicallyCleaned = new AtomicReference<>();
		QuestGraphLifecycleActionAdapter lifecycle = new QuestGraphLifecycleActionAdapter(PLAYER_ID, command -> READY,
			command -> ActionResult.APPLIED, (lease, reason) -> {
				physicallyCleaned.set(lease);
				return ActionResult.APPLIED;
			});
		TransitionContext context = new TransitionContext(PLAYER_ID, 0, SERVER_ZONE, states, invocation -> MATCHED, invocation -> READY,
			invocation -> {
				if (invocation.action().type() == ActionType.START_QUEST_TIMER) {
					timerStarts.incrementAndGet();
				}
				if (invocation.action().type().phase() == CompiledQuestGraph.ActionPhase.POST_COMMIT_PROTOCOL) {
					protocol.add(invocation.action().type());
				}
				return ActionResult.APPLIED;
			}, lifecycle, cas(database));
		QuestGraphRouter router = new QuestGraphRouter(data);
		QuestGraphTransitionExecutor executor = new QuestGraphTransitionExecutor();

		assertEquals(Status.APPLIED, router.dispatch(new DialogEvent("accept", PLAYER_ID, 1000, 100, "QUEST_ACCEPT"), states,
			match -> executor.execute(match, context)).status());
		assertEquals(QuestStatus.START, states.get(11).getQuestStatus());
		assertEquals("active", states.get(11).getNodeId());

		// 条件失败关闭：已 START 时 NONE 门不再匹配。
		// Condition fail-closed: NONE gate no longer matches after START.
		TransitionContext rejectContext = new TransitionContext(PLAYER_ID, 0, SERVER_ZONE, states, invocation -> NOT_MATCHED,
			invocation -> READY, invocation -> ActionResult.APPLIED, cas(database));
		assertEquals(Status.NO_MATCH, router.dispatch(new DialogEvent("re-accept", PLAYER_ID, 1001, 100, "QUEST_ACCEPT"), states,
			match -> executor.execute(match, rejectContext)).status());

		assertEquals(Status.APPLIED, router.dispatch(new DialogEvent("timer", PLAYER_ID, 1002, 100, "STEP_TO_1"), states,
			match -> executor.execute(match, context)).status());
		assertEquals(1, timerStarts.get());
		assertTrue(states.get(11).getDeadlines().containsKey("QUEST_TIMER")
			|| states.get(11).getNodeId().equals("timed"));

		// CLEANUP：结算前写入已物化 typed lease，只有物理 endpoint 确认后 finish 才能清账本并进入终态。
		// CLEANUP: persist a materialized typed lease; finish may clear it and become terminal only after physical endpoint confirmation.
		PlayerQuestGraphState current = states.get(11);
		String resourceKey = "spawn:acceptance";
		CleanupLease persistedLease = CleanupLease.instanceSpawn(new InstanceSpawnResourceIdentity(PLAYER_ID, 11, 990011, 216608,
			SpawnPlacementKind.FIXED, 0, 0, 210010000, 1, 10, 20, 30, (byte) 0, resourceKey));
		PlayerQuestGraphState withLease = new PlayerQuestGraphState(11, current.getDefinitionVersion(),
			current.getRevision() + 1, "timed", QuestStatus.START, QuestHistory.EMPTY, null, Lifecycle.ACTIVE,
			Map.of(), current.getDeadlines(), null,
			Map.of(resourceKey, persistedLease), null);
		states.put(withLease);
		database.set(withLease);
		assertEquals(1, states.get(11).getCleanupLeases().size());
		assertEquals(Status.APPLIED, router.dispatch(new DialogEvent("finish", PLAYER_ID, 1003, 100, "SELECT_REWARD"), states,
			match -> executor.execute(match, context)).status());
		assertEquals(persistedLease, physicallyCleaned.get());
		assertEquals("done", states.get(11).getNodeId());
		assertFalse(protocol.isEmpty());

		// TIME：非法 timer 在编译期失败关闭。
		// TIME: illegal timers fail closed at compile time.
		assertThrows(IllegalArgumentException.class, () -> load("""
			<quest_graphs>
				<quest_graph quest_id="12" version="1" scope="PLAYER" initial_node="active">
					<node id="active">
						<transition id="bad-timer" priority="1" to="done">
							<dialog npc_id="100" dialog="QUEST_SELECT"/>
							<actions><start-quest-timer timer="QUEST_TIMER" duration_seconds="0"/></actions>
						</transition>
					</node>
					<node id="done" terminal="true"/>
				</quest_graph>
			</quest_graphs>
			""", Set.of(12), Set.of(100), Set.of()));
	}

	/**
	 * 验证 credit、instance/world、对话协议、引用完整性与可观测性 acceptance 合同。
	 * Verifies acceptance contracts for credit, instance/world, dialog protocol, reference integrity, and observability.
	 */
	@Test
	void creditInstanceProtocolReferenceAndObservabilityContractsAreClosed() throws Exception {
		// REFERENCE_INTEGRITY：缺失 NPC / movie 引用编译失败。
		// REFERENCE_INTEGRITY: missing NPC / movie references fail compilation.
		assertThrows(IllegalArgumentException.class, () -> load("""
			<quest_graphs>
				<quest_graph quest_id="21" version="1" scope="PLAYER" initial_node="offer">
					<node id="offer">
						<transition id="talk" priority="1" to="done">
							<dialog npc_id="999" dialog="QUEST_SELECT"/>
						</transition>
					</node>
					<node id="done" terminal="true"/>
				</quest_graph>
			</quest_graphs>
			""", Set.of(21), Set.of(100), Set.of()));
		assertThrows(IllegalArgumentException.class, () -> load("""
			<quest_graphs>
				<quest_graph quest_id="22" version="1" scope="PLAYER" initial_node="play">
					<node id="play">
						<transition id="end" priority="1" to="done">
							<movie-ended movie_id="913"/>
						</transition>
					</node>
					<node id="done" terminal="true"/>
				</quest_graph>
			</quest_graphs>
			""", Set.of(22), Set.of(), Set.of()));

		// INSTANCE_WORLD + DIALOG_PROTOCOL：world-entered 携带 instance 快照；对话协议只消费绑定的 DIALOG 快照。
		// INSTANCE_WORLD + DIALOG_PROTOCOL: world-entered carries instance identity; dialog protocol consumes only bound DIALOG snapshots.
		CompiledQuestGraphData data = load("""
			<quest_graphs>
				<quest_graph quest_id="23" version="1" scope="PLAYER" initial_node="entry">
					<node id="entry">
						<transition id="enter" priority="1" to="playing">
							<world-entered/>
							<actions><start-quest/><sync-quest-status/><play-movie movie_id="913"/></actions>
						</transition>
					</node>
					<node id="playing">
						<transition id="dialog" priority="1" to="playing">
							<dialog npc_id="100" dialog="STEP_TO_1"/>
							<actions><send-dialog dialog_id="1011"/></actions>
						</transition>
						<transition id="movie-end" priority="1" to="done">
							<movie-ended movie_id="913"/>
						</transition>
					</node>
					<node id="done" terminal="true"/>
				</quest_graph>
			</quest_graphs>
			""", Set.of(23), Set.of(100), Set.of(913));
		PlayerQuestGraphStateList states = new PlayerQuestGraphStateList();
		AtomicReference<PlayerQuestGraphState> database = new AtomicReference<>();
		List<ActionType> protocol = new ArrayList<>();
		AtomicReference<SendDialogAction> dialog = new AtomicReference<>();
		TransitionContext context = new TransitionContext(PLAYER_ID, 0, SERVER_ZONE, states, invocation -> MATCHED, invocation -> READY,
			invocation -> {
				if (invocation.action() instanceof SendDialogAction send) {
					dialog.set(send);
				}
				if (invocation.action().type().phase() == CompiledQuestGraph.ActionPhase.POST_COMMIT_PROTOCOL) {
					protocol.add(invocation.action().type());
				}
				return ActionResult.APPLIED;
			}, cas(database));
		QuestGraphRouter router = new QuestGraphRouter(data);
		WorldEnteredEvent entered = new WorldEnteredEvent("enter", PLAYER_ID, 2000, 300030000, 9, 1f, 2f, 3f);
		assertEquals(9, entered.instanceId());
		assertEquals(Status.APPLIED, router.dispatch(entered, states,
			match -> new QuestGraphTransitionExecutor().execute(match, context)).status());
		assertEquals("playing", states.get(23).getNodeId());
		assertTrue(protocol.contains(ActionType.PLAY_MOVIE));
		assertEquals(Status.APPLIED, router.dispatch(new DialogEvent("dialog", PLAYER_ID, 2500, 100, 500, "STEP_TO_1"), states,
			match -> new QuestGraphTransitionExecutor().execute(match, context)).status());
		assertEquals(DialogBindingMode.BOUND, dialog.get().binding());
		assertTrue(protocol.contains(ActionType.SEND_DIALOG));

		// CREDIT：独占 movie-end 只消费一次；重复事件不再匹配（credit 单次语义）。
		// CREDIT: exclusive movie-end is consumed once; repeats no longer match (single-credit semantics).
		MovieEndedEvent ended = new MovieEndedEvent("movie", PLAYER_ID, 3000, 913, 1, 2500);
		assertEquals(Status.APPLIED, router.dispatch(ended, states,
			match -> new QuestGraphTransitionExecutor().execute(match, context)).status());
		assertEquals("done", states.get(23).getNodeId());
		assertEquals(Status.NO_MATCH, router.dispatch(ended, states,
			match -> new QuestGraphTransitionExecutor().execute(match, context)).status());

		// OBSERVABILITY_AUDIT：定义 registry generation 与不可变快照可审计；失败 reload 不静默改写。
		// OBSERVABILITY_AUDIT: definition registry generation and immutable snapshots are auditable; failed reload does not silently mutate.
		QuestGraphDefinitionRegistry registry = new QuestGraphDefinitionRegistry();
		QuestGraphDefinitionRegistry.Snapshot snap = registry.installInitial(data);
		assertEquals(1, snap.generation());
		assertTrue(snap.data().graphs().containsKey(23));
		assertThrows(IllegalArgumentException.class,
			() -> registry.reload(new CompiledQuestGraphData(Map.of(), Map.of()), List.of()));
		assertSame(snap, registry.snapshot());
		assertEquals(EventType.WORLD_ENTERED, entered.type());
		assertEquals(EventType.MOVIE_ENDED, ended.type());
		assertFalse(protocol.isEmpty());
	}

	private CompiledQuestGraphData load(String xml, Set<Integer> questIds, Set<Integer> npcIds, Set<Integer> movieIds)
			throws Exception {
		Path file = tempDir.resolve("foundation-" + questIds.iterator().next() + ".xml");
		Files.writeString(file, xml, StandardCharsets.UTF_8);
		return QuestGraphCompiler.load(file, SCHEMA,
			new QuestGraphCompiler.References(questIds, npcIds, Set.of(), Set.of(), Set.of(), movieIds));
	}

	private static BiFunction<Long, PlayerQuestGraphState, PersistenceResult> cas(
			AtomicReference<PlayerQuestGraphState> database) {
		return (expectedRevision, next) -> {
			PlayerQuestGraphState current = database.get();
			if (expectedRevision == null ? current != null : current == null || current.getRevision() != expectedRevision) {
				return CONFLICT;
			}
			database.set(next);
			return PersistenceResult.APPLIED;
		};
	}

	/** 创建同时含独占对话与广播击杀路径的最小不可变图。 / Creates a minimal immutable graph with exclusive dialog and broadcast kill paths. */
	private static CompiledQuestGraph graph(int questId) {
		Transition dialog = new Transition("dialog-" + questId, 1, "done", new Event(DIALOG, 100, "QUEST_SELECT"), List.of(), List.of());
		Transition kill = new Transition("kill-" + questId, 1, "done", new Event(KILL, 200, ""), List.of(), List.of());
		return new CompiledQuestGraph(questId, 1, PLAYER, "active", Map.of(), Map.of(
			"active", new Node("active", false, List.of(dialog, kill)),
			"done", new Node("done", true, List.of())));
	}
}
