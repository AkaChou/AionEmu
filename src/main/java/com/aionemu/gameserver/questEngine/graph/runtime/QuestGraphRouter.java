package com.aionemu.gameserver.questEngine.graph.runtime;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraphData;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraphData.EventRoute;
import com.aionemu.gameserver.questEngine.graph.runtime.DispatchResult.Propagation;
import com.aionemu.gameserver.questEngine.graph.runtime.DispatchResult.Status;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.DialogEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.KillEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.RoutingPolicy;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.Lifecycle;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphStateList;

/**
 * 使用编译期事件索引按固定策略路由不可变任务图事件。
 * Routes immutable quest graph events by fixed policy using the compile-time event index.
 */
public final class QuestGraphRouter {

	private final CompiledQuestGraphData graphData;

	/**
	 * 创建只读任务图路由器。
	 * Creates a read-only quest graph router.
	 */
	public QuestGraphRouter(CompiledQuestGraphData graphData) {
		this.graphData = Objects.requireNonNull(graphData, "graphData");
	}

	/**
	 * 根据事件固定策略评估候选；evaluator 只决定业务状态，传播由路由器统一控制。
	 * Evaluates candidates by the event's fixed policy; the evaluator decides status while the router controls propagation.
	 */
	public DispatchResult dispatch(QuestGraphEvent event, PlayerQuestGraphStateList playerStates, Function<Match, Status> evaluator) {
		Objects.requireNonNull(event, "event");
		Objects.requireNonNull(playerStates, "playerStates");
		Objects.requireNonNull(evaluator, "evaluator");
		List<EventRoute> routes = graphData.eventIndex().getOrDefault(event.eventKey(), List.of());
		return switch (event.routingPolicy()) {
			case EXCLUSIVE -> dispatchExclusive(event, playerStates, routes, evaluator);
			case BROADCAST -> dispatchBroadcast(event, playerStates, routes, evaluator);
		};
	}

	/**
	 * 表示已通过索引、节点和恢复状态筛选的候选转换。
	 * Represents a candidate transition filtered by index, node, and recovery state.
	 */
	public record Match(QuestGraphEvent event, CompiledQuestGraph graph, EventRoute route, PlayerQuestGraphState state) {
	}

	/**
	 * 独占评估候选并在首个非 NO_MATCH 状态停止。
	 * Evaluates exclusive candidates and stops on the first non-NO_MATCH status.
	 */
	private DispatchResult dispatchExclusive(QuestGraphEvent event, PlayerQuestGraphStateList playerStates, List<EventRoute> routes,
			Function<Match, Status> evaluator) {
		for (EventRoute route : routes) {
			Status status = evaluate(event, playerStates, route, evaluator);
			if (status != Status.NO_MATCH) {
				return new DispatchResult(status, Propagation.STOP);
			}
		}
		return new DispatchResult(Status.NO_MATCH, Propagation.CONTINUE);
	}

	/**
	 * 广播评估全部任务，隔离单任务失败并聚合最严重结果。
	 * Evaluates all quests in broadcast mode, isolates per-quest failures, and aggregates the most severe result.
	 */
	private DispatchResult dispatchBroadcast(QuestGraphEvent event, PlayerQuestGraphStateList playerStates, List<EventRoute> routes,
			Function<Match, Status> evaluator) {
		Set<Integer> concludedQuestIds = new HashSet<>();
		Status aggregate = Status.NO_MATCH;
		for (EventRoute route : routes) {
			if (concludedQuestIds.contains(route.questId())) {
				continue;
			}
			Status status = evaluate(event, playerStates, route, evaluator);
			if (status != Status.NO_MATCH) {
				concludedQuestIds.add(route.questId());
				aggregate = moreSevere(aggregate, status);
			}
		}
		return new DispatchResult(aggregate, Propagation.CONTINUE);
	}

	/**
	 * 校验候选的 graph/state/event 后调用状态 evaluator。
	 * Validates candidate graph, state, and event before invoking the status evaluator.
	 */
	private Status evaluate(QuestGraphEvent event, PlayerQuestGraphStateList playerStates, EventRoute route,
			Function<Match, Status> evaluator) {
		CompiledQuestGraph graph = graphData.graphs().get(route.questId());
		if (graph == null) {
			return Status.FAILED;
		}
		PlayerQuestGraphState state = playerStates.get(route.questId());
		if (state != null && (state.getDefinitionVersion() != graph.version() || state.getLifecycle() != Lifecycle.ACTIVE
				|| !graph.nodes().containsKey(state.getNodeId()))) {
			return Status.FAILED;
		}
		String nodeId = state == null ? graph.initialNode() : state.getNodeId();
		if (!route.nodeId().equals(nodeId) || !matches(event, route)) {
			return Status.NO_MATCH;
		}
		try {
			return Objects.requireNonNull(evaluator.apply(new Match(event, graph, route, state)), "evaluator result");
		} catch (RuntimeException e) {
			return Status.FAILED;
		}
	}

	/**
	 * 校验类型化事件参数与编译转换事件完全一致。
	 * Validates that typed event parameters exactly match the compiled transition event.
	 */
	private static boolean matches(QuestGraphEvent event, EventRoute route) {
		return switch (event) {
			case DialogEvent dialog -> route.transition().event().type() == dialog.type()
				&& route.transition().event().npcId() == dialog.npcId()
				&& route.transition().event().dialog().equals(dialog.dialog());
			case KillEvent kill -> route.transition().event().type() == kill.type()
				&& route.transition().event().npcId() == kill.npcId();
		};
	}

	/**
	 * 按 FAILED、APPLIED、REJECTED、NO_MATCH 顺序聚合广播结果。
	 * Aggregates broadcast results in FAILED, APPLIED, REJECTED, NO_MATCH severity order.
	 */
	private static Status moreSevere(Status current, Status candidate) {
		return severity(candidate) > severity(current) ? candidate : current;
	}

	/**
	 * 返回用于广播聚合的固定严重度。
	 * Returns the fixed severity used for broadcast aggregation.
	 */
	private static int severity(Status status) {
		return switch (status) {
			case NO_MATCH -> 0;
			case REJECTED -> 1;
			case APPLIED -> 2;
			case FAILED -> 3;
		};
	}
}
