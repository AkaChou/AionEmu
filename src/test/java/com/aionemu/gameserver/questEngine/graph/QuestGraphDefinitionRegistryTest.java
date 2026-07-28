package com.aionemu.gameserver.questEngine.graph;

import static com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.StateScope.PLAYER;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.IntVariable;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Node;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.IntValue;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.Lifecycle;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.QuestHistory;

/**
 * 验证任务图定义启动安装、版本门禁、状态兼容和原子 reload。
 * Verifies quest-graph startup installation, version gates, state compatibility, and atomic reload.
 */
class QuestGraphDefinitionRegistryTest {

	/**
	 * 验证失败 reload 不替换快照，并拒绝同版本漂移、owner 删除和版本回退。
	 * Verifies failed reloads preserve the snapshot and reject same-version drift, owner removal, and rollback.
	 */
	@Test
	void reloadIsAtomicAndVersioned() {
		QuestGraphDefinitionRegistry registry = new QuestGraphDefinitionRegistry();
		CompiledQuestGraphData versionOne = data(graph(1, 5, "active"));
		assertEquals(1, registry.installInitial(versionOne).generation());
		assertEquals(1, registry.reload(versionOne, List.of()).generation());

		CompiledQuestGraphData drift = data(graph(1, 6, "active"));
		assertThrows(IllegalArgumentException.class, () -> registry.reload(drift, List.of()));
		assertEquals(versionOne, registry.snapshot().data());
		CompiledQuestGraphData indexDrift = new CompiledQuestGraphData(versionOne.graphs(),
			Map.of(new CompiledQuestGraphData.EventKey(CompiledQuestGraph.EventType.DIALOG, 1), List.of()));
		assertThrows(IllegalArgumentException.class, () -> registry.reload(indexDrift, List.of()));
		assertEquals(versionOne, registry.snapshot().data());
		assertThrows(IllegalArgumentException.class,
			() -> registry.reload(new CompiledQuestGraphData(Map.of(), Map.of()), List.of()));
		assertEquals(versionOne, registry.snapshot().data());

		CompiledQuestGraphData versionTwo = data(graph(2, 6, "renamed"));
		assertEquals(2, registry.reload(versionTwo, List.of()).generation());
		assertThrows(IllegalArgumentException.class, () -> registry.reload(versionOne, List.of()));
		assertEquals(versionTwo, registry.snapshot().data());
	}

	/**
	 * 验证任一未迁移状态都会阻断整批 reload，兼容状态才允许原子安装。
	 * Verifies any unmigrated state blocks the whole reload and compatible states permit atomic installation.
	 */
	@Test
	void reloadRequiresEveryPersistedStateToMatchCandidate() {
		QuestGraphDefinitionRegistry registry = new QuestGraphDefinitionRegistry();
		CompiledQuestGraphData versionOne = data(graph(1, 5, "active"));
		CompiledQuestGraphData versionTwo = data(graph(2, 5, "active"));
		registry.installInitial(versionOne);

		assertThrows(IllegalArgumentException.class, () -> registry.reload(versionTwo, List.of(state(1, "active", 2, Lifecycle.ACTIVE))));
		assertEquals(versionOne, registry.snapshot().data());
		assertEquals(2, registry.reload(versionTwo, List.of(state(2, "active", 2, Lifecycle.ACTIVE))).generation());
		assertEquals(versionTwo, registry.snapshot().data());
	}

	/**
	 * 验证加载状态必须匹配稳定节点和强类型变量边界，隔离状态保持可审计。
	 * Verifies loaded states match stable nodes and typed variable bounds while quarantined states remain auditable.
	 */
	@Test
	void stateLoadFailsClosedOnIncompatibleShape() {
		QuestGraphDefinitionRegistry registry = new QuestGraphDefinitionRegistry();
		registry.installInitial(data(graph(2, 5, "active")));

		assertDoesNotThrow(() -> registry.validateState(state(2, "active", 5, Lifecycle.ACTIVE)));
		assertThrows(IllegalArgumentException.class, () -> registry.validateState(state(2, "missing", 2, Lifecycle.ACTIVE)));
		assertThrows(IllegalArgumentException.class, () -> registry.validateState(state(2, "active", 6, Lifecycle.ACTIVE)));
		assertDoesNotThrow(() -> registry.validateState(state(1, "legacy", 999, Lifecycle.QUARANTINED)));
	}

	/** 创建包含单个 owner 的不可变定义数据。 / Creates immutable definition data with one owner. */
	private static CompiledQuestGraphData data(CompiledQuestGraph graph) {
		return new CompiledQuestGraphData(Map.of(graph.questId(), graph), Map.of());
	}

	/** 创建聚焦版本与状态形状测试的最小图。 / Creates a minimal graph for focused version and state-shape tests. */
	private static CompiledQuestGraph graph(int version, int maximum, String nodeId) {
		return new CompiledQuestGraph(1, version, PLAYER, nodeId, Map.of("count", new IntVariable("count", PLAYER, 0, 0, maximum)),
			Map.of(nodeId, new Node(nodeId, true, List.of())));
	}

	/** 创建指定生命周期的玩家图状态。 / Creates a player graph state with the requested lifecycle. */
	private static PlayerQuestGraphState state(int definitionVersion, String nodeId, int value, Lifecycle lifecycle) {
		return new PlayerQuestGraphState(1, definitionVersion, 0, nodeId, CompiledQuestGraph.QuestStatus.START, QuestHistory.EMPTY, null,
			lifecycle, Map.of("count", new IntValue(value)), Map.of(), null, Map.of(),
			lifecycle == Lifecycle.QUARANTINED ? "TEST_QUARANTINE" : null);
	}
}
