package com.aionemu.gameserver.questEngine.graph.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraphData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphCompiler;
import com.aionemu.gameserver.questEngine.graph.runtime.DispatchResult.Propagation;
import com.aionemu.gameserver.questEngine.graph.runtime.DispatchResult.Status;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.DialogEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.KillEvent;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.Lifecycle;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.PreparedTransition;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphStateList;

class QuestGraphRouterTest {

	private static final Path SCHEMA = Path.of("src/main/resources/aion/data/static_data/quest_graph_data/quest_graph_data.xsd");

	@TempDir
	Path tempDir;
	private QuestGraphRouter router;

	@BeforeEach
	void loadGraphs() throws Exception {
		Path xml = tempDir.resolve("graphs.xml");
		Files.writeString(xml, document(), StandardCharsets.UTF_8);
		CompiledQuestGraphData data = QuestGraphCompiler.load(xml, SCHEMA,
			new QuestGraphCompiler.References(Set.of(1, 2), Set.of(100)));
		router = new QuestGraphRouter(data);
	}

	@Test
	void dialogUsesStablePriorityAndStopsOnFirstNonMatchResult() {
		List<Integer> visited = new ArrayList<>();
		DispatchResult result = router.dispatch(new DialogEvent("dialog-1", 7, 1000, 100, "QUEST_SELECT"),
			new PlayerQuestGraphStateList(), match -> {
				visited.add(match.route().questId());
				return match.route().questId() == 2 ? Status.NO_MATCH : Status.APPLIED;
			});

		assertEquals(List.of(2, 1), visited);
		assertEquals(new DispatchResult(Status.APPLIED, Propagation.STOP), result);
		visited.clear();
		result = router.dispatch(new DialogEvent("dialog-2", 7, 1001, 100, "QUEST_SELECT"),
			new PlayerQuestGraphStateList(), match -> {
				visited.add(match.route().questId());
				return Status.REJECTED;
			});
		assertEquals(List.of(2), visited);
		assertEquals(new DispatchResult(Status.REJECTED, Propagation.STOP), result);
	}

	@Test
	void dialogPayloadAndInvalidStateBlockEvaluation() {
		DispatchResult noMatch = router.dispatch(new DialogEvent("dialog-3", 7, 1002, 100, "QUEST_ACCEPT"),
			new PlayerQuestGraphStateList(), match -> {
				throw new AssertionError("Mismatched dialog must not reach evaluator");
			});
		assertEquals(new DispatchResult(Status.NO_MATCH, Propagation.CONTINUE), noMatch);

		List<PlayerQuestGraphState> blockedStates = List.of(
			new PlayerQuestGraphState(2, 2, 0, "start", null, Lifecycle.ACTIVE, Map.of(), Map.of(), null, Map.of(), null),
			new PlayerQuestGraphState(2, 1, 0, "missing", null, Lifecycle.ACTIVE, Map.of(), Map.of(), null, Map.of(), null),
			new PlayerQuestGraphState(2, 1, 0, "start", null, Lifecycle.PREPARED, Map.of(), Map.of(),
				new PreparedTransition(-1, "event", "dialog-q2", 0, new byte[0]), Map.of(), null),
			new PlayerQuestGraphState(2, 1, 0, "start", null, Lifecycle.QUARANTINED, Map.of(), Map.of(), null, Map.of(), "blocked"));
		for (PlayerQuestGraphState blockedState : blockedStates) {
			PlayerQuestGraphStateList states = new PlayerQuestGraphStateList();
			states.addLoaded(blockedState);
			DispatchResult failed = router.dispatch(new DialogEvent("dialog-4", 7, 1003, 100, "QUEST_SELECT"), states,
				match -> {
					throw new AssertionError("Blocked state must not reach evaluator");
				});
			assertEquals(new DispatchResult(Status.FAILED, Propagation.STOP), failed);
		}
	}

	@Test
	void killBroadcastIsolatesFailuresAndContinuesOtherQuests() {
		List<String> visited = new ArrayList<>();
		DispatchResult result = router.dispatch(new KillEvent("kill-1", 7, 2000, 100), new PlayerQuestGraphStateList(), match -> {
			visited.add(match.route().transition().id());
			if (match.route().questId() == 2) {
				throw new IllegalStateException("isolated failure");
			}
			return match.route().transition().id().equals("kill-first") ? Status.NO_MATCH : Status.APPLIED;
		});

		assertEquals(List.of("kill-q2", "kill-first", "kill-second"), visited);
		assertEquals(new DispatchResult(Status.FAILED, Propagation.CONTINUE), result);
	}

	@Test
	void killBroadcastStopsRemainingCandidatesForConcludedQuest() {
		List<String> visited = new ArrayList<>();
		DispatchResult result = router.dispatch(new KillEvent("kill-2", 7, 2001, 100), new PlayerQuestGraphStateList(), match -> {
			visited.add(match.route().transition().id());
			return Status.APPLIED;
		});

		assertEquals(List.of("kill-q2", "kill-first"), visited);
		assertEquals(new DispatchResult(Status.APPLIED, Propagation.CONTINUE), result);
	}

	@Test
	void eventCodecRoundTripsAndRejectsCorruption() {
		DialogEvent dialog = new DialogEvent("dialog", 7, 3000, 100, "QUEST_SELECT");
		KillEvent kill = new KillEvent("kill", 8, 3001, 100);
		assertEquals(dialog, QuestGraphEventCodec.decode(QuestGraphEventCodec.encode(dialog)));
		assertEquals(kill, QuestGraphEventCodec.decode(QuestGraphEventCodec.encode(kill)));

		byte[] encoded = QuestGraphEventCodec.encode(kill);
		byte[] unknown = Arrays.copyOf(encoded, encoded.length);
		unknown[4] = 99;
		assertThrows(IllegalArgumentException.class, () -> QuestGraphEventCodec.decode(unknown));
		assertThrows(IllegalArgumentException.class,
			() -> QuestGraphEventCodec.decode(Arrays.copyOf(encoded, encoded.length - 1)));
		assertThrows(IllegalArgumentException.class,
			() -> QuestGraphEventCodec.decode(Arrays.copyOf(encoded, encoded.length + 1)));
	}

	private static String document() {
		return """
			<quest_graphs>
				<quest_graph quest_id="1" version="1" scope="PLAYER" initial_node="start">
					<node id="start">
						%s
						%s
						%s
					</node>
					<node id="done" terminal="true"/>
				</quest_graph>
				<quest_graph quest_id="2" version="1" scope="PLAYER" initial_node="start">
					<node id="start">
						%s
						%s
					</node>
					<node id="done" terminal="true"/>
				</quest_graph>
			</quest_graphs>
			""".formatted(dialogTransition("dialog-q1", 20), killTransition("kill-first", 15), killTransition("kill-second", 25),
			dialogTransition("dialog-q2", 10), killTransition("kill-q2", 5));
	}

	private static String dialogTransition(String id, int priority) {
		return """
			<transition id="%s" priority="%d" to="done">
				<dialog npc_id="100" dialog="QUEST_SELECT"/>
			</transition>
			""".formatted(id, priority);
	}

	private static String killTransition(String id, int priority) {
		return """
			<transition id="%s" priority="%d" to="done">
				<kill npc_id="100"/>
			</transition>
			""".formatted(id, priority);
	}
}
