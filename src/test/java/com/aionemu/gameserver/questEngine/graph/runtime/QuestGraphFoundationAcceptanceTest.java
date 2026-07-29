package com.aionemu.gameserver.questEngine.graph.runtime;

import static com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EventType.DIALOG;
import static com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EventType.KILL;
import static com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.StateScope.PLAYER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Event;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Node;
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
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.Lifecycle;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.QuestHistory;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphStateList;

/**
 * 验证全部任务图共享的事件、路由、结果、安全、控制流和 owner 基础合同。
 * Verifies the event, routing, result, security, control-flow, and ownership contracts shared by every quest graph.
 */
class QuestGraphFoundationAcceptanceTest {

	private static final Path SCHEMA = Path.of("src/main/resources/aion/data/static_data/quest_graph_data/quest_graph_data.xsd");

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

		DispatchResult exclusive = router.dispatch(new DialogEvent("dialog", 7, 1000, 100, "QUEST_SELECT"), states, match -> {
			visited.add(match.route().questId());
			return match.route().questId() == 1 ? Status.NO_MATCH : Status.APPLIED;
		});
		assertEquals(List.of(1, 2), visited);
		assertEquals(new DispatchResult(Status.APPLIED, Propagation.STOP), exclusive);

		visited.clear();
		DispatchResult mismatch = router.dispatch(new DialogEvent("mismatch", 7, 1001, 100, "QUEST_ACCEPT"), states, match -> {
			visited.add(match.route().questId());
			return Status.APPLIED;
		});
		assertEquals(List.of(), visited);
		assertEquals(new DispatchResult(Status.NO_MATCH, Propagation.CONTINUE), mismatch);
		assertEquals(new DispatchResult(Status.REJECTED, Propagation.STOP),
			router.dispatch(new DialogEvent("rejected", 7, 1002, 100, "QUEST_SELECT"), states, match -> Status.REJECTED));

		visited.clear();
		DispatchResult broadcast = router.dispatch(new KillEvent("kill", 7, 1003, 200), states, match -> {
			visited.add(match.route().questId());
			if (match.route().questId() == 1) {
				throw new IllegalStateException("isolated owner failure");
			}
			return Status.APPLIED;
		});
		assertEquals(List.of(1, 2), visited);
		assertEquals(new DispatchResult(Status.FAILED, Propagation.CONTINUE), broadcast);

		PlayerQuestGraphStateList incompatible = new PlayerQuestGraphStateList();
		incompatible.addLoaded(new PlayerQuestGraphState(1, 2, 0, "active", CompiledQuestGraph.QuestStatus.START,
			QuestHistory.EMPTY, null, Lifecycle.ACTIVE, Map.of(), Map.of(), null, Map.of(), null));
		assertEquals(new DispatchResult(Status.FAILED, Propagation.STOP),
			router.dispatch(new DialogEvent("bad-state", 7, 1004, 100, "QUEST_SELECT"), incompatible, match -> Status.APPLIED));
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

	/** 创建同时含独占对话与广播击杀路径的最小不可变图。 / Creates a minimal immutable graph with exclusive dialog and broadcast kill paths. */
	private static CompiledQuestGraph graph(int questId) {
		Transition dialog = new Transition("dialog-" + questId, 1, "done", new Event(DIALOG, 100, "QUEST_SELECT"), List.of(), List.of());
		Transition kill = new Transition("kill-" + questId, 1, "done", new Event(KILL, 200, ""), List.of(), List.of());
		return new CompiledQuestGraph(questId, 1, PLAYER, "active", Map.of(), Map.of(
			"active", new Node("active", false, List.of(dialog, kill)),
			"done", new Node("done", true, List.of())));
	}
}
