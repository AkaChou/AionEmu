package com.aionemu.gameserver.questEngine.graph.runtime;

import static com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.ActionType.CLOSE_DIALOG;
import static com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.ActionType.SYNC_QUEST_STATUS;
import static com.aionemu.gameserver.questEngine.graph.runtime.DispatchResult.Propagation.CONTINUE;
import static com.aionemu.gameserver.questEngine.graph.runtime.DispatchResult.Propagation.STOP;
import static com.aionemu.gameserver.questEngine.graph.runtime.DispatchResult.Status.APPLIED;
import static com.aionemu.gameserver.questEngine.graph.runtime.DispatchResult.Status.FAILED;
import static com.aionemu.gameserver.questEngine.graph.runtime.DispatchResult.Status.NO_MATCH;
import static com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ConditionResult.MATCHED;
import static com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PersistenceResult.CONFLICT;
import static com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PreflightResult.READY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.MoviePlaybackAuthority;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.ActionType;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EscortCoordinatesDestination;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EscortSource;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EventType;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatus;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.StartEscortAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraphData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphCompiler;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEscortActionAdapter.CleanupReason;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.DialogEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.MovieEndedEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.QuestTimerEndedEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.RankedPlayerKillEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.WorldEnteredEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphLifecycleActionAdapter.LifecycleCommand;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphLifecycleActionAdapter.SettlementCommand;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphNpcSignalBridge.NpcSnapshot;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphNpcSignalBridge.PlayerSnapshot;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphPvpSignalBridge.ParticipantSnapshot;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionInvocation;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PersistenceResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.TransitionContext;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.CleanupLease;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.EscortResourceIdentity;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.IntValue;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.Lifecycle;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.QuestHistory;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphStateList;

class QuestGraphVerticalFixtureAcceptanceTest {

	private static final Path SCHEMA = Path.of("src/main/resources/aion/data/static_data/quest_graph_data/quest_graph_data.xsd");
	private static final ZoneId SERVER_ZONE = ZoneId.of("Asia/Shanghai");
	private static final int PLAYER_ID = 7;

