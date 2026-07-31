package com.aionemu.gameserver.questEngine.graph;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.BooleanVariable;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.IntVariable;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.StateScope;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Variable;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.BooleanValue;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.IntValue;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.Lifecycle;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.PreparedTransition;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.VariableValue;

/**
 * 在完整编译与状态兼容校验后原子安装不可变任务图定义快照。
 * Atomically installs immutable quest-graph definition snapshots after full compilation and state compatibility validation.
 */
public final class QuestGraphDefinitionRegistry {

	private static final CompiledQuestGraphData EMPTY_DATA = new CompiledQuestGraphData(Map.of(), Map.of());
	private final AtomicReference<Snapshot> current = new AtomicReference<>(new Snapshot(0, EMPTY_DATA));

	/**
	 * 返回当前不可变定义快照。
	 * Returns the current immutable definition snapshot.
	 */
	public Snapshot snapshot() {
		return current.get();
	}

	/**
	 * 仅在启动时安装首个已完整编译的定义集合。
	 * Installs the first fully compiled definition set during startup only.
	 */
	public synchronized Snapshot installInitial(CompiledQuestGraphData candidate) {
		Objects.requireNonNull(candidate, "candidate");
		if (current.get().generation() != 0) {
			throw new IllegalStateException("Quest graph definitions are already initialized");
		}
		validateRuntimeScopes(candidate);
		Snapshot installed = new Snapshot(1, candidate);
		current.set(installed);
		return installed;
	}

	/**
	 * 使用全部已持久化状态校验定义版本与形状后原子替换当前集合。
	 * Atomically replaces the current set after validating definition versions and shapes against all persisted states.
	 */
	public synchronized Snapshot reload(CompiledQuestGraphData candidate, Collection<PlayerQuestGraphState> allPersistedStates) {
		Objects.requireNonNull(candidate, "candidate");
		List<PlayerQuestGraphState> states = List.copyOf(Objects.requireNonNull(allPersistedStates, "allPersistedStates"));
		Snapshot installed = current.get();
		if (installed.generation() == 0) {
			throw new IllegalStateException("Quest graph definitions are not initialized");
		}
		validateRuntimeScopes(candidate);
		validateReload(installed.data(), candidate);
		states.forEach(state -> validateState(candidate, Objects.requireNonNull(state, "persisted state")));
		if (installed.data().equals(candidate)) {
			return installed;
		}
		Snapshot replacement = new Snapshot(Math.addExact(installed.generation(), 1), candidate);
		current.set(replacement);
		return replacement;
	}

	/**
	 * 按当前定义验证单个数据库状态，供状态加载路径 fail-closed 使用。
	 * Validates one database state against current definitions for fail-closed state loading.
	 */
	public void validateState(PlayerQuestGraphState state) {
		Snapshot installed = current.get();
		if (installed.generation() == 0) {
			throw new IllegalStateException("Quest graph definitions are not initialized");
		}
		validateState(installed.data(), Objects.requireNonNull(state, "state"));
	}

	/**
	 * 拒绝无 typed bridge 的非 PLAYER 图或变量范围。
	 * Rejects non-PLAYER graph or variable scopes that have no typed bridge.
	 */
	private static void validateRuntimeScopes(CompiledQuestGraphData data) {
		for (CompiledQuestGraph graph : data.graphs().values()) {
			if (graph.scope() != StateScope.PLAYER
					|| graph.variables().values().stream().anyMatch(variable -> variable.scope() != StateScope.PLAYER)) {
				throw new IllegalArgumentException("Quest " + graph.questId() + " requires an unsupported state scope");
			}
		}
	}

	/**
	 * 拒绝 owner 删除、版本回退和同版本定义漂移。
	 * Rejects owner removal, version rollback, and definition drift at the same version.
	 */
	private static void validateReload(CompiledQuestGraphData installed, CompiledQuestGraphData candidate) {
		for (CompiledQuestGraph previous : installed.graphs().values()) {
			CompiledQuestGraph next = candidate.graphs().get(previous.questId());
			if (next == null) {
				throw new IllegalArgumentException("Reload removes quest graph owner " + previous.questId());
			}
			if (next.version() < previous.version()) {
				throw new IllegalArgumentException("Reload rolls quest " + previous.questId() + " back from version "
					+ previous.version() + " to " + next.version());
			}
			if (next.version() == previous.version() && !next.equals(previous)) {
				throw new IllegalArgumentException("Reload changes quest " + previous.questId() + " without a version increment");
			}
		}
		if (installed.graphs().equals(candidate.graphs()) && !installed.equals(candidate)) {
			throw new IllegalArgumentException("Reload changes the event index without a definition version increment");
		}
	}