	@TempDir
	Path tempDir;

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	private @interface VerticalFixtureEvidence {
		String id();

		EvidenceStatus status();

		EvidenceDimension[] proves();

		String[] blockers() default {};
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	private @interface AcceptanceEvidence {
		EvidenceStatus status();

		String scope();

		EvidenceDimension[] proves();

		String[] blockers() default {};
	}

	private enum EvidenceStatus {
		IMPLEMENTED,
		BLOCKED
	}

	private enum EvidenceDimension {
		POSITIVE,
		NEGATIVE,
		RECOVERY,
		CREDIT,
		CLEANUP,
		PROTOCOL,
		OWNERSHIP
	}

	private static final Map<String, FixtureContract> FIXTURE_CONTRACTS = Map.of(
		"SIMPLE_DIALOG", new FixtureContract(EvidenceStatus.IMPLEMENTED,
			Set.of(EvidenceDimension.POSITIVE, EvidenceDimension.NEGATIVE, EvidenceDimension.PROTOCOL, EvidenceDimension.OWNERSHIP), Set.of()),
		"COMPLEX_MULTIVARIABLE", new FixtureContract(EvidenceStatus.IMPLEMENTED,
			Set.of(EvidenceDimension.POSITIVE, EvidenceDimension.NEGATIVE, EvidenceDimension.RECOVERY, EvidenceDimension.OWNERSHIP), Set.of()),
		"TIMED_FAILURE", new FixtureContract(EvidenceStatus.IMPLEMENTED,
			Set.of(EvidenceDimension.POSITIVE, EvidenceDimension.NEGATIVE, EvidenceDimension.RECOVERY, EvidenceDimension.CLEANUP,
				EvidenceDimension.PROTOCOL, EvidenceDimension.OWNERSHIP), Set.of()),
		"MOVIE_INSTANCE", new FixtureContract(EvidenceStatus.IMPLEMENTED,
			Set.of(EvidenceDimension.POSITIVE, EvidenceDimension.NEGATIVE, EvidenceDimension.RECOVERY, EvidenceDimension.PROTOCOL,
				EvidenceDimension.OWNERSHIP),
			Set.of()),
		"ESCORT_AI", new FixtureContract(EvidenceStatus.IMPLEMENTED,
			Set.of(EvidenceDimension.POSITIVE, EvidenceDimension.NEGATIVE, EvidenceDimension.CLEANUP, EvidenceDimension.OWNERSHIP), Set.of()),
		"PVP_CREDIT", new FixtureContract(EvidenceStatus.IMPLEMENTED,
			Set.of(EvidenceDimension.POSITIVE, EvidenceDimension.NEGATIVE, EvidenceDimension.CREDIT, EvidenceDimension.OWNERSHIP), Set.of()));
	private static final AcceptanceContract ACCEPTANCE_CONTRACT = new AcceptanceContract(EvidenceStatus.IMPLEMENTED,
		"ALL_CURRENT_MECHANISMS", Set.of(EvidenceDimension.values()),
		Set.of());

	/** 保存一个不依赖运行时反射的垂直 fixture 合同。 / Holds one vertical-fixture contract without runtime reflection. */
	private record FixtureContract(EvidenceStatus status, Set<EvidenceDimension> proves, Set<String> blockers) {
	}

	/** 保存综合 acceptance 的显式合同。 / Holds the explicit aggregate-acceptance contract. */
	private record AcceptanceContract(EvidenceStatus status, String scope, Set<EvidenceDimension> proves, Set<String> blockers) {
	}

	@VerticalFixtureEvidence(id = "SIMPLE_DIALOG", status = EvidenceStatus.IMPLEMENTED,
		proves = { EvidenceDimension.POSITIVE, EvidenceDimension.NEGATIVE, EvidenceDimension.PROTOCOL, EvidenceDimension.OWNERSHIP })
	@Test
	void simpleDialogCompilesExecutesAndFailsClosed() throws Exception {
		CompiledQuestGraphData data = compile("simple-dialog", 1, Set.of(100), Set.of(), """
			<quest_graph quest_id="1" version="1" scope="PLAYER" initial_node="offer">
				<node id="offer">
					<transition id="accept" priority="1" to="done">
						<dialog npc_id="100" dialog="QUEST_SELECT"/>
						<actions><start-quest/><sync-quest-status/><close-dialog/></actions>
					</transition>
				</node>
				<node id="done" terminal="true"/>
			</quest_graph>
			""");
		PlayerQuestGraphStateList states = new PlayerQuestGraphStateList();
		AtomicReference<PlayerQuestGraphState> database = new AtomicReference<>();
		List<ActionType> protocol = new ArrayList<>();
		TransitionContext context = context(PLAYER_ID, states, database, invocation -> {
			if (invocation.action().type().phase() == CompiledQuestGraph.ActionPhase.POST_COMMIT_PROTOCOL) {
				protocol.add(invocation.action().type());
			}
			return ActionResult.APPLIED;
		});
		QuestGraphRouter router = new QuestGraphRouter(data);

		assertEquals(new DispatchResult(APPLIED, STOP), router.dispatch(
			new DialogEvent("simple-positive", PLAYER_ID, 1000, 100, "QUEST_SELECT"), states,
			match -> new QuestGraphTransitionExecutor().execute(match, context)));
		assertEquals("done", states.get(1).getNodeId());
		assertEquals(QuestStatus.START, states.get(1).getQuestStatus());
		assertEquals(List.of(SYNC_QUEST_STATUS, CLOSE_DIALOG), protocol);

		assertEquals(new DispatchResult(NO_MATCH, CONTINUE), new QuestGraphRouter(data).dispatch(
			new DialogEvent("simple-wrong-dialog", PLAYER_ID, 1001, 100, "QUEST_ACCEPT"), new PlayerQuestGraphStateList(),
			match -> APPLIED));
		PlayerQuestGraphStateList foreignStates = new PlayerQuestGraphStateList();
		AtomicReference<PlayerQuestGraphState> foreignDatabase = new AtomicReference<>();
		assertEquals(new DispatchResult(FAILED, STOP), new QuestGraphRouter(data).dispatch(
			new DialogEvent("simple-wrong-owner", PLAYER_ID + 1, 1002, 100, "QUEST_SELECT"), foreignStates,
			match -> new QuestGraphTransitionExecutor().execute(match,
				context(PLAYER_ID, foreignStates, foreignDatabase, invocation -> ActionResult.APPLIED))));
		assertNull(foreignStates.get(1));
		assertThrows(IllegalArgumentException.class,
			() -> compile("simple-missing-reference", 1, Set.of(), Set.of(), """
				<quest_graph quest_id="1" version="1" scope="PLAYER" initial_node="offer">
					<node id="offer"><transition id="accept" priority="1" to="done">
						<dialog npc_id="100" dialog="QUEST_SELECT"/>
					</transition></node>
					<node id="done" terminal="true"/>
				</quest_graph>
				"""));
	}

	@VerticalFixtureEvidence(id = "COMPLEX_MULTIVARIABLE", status = EvidenceStatus.IMPLEMENTED,
		proves = { EvidenceDimension.POSITIVE, EvidenceDimension.NEGATIVE, EvidenceDimension.RECOVERY, EvidenceDimension.OWNERSHIP })
	@Test
	void complexMultiVariableJoinsAndRecovers() throws Exception {
		CompiledQuestGraphData data = compile("complex-multivariable", 2, Set.of(100), Set.of(), """
			<quest_graph quest_id="2" version="1" scope="PLAYER" initial_node="collecting">
				<variables>
					<variable name="left" type="INT" scope="PLAYER" initial="0" min="0" max="1"/>
					<variable name="right" type="INT" scope="PLAYER" initial="0" min="0" max="1"/>
				</variables>
				<node id="collecting">
					<transition id="left" priority="1" to="joining">
						<dialog npc_id="100" dialog="LEFT"/>
						<actions><start-quest/><set-quest-variable variable="left" value="1"/></actions>
					</transition>
				</node>
				<node id="joining">
					<transition id="right" priority="1" to="done">
						<dialog npc_id="100" dialog="RIGHT"/>
						<conditions>
							<quest-variable variable="left" op="EQUAL" value="1"/>
							<quest-variable variable="right" op="EQUAL" value="0"/>
						</conditions>
						<actions>
							<set-quest-variable variable="right" value="1"/>
							<start-quest-timer timer="JOIN_TIMER" duration_seconds="30"/>
						</actions>
					</transition>
				</node>
				<node id="done" terminal="true"/>
			</quest_graph>
			""");
		PlayerQuestGraphStateList states = new PlayerQuestGraphStateList();
		AtomicReference<PlayerQuestGraphState> database = new AtomicReference<>();
		AtomicInteger timerAttempts = new AtomicInteger();
		List<String> keys = new ArrayList<>();
		Function<ActionInvocation, ActionResult> actions = invocation -> {
			if (invocation.action().type() == ActionType.START_QUEST_TIMER) {
				keys.add(invocation.idempotencyKey());
				return timerAttempts.getAndIncrement() == 0 ? ActionResult.FAILED : ActionResult.APPLIED;
			}
			return ActionResult.APPLIED;
		};
		TransitionContext context = context(PLAYER_ID, states, database, actions);
		QuestGraphRouter router = new QuestGraphRouter(data);

		assertEquals(new DispatchResult(APPLIED, STOP), router.dispatch(
			new DialogEvent("multi-left", PLAYER_ID, 1000, 100, "LEFT"), states,
			match -> new QuestGraphTransitionExecutor().execute(match, context)));
		assertEquals("joining", states.get(2).getNodeId());
		assertEquals(new DispatchResult(FAILED, STOP), router.dispatch(
			new DialogEvent("multi-right", PLAYER_ID, 2000, 100, "RIGHT"), states,
			match -> new QuestGraphTransitionExecutor().execute(match, context)));
		assertEquals(Lifecycle.PREPARED, states.get(2).getLifecycle());
		assertEquals("joining", states.get(2).getNodeId());

		assertEquals(APPLIED, new QuestGraphTransitionExecutor().recover(data.graphs().get(2), context));
		PlayerQuestGraphState recovered = states.get(2);
		assertEquals(Lifecycle.ACTIVE, recovered.getLifecycle());
		assertEquals("done", recovered.getNodeId());
		assertEquals(new IntValue(1), recovered.getVariables().get("left"));
		assertEquals(new IntValue(1), recovered.getVariables().get("right"));
		assertEquals(32_000L, recovered.getDeadlines().get("JOIN_TIMER"));
		assertEquals(List.of(keys.getFirst(), keys.getFirst()), keys);

		PlayerQuestGraphState corrupt = new PlayerQuestGraphState(2, 1, 0, "joining", QuestStatus.START,
			QuestHistory.EMPTY, null, Lifecycle.ACTIVE, Map.of("left", new IntValue(1)), Map.of(), null, Map.of(), null);
		PlayerQuestGraphStateList corruptStates = new PlayerQuestGraphStateList();
		corruptStates.addLoaded(corrupt);
		AtomicInteger corruptCallbacks = new AtomicInteger();
		TransitionContext corruptContext = new TransitionContext(PLAYER_ID, 0, SERVER_ZONE, corruptStates, invocation -> {
			corruptCallbacks.incrementAndGet();
			return MATCHED;
		}, invocation -> READY, invocation -> ActionResult.APPLIED,
			strictCas(new AtomicReference<>(corrupt)));
		assertEquals(new DispatchResult(FAILED, STOP), router.dispatch(
			new DialogEvent("multi-corrupt", PLAYER_ID, 3000, 100, "RIGHT"), corruptStates,
			match -> new QuestGraphTransitionExecutor().execute(match, corruptContext)));
		assertEquals(0, corruptCallbacks.get());
	}

	@VerticalFixtureEvidence(id = "TIMED_FAILURE", status = EvidenceStatus.IMPLEMENTED,
		proves = { EvidenceDimension.POSITIVE, EvidenceDimension.NEGATIVE, EvidenceDimension.RECOVERY,
			EvidenceDimension.CLEANUP, EvidenceDimension.PROTOCOL, EvidenceDimension.OWNERSHIP })
	@Test
	void timerExpiryFailureCleansAndRecovers() throws Exception {
		CompiledQuestGraphData data = compile("timed-failure", 3, Set.of(), Set.of(), """
			<quest_graph quest_id="3" version="1" scope="PLAYER" initial_node="timed">
				<node id="timed">
					<transition id="timeout" priority="1" to="failed">
						<quest-timer-ended timer="QUEST_TIMER"/>
						<actions><end-quest-timer timer="QUEST_TIMER"/></actions>
					</transition>
				</node>
				<node id="failed" terminal="true"/>
			</quest_graph>
			""");
		PlayerQuestGraphState initial = new PlayerQuestGraphState(3, 1, 0, "timed", QuestStatus.START,
			QuestHistory.EMPTY, null, Lifecycle.ACTIVE, Map.of(), Map.of("QUEST_TIMER", 1000L), null,
			Map.of("timer", new CleanupLease("QUEST_TIMER", "QUEST_TIMER")), null);
		PlayerQuestGraphStateList states = new PlayerQuestGraphStateList();
		states.addLoaded(initial);
		AtomicReference<PlayerQuestGraphState> database = new AtomicReference<>(initial);
		AtomicInteger endAttempts = new AtomicInteger();
		AtomicInteger protocolCalls = new AtomicInteger();
		List<String> keys = new ArrayList<>();
		Function<ActionInvocation, ActionResult> actions = invocation -> {
			if (invocation.action().type() == ActionType.END_QUEST_TIMER) {
				keys.add(invocation.idempotencyKey());
				return endAttempts.getAndIncrement() == 0 ? ActionResult.FAILED : ActionResult.APPLIED;
			}
			if (invocation.action().type() == ActionType.SYNC_QUEST_TIMER) {
				assertEquals("failed", states.get(3).getNodeId());
				assertEquals(Lifecycle.PREPARED, states.get(3).getLifecycle());
				assertTrue(states.get(3).getJournal().isTargetCommitted());
				protocolCalls.incrementAndGet();
			}
			return ActionResult.APPLIED;
		};
		TransitionContext context = context(PLAYER_ID, states, database, actions);
		QuestGraphRouter router = new QuestGraphRouter(data);
		QuestTimerEndedEvent event = new QuestTimerEndedEvent("timer-expired", PLAYER_ID, 2000, 3, "QUEST_TIMER", 1000);

		assertEquals(new DispatchResult(FAILED, CONTINUE), router.dispatch(event, states,
			match -> new QuestGraphTransitionExecutor().execute(match, context)));
		assertEquals(Lifecycle.PREPARED, states.get(3).getLifecycle());
		assertEquals(1000L, states.get(3).getDeadlines().get("QUEST_TIMER"));
		assertEquals(APPLIED, new QuestGraphTransitionExecutor().recover(data.graphs().get(3), context));
		assertEquals(Map.of(), states.get(3).getDeadlines());
		assertEquals("failed", states.get(3).getNodeId());
		assertEquals(1, protocolCalls.get());
		assertEquals(List.of(keys.getFirst(), keys.getFirst()), keys);
		assertEquals(new DispatchResult(NO_MATCH, CONTINUE), new QuestGraphRouter(data).dispatch(
			new QuestTimerEndedEvent("wrong-timer", PLAYER_ID, 2001, 3, "OTHER_TIMER", 1000),
			new PlayerQuestGraphStateList(), match -> APPLIED));
	}

	/**
	 * 验证 instance 世界进入触发影片协议，movie-end 恢复/一次性凭据与错误 owner 失败关闭。
	 * Verifies instance world-entry triggers movie protocol, movie-end recovery/one-shot credentials, and wrong-owner fail-closed.
	 */
	@VerticalFixtureEvidence(id = "MOVIE_INSTANCE", status = EvidenceStatus.IMPLEMENTED,
		proves = { EvidenceDimension.POSITIVE, EvidenceDimension.NEGATIVE, EvidenceDimension.RECOVERY,
			EvidenceDimension.PROTOCOL, EvidenceDimension.OWNERSHIP })
	@Test
	void movieInstanceWorldEntryPlaysAndRecovers() throws Exception {
		CompiledQuestGraphData data = compile("movie-instance", 4, Set.of(), Set.of(913), """
			<quest_graph quest_id="4" version="1" scope="PLAYER" initial_node="entry">
				<node id="entry">
					<transition id="enter-instance" priority="1" to="playing">
						<world-entered/>
						<actions><start-quest/><sync-quest-status/><play-movie movie_id="913"/></actions>
					</transition>
				</node>
				<node id="playing">
					<transition id="movie-finished" priority="1" to="done">
						<movie-ended movie_id="913"/>
					</transition>
				</node>
				<node id="done" terminal="true"/>
			</quest_graph>
			""");
		// POSITIVE：进入带 instanceId 的世界后投影 play-movie，不借用 crafting_rewards 证据。
		// POSITIVE: entering a world with instanceId projects play-movie without borrowing crafting_rewards evidence.
		PlayerQuestGraphStateList states = new PlayerQuestGraphStateList();
		AtomicReference<PlayerQuestGraphState> database = new AtomicReference<>();
		List<ActionType> protocol = new ArrayList<>();
		TransitionContext context = context(PLAYER_ID, states, database, invocation -> {
			if (invocation.action().type().phase() == CompiledQuestGraph.ActionPhase.POST_COMMIT_PROTOCOL) {
				protocol.add(invocation.action().type());
			}
			return ActionResult.APPLIED;
		});
		QuestGraphRouter router = new QuestGraphRouter(data);
		int instanceId = 7;
		WorldEnteredEvent entered = new WorldEnteredEvent("instance-enter", PLAYER_ID, 1000, 300030000, instanceId, 1f, 2f, 3f);
		assertEquals(instanceId, entered.instanceId());
		assertEquals(new DispatchResult(APPLIED, CONTINUE), router.dispatch(entered, states,
			match -> new QuestGraphTransitionExecutor().execute(match, context)));
		assertEquals("playing", states.get(4).getNodeId());
		assertEquals(QuestStatus.START, states.get(4).getQuestStatus());
		assertEquals(List.of(SYNC_QUEST_STATUS, ActionType.PLAY_MOVIE), protocol);

		// RECOVERY + PROTOCOL：服务端 playback 凭据一次性完成，codec 往返保持 playbackId。
		// RECOVERY + PROTOCOL: server playback credentials complete once; codec round-trip keeps playbackId.
		MoviePlaybackAuthority authority = new MoviePlaybackAuthority();
		MoviePlaybackAuthority.Playback playback = authority.begin(913, 2000);
		assertTrue(authority.complete(914, 2001).isEmpty());
		MovieEndedEvent ended = QuestGraphMovieSignalBridge.fromPlayback(PLAYER_ID, 2002,
			authority.complete(913, 2002).orElseThrow());
		MovieEndedEvent recoveredEvent = (MovieEndedEvent) QuestGraphEventCodec.decode(QuestGraphEventCodec.encode(ended));
		assertEquals(playback.playbackId(), recoveredEvent.playbackId());
		assertEquals(ended, recoveredEvent);
		assertEquals(new DispatchResult(APPLIED, STOP), router.dispatch(recoveredEvent, states,
			match -> new QuestGraphTransitionExecutor().execute(match, context)));
		assertEquals("done", states.get(4).getNodeId());
		assertTrue(authority.complete(913, 2003).isEmpty());

		// NEGATIVE：错误影片、重复 movie-end、错误 owner。
		// NEGATIVE: wrong movie, repeated movie-end, and wrong owner.
		assertEquals(new DispatchResult(NO_MATCH, CONTINUE), new QuestGraphRouter(data).dispatch(
			new MovieEndedEvent("wrong-movie", PLAYER_ID, 3000, 914, 1, 2500), new PlayerQuestGraphStateList(),
			match -> APPLIED));
		assertEquals(new DispatchResult(NO_MATCH, CONTINUE), router.dispatch(recoveredEvent, states,
			match -> new QuestGraphTransitionExecutor().execute(match, context)));
		PlayerQuestGraphStateList foreignStates = new PlayerQuestGraphStateList();
		AtomicReference<PlayerQuestGraphState> foreignDatabase = new AtomicReference<>();
		assertEquals(new DispatchResult(FAILED, CONTINUE), new QuestGraphRouter(data).dispatch(
			new WorldEnteredEvent("wrong-owner", PLAYER_ID + 1, 4000, 300030000, instanceId, 1f, 2f, 3f), foreignStates,
			match -> new QuestGraphTransitionExecutor().execute(match,
				context(PLAYER_ID, foreignStates, foreignDatabase, invocation -> ActionResult.APPLIED))));
		assertNull(foreignStates.get(4));
		// OWNERSHIP：instance-scoped spawn 与 world-entered instance 快照并存；不把 crafting 当 instance 证据。
		// OWNERSHIP: instance-scoped spawn coexists with world-entered instance snapshots; crafting is not instance evidence.
		assertTrue(Arrays.stream(ActionType.values()).anyMatch(value -> value == ActionType.SPAWN_INSTANCE_NPC));
		assertTrue(Arrays.stream(EventType.values()).anyMatch(value -> value == EventType.WORLD_ENTERED));
		assertTrue(Arrays.stream(EventType.values()).anyMatch(value -> value == EventType.MOVIE_ENDED));
	}

	@VerticalFixtureEvidence(id = "ESCORT_AI", status = EvidenceStatus.IMPLEMENTED,
		proves = { EvidenceDimension.POSITIVE, EvidenceDimension.NEGATIVE, EvidenceDimension.CLEANUP, EvidenceDimension.OWNERSHIP })
	@Test
	void escortAiLifecycleCleansOnEveryExit() throws Exception {
		CompiledQuestGraphData data = compile("escort-ai", 5, Set.of(), Set.of(), """
			<quest_graph quest_id="5" version="1" scope="PLAYER" initial_node="escorting">
				<node id="escorting">
					<transition id="reached" priority="1" to="done"><escort-reached-target/><actions><finish-quest reward_index="0"/></actions></transition>
					<transition id="lost" priority="1" to="done"><escort-lost-target/><actions><finish-quest reward_index="0"/></actions></transition>
				</node>
				<node id="done" terminal="true"/>
			</quest_graph>
			""");
		PlayerSnapshot player = new PlayerSnapshot(PLAYER_ID, 210010000, 1, 0, 0, 0);
		NpcSnapshot npc = new NpcSnapshot(100, 5001, 210010000, 1, 3, 4, 0);
		List<QuestGraphEvent> exits = List.of(
			QuestGraphNpcSignalBridge.escortReached("escort-reached", 1000, 5, player, npc),
			QuestGraphNpcSignalBridge.escortLost("escort-lost", 1001, 5, player, npc));
		StartEscortAction escortAction = new StartEscortAction(EscortSource.EVENT_NPC, 0, (byte) 0, "4212", true, false,
			true, false, new EscortCoordinatesDestination(505.69427f, 437.69382f, 885.1844f));

		for (QuestGraphEvent event : exits) {
			String resourceKey = "escort:" + event.eventId();
			CleanupLease persistedEscort = CleanupLease.escort(new EscortResourceIdentity(PLAYER_ID, 5, npc.npcObjectId(), npc.npcId(),
				npc.worldId(), npc.instanceId(), npc.x(), npc.y(), npc.z(), npc.npcId(), npc.npcObjectId(), false, null, escortAction,
				resourceKey));
			Map<String, CleanupLease> lease = Map.of(resourceKey, persistedEscort);
			PlayerQuestGraphState initial = new PlayerQuestGraphState(5, 1, 0, "escorting", QuestStatus.REWARD,
				QuestHistory.EMPTY, null, Lifecycle.ACTIVE, Map.of(), Map.of(), null, lease, null);
			PlayerQuestGraphStateList states = new PlayerQuestGraphStateList();
			states.addLoaded(initial);
			AtomicReference<PlayerQuestGraphState> database = new AtomicReference<>(initial);
			AtomicReference<LifecycleCommand> command = new AtomicReference<>();
			AtomicReference<CleanupReason> cleanupReason = new AtomicReference<>();
			QuestGraphLifecycleActionAdapter lifecycle = new QuestGraphLifecycleActionAdapter(PLAYER_ID,
				value -> READY, value -> {
					command.set(value);
					return ActionResult.APPLIED;
				}, (cleanupLease, reason) -> {
					assertEquals(persistedEscort, cleanupLease);
					cleanupReason.set(reason);
					return ActionResult.APPLIED;
				});
			TransitionContext context = new TransitionContext(PLAYER_ID, 0, SERVER_ZONE, states, invocation -> MATCHED,
				lifecycle::preflight, lifecycle::execute, strictCas(database));

			assertEquals(new DispatchResult(APPLIED, CONTINUE), new QuestGraphRouter(data).dispatch(event, states,
				match -> new QuestGraphTransitionExecutor().execute(match, context)));
			SettlementCommand settlement = (SettlementCommand) command.get();
			assertEquals(lease, settlement.cleanupLeases());
			assertEquals(CleanupReason.FINISH, cleanupReason.get());
			assertEquals(Map.of(), states.get(5).getCleanupLeases());
			assertEquals(QuestStatus.COMPLETE, states.get(5).getQuestStatus());
		}

		assertEquals(new DispatchResult(NO_MATCH, CONTINUE), new QuestGraphRouter(data).dispatch(
			QuestGraphNpcSignalBridge.escortReached("wrong-owner", 1002, 6, player, npc),
			new PlayerQuestGraphStateList(), match -> APPLIED));
		assertThrows(IllegalArgumentException.class, () -> QuestGraphNpcSignalBridge.escortLost("cross-instance", 1003, 5,
			player, new NpcSnapshot(100, 5001, 210010000, 2, 3, 4, 0)));
	}

	@VerticalFixtureEvidence(id = "PVP_CREDIT", status = EvidenceStatus.IMPLEMENTED,
		proves = { EvidenceDimension.POSITIVE, EvidenceDimension.NEGATIVE, EvidenceDimension.CREDIT, EvidenceDimension.OWNERSHIP })
	@Test
	void pvpCreditRoutesEligibleRecipientsExactlyOnce() throws Exception {
		CompiledQuestGraphData data = compile("pvp-credit", 6, Set.of(), Set.of(), """
			<quest_graph quest_id="6" version="1" scope="PLAYER" initial_node="waiting">
				<node id="waiting">
					<transition id="rank-credit" priority="1" to="done"><ranked-player-kill minimum_rank="10"/></transition>
				</node>
				<node id="done" terminal="true"/>
			</quest_graph>
			""");
		ParticipantSnapshot killer = participant(7, Race.ELYOS, 0, true, 1);
		ParticipantSnapshot recipient = participant(8, Race.ELYOS, 3, true, 1);
		ParticipantSnapshot victim = participant(9, Race.ASMODIANS, 8, true, 1);
		RankedPlayerKillEvent event = QuestGraphPvpSignalBridge.rankedKill("rank-credit", 1000,
			recipient, killer, victim, 12, 100);
		PlayerQuestGraphStateList states = new PlayerQuestGraphStateList();
		AtomicReference<PlayerQuestGraphState> database = new AtomicReference<>();
		AtomicInteger executions = new AtomicInteger();
		TransitionContext context = context(8, states, database, invocation -> ActionResult.APPLIED);
		QuestGraphRouter router = new QuestGraphRouter(data);

		assertEquals(8, event.playerId());
		assertEquals(7, event.killerPlayerId());
		assertEquals(9, event.victimPlayerId());
		assertEquals(new DispatchResult(APPLIED, CONTINUE), router.dispatch(event, states, match -> {
			executions.incrementAndGet();
			return new QuestGraphTransitionExecutor().execute(match, context);
		}));
		assertEquals(new DispatchResult(NO_MATCH, CONTINUE), router.dispatch(event, states, match -> {
			executions.incrementAndGet();
			return new QuestGraphTransitionExecutor().execute(match, context);
		}));
		assertEquals(1, executions.get());

		assertThrows(IllegalArgumentException.class, () -> QuestGraphPvpSignalBridge.rankedKill("dead", 1001,
			participant(8, Race.ELYOS, 3, false, 1), killer, victim, 12, 100));
		assertThrows(IllegalArgumentException.class, () -> QuestGraphPvpSignalBridge.rankedKill("distance", 1002,
			recipient, killer, victim, 12, 5));
		assertThrows(IllegalArgumentException.class, () -> QuestGraphPvpSignalBridge.rankedKill("instance", 1003,
			participant(8, Race.ELYOS, 3, true, 2), killer, victim, 12, 100));
	}

	@AcceptanceEvidence(status = EvidenceStatus.IMPLEMENTED, scope = "ALL_CURRENT_MECHANISMS",
		proves = { EvidenceDimension.RECOVERY, EvidenceDimension.CREDIT, EvidenceDimension.CLEANUP,
			EvidenceDimension.PROTOCOL, EvidenceDimension.OWNERSHIP, EvidenceDimension.POSITIVE, EvidenceDimension.NEGATIVE })
	@Test
	void fixtureEvidenceContractsAreCompleteAndFailClosed() {
		assertEquals(Set.of("SIMPLE_DIALOG", "COMPLEX_MULTIVARIABLE", "TIMED_FAILURE", "MOVIE_INSTANCE", "ESCORT_AI", "PVP_CREDIT"),
			FIXTURE_CONTRACTS.keySet());
		assertEquals(EvidenceStatus.IMPLEMENTED, FIXTURE_CONTRACTS.get("MOVIE_INSTANCE").status());
		assertEquals(Set.of(), FIXTURE_CONTRACTS.get("MOVIE_INSTANCE").blockers());
		assertTrue(FIXTURE_CONTRACTS.values().stream()
			.allMatch(contract -> contract.status() == EvidenceStatus.IMPLEMENTED && contract.blockers().isEmpty()));

		EnumSet<EvidenceDimension> covered = FIXTURE_CONTRACTS.values().stream()
			.filter(contract -> contract.status() == EvidenceStatus.IMPLEMENTED)
			.map(FixtureContract::proves)
			.collect(() -> EnumSet.noneOf(EvidenceDimension.class), EnumSet::addAll, EnumSet::addAll);
		assertEquals(EnumSet.allOf(EvidenceDimension.class), covered);
		assertEquals(EvidenceStatus.IMPLEMENTED, ACCEPTANCE_CONTRACT.status());
		assertEquals("ALL_CURRENT_MECHANISMS", ACCEPTANCE_CONTRACT.scope());
		assertEquals(Set.of(EvidenceDimension.values()), ACCEPTANCE_CONTRACT.proves());
		assertEquals(Set.of(), ACCEPTANCE_CONTRACT.blockers());
	}

	private CompiledQuestGraphData compile(String name, int questId, Set<Integer> npcIds, Set<Integer> movieIds,
			String graph) throws Exception {
		Path xml = tempDir.resolve(name + ".xml");
		Files.writeString(xml, "<quest_graphs>" + graph + "</quest_graphs>", StandardCharsets.UTF_8);
		return QuestGraphCompiler.load(xml, SCHEMA,
			new QuestGraphCompiler.References(Set.of(questId), npcIds, Set.of(), Set.of(), Set.of(), movieIds));
	}

	private static TransitionContext context(int playerId, PlayerQuestGraphStateList states,
			AtomicReference<PlayerQuestGraphState> database, Function<ActionInvocation, ActionResult> actions) {
		return new TransitionContext(playerId, 0, SERVER_ZONE, states, invocation -> MATCHED, invocation -> READY,
			actions, strictCas(database));
	}

	private static BiFunction<Long, PlayerQuestGraphState, PersistenceResult> strictCas(
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

	private static ParticipantSnapshot participant(int playerId, Race race, float x, boolean alive, int instanceId) {
		return new ParticipantSnapshot(playerId, race, 400010000, instanceId, x, 0, 0, alive);
	}
}