	/**
	 * 校验 owner、定义版本、稳定节点、变量形状和 PREPARED 转换位置。
	 * Validates owner, definition version, stable node, variable shape, and PREPARED transition location.
	 */
	private static void validateState(CompiledQuestGraphData data, PlayerQuestGraphState state) {
		CompiledQuestGraph graph = data.graphs().get(state.getQuestId());
		if (graph == null) {
			throw new IllegalArgumentException("State references missing quest graph owner " + state.getQuestId());
		}
		if (state.getLifecycle() == Lifecycle.QUARANTINED) {
			return;
		}
		if (state.getDefinitionVersion() != graph.version()) {
			throw new IllegalArgumentException("Quest " + state.getQuestId() + " state version " + state.getDefinitionVersion()
				+ " is incompatible with definition version " + graph.version());
		}
		CompiledQuestGraph.Node node = graph.nodes().get(state.getNodeId());
		if (node == null) {
			throw new IllegalArgumentException("Quest " + state.getQuestId() + " state references missing node " + state.getNodeId());
		}
		validateVariables(graph, state);
		if (state.getLifecycle() == Lifecycle.PREPARED) {
			validatePreparedTransition(graph, state, node);
		}
	}

	/**
	 * 校验持久化变量与定义具有完全一致的名称、类型和数值边界。
	 * Validates that persisted variables exactly match definition names, types, and value bounds.
	 */
	private static void validateVariables(CompiledQuestGraph graph, PlayerQuestGraphState state) {
		if (!state.getVariables().keySet().equals(graph.variables().keySet())) {
			throw new IllegalArgumentException("Quest " + state.getQuestId() + " state variable names are incompatible");
		}
		for (Map.Entry<String, Variable> entry : graph.variables().entrySet()) {
			String name = entry.getKey();
			Variable definition = entry.getValue();
			VariableValue value = state.getVariables().get(name);
			if (definition instanceof IntVariable integer) {
				if (!(value instanceof IntValue actual) || actual.value() < integer.min() || actual.value() > integer.max()) {
					throw new IllegalArgumentException("Quest " + state.getQuestId() + " state variable " + name + " is incompatible");
				}
			} else if (definition instanceof BooleanVariable && !(value instanceof BooleanValue)) {
				throw new IllegalArgumentException("Quest " + state.getQuestId() + " state variable " + name + " is incompatible");
			}
		}
	}

	/**
	 * 校验 PREPARED journal 仍指向当前节点中的同名转换和有效动作边界。
	 * Validates that a PREPARED journal still targets a named transition and valid action boundary in the current node.
	 */
	private static void validatePreparedTransition(CompiledQuestGraph graph, PlayerQuestGraphState state, CompiledQuestGraph.Node node) {
		PreparedTransition journal = state.getJournal();
		CompiledQuestGraph.Transition transition = journal.isTargetCommitted()
			? graph.nodes().values().stream().flatMap(candidate -> candidate.transitions().stream())
				.filter(candidate -> candidate.id().equals(journal.getTransitionId())
					&& candidate.targetNode().equals(state.getNodeId())).findFirst().orElse(null)
			: node.transitions().stream().filter(candidate -> candidate.id().equals(journal.getTransitionId())).findFirst().orElse(null);
		if (transition == null || journal.getNextActionIndex() > transition.actions().size()) {
			throw new IllegalArgumentException("Quest " + state.getQuestId() + " PREPARED journal is incompatible");
		}
	}

	/**
	 * 表示一次原子安装后的单调 generation 与不可变定义数据。
	 * Represents the monotonic generation and immutable definition data after an atomic installation.
	 */
	public record Snapshot(long generation, CompiledQuestGraphData data) {
		/** 校验 generation 与定义数据。 / Validates the generation and definition data. */
		public Snapshot {
			if (generation < 0) {
				throw new IllegalArgumentException("Quest graph generation must be non-negative");
			}
			Objects.requireNonNull(data, "data");
		}
	}
}
