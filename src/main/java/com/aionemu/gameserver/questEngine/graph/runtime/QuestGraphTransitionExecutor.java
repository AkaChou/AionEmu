package com.aionemu.gameserver.questEngine.graph.runtime;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Action;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.BooleanVariable;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Condition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Event;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.IntVariable;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatus;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatusCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.StateScope;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Transition;
import com.aionemu.gameserver.questEngine.graph.runtime.DispatchResult.Status;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphRouter.Match;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.BooleanValue;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.IntValue;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.Lifecycle;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.PreparedTransition;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.QuestHistory;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.VariableValue;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphStateList;

/**
 * 以 PREPARED journal 和 revision CAS 确定性执行单个玩家任务图转换。
 * Deterministically executes one player quest-graph transition using a PREPARED journal and revision CAS.
 */
public final class QuestGraphTransitionExecutor {

	/**
	 * 定义只读条件能力的显式结果。
	 * Defines explicit outcomes of a read-only condition capability.
	 */
	public enum ConditionResult {
		MATCHED,
		NOT_MATCHED,
		FAILED
	}

	/**
	 * 定义必需动作预检的显式结果。
	 * Defines explicit outcomes of required-action preflight.
	 */
	public enum PreflightResult {
		READY,
		REJECTED,
		FAILED
	}

	/**
	 * 定义幂等动作执行的显式结果。
	 * Defines explicit outcomes of idempotent action execution.
	 */
	public enum ActionResult {
		APPLIED,
		ALREADY_APPLIED,
		REJECTED,
		FAILED
	}

	/**
	 * 定义状态 CAS 的显式结果。
	 * Defines explicit outcomes of state compare-and-set persistence.
	 */
	public enum PersistenceResult {
		APPLIED,
		CONFLICT,
		FAILED
	}

	/**
	 * 保存一次类型化条件评估所需的不可变输入。
	 * Holds immutable input required to evaluate one typed condition.
	 */
	public record ConditionInvocation(Condition condition, int questId, QuestStatus questStatus, QuestGraphEvent event) {
		/**
		 * 校验条件调用输入。
		 * Validates condition invocation input.
		 */
		public ConditionInvocation {
			Objects.requireNonNull(condition, "condition");
			Objects.requireNonNull(questStatus, "questStatus");
			Objects.requireNonNull(event, "event");
		}
	}

	/**
	 * 保存一次动作预检或执行所需的不可变输入和稳定幂等键。
	 * Holds immutable action input and its stable idempotency key for preflight or execution.
	 */
	public record ActionInvocation(Action action, int questId, int actionIndex, QuestStatus questStatus, QuestGraphEvent event,
		String idempotencyKey) {
		/**
		 * 校验动作调用输入。
		 * Validates action invocation input.
		 */
		public ActionInvocation {
			Objects.requireNonNull(action, "action");
			Objects.requireNonNull(questStatus, "questStatus");
			Objects.requireNonNull(event, "event");
			if (actionIndex < 0 || idempotencyKey == null || idempotencyKey.isBlank()) {
				throw new IllegalArgumentException("Action index/idempotency key is invalid");
			}
		}
	}

	/**
	 * 组合玩家状态、类型化能力和 CAS 持久化回调，不引入单实现接口。
	 * Composes player state, typed capabilities, and CAS persistence callbacks without single-use interfaces.
	 */
	public record TransitionContext(int playerId, PlayerQuestGraphStateList states,
		Function<ConditionInvocation, ConditionResult> conditionEvaluator,
		Function<ActionInvocation, PreflightResult> actionPreflight,
		Function<ActionInvocation, ActionResult> actionExecutor,
		BiFunction<Long, PlayerQuestGraphState, PersistenceResult> persistence) {
		/**
		 * 校验转换执行依赖；新状态写入时 persistence 的 expected revision 为 null。
		 * Validates transition dependencies; persistence receives a null expected revision for a new state.
		 */
		public TransitionContext {
			if (playerId <= 0) {
				throw new IllegalArgumentException("Player id must be positive");
			}
			Objects.requireNonNull(states, "states");
			Objects.requireNonNull(conditionEvaluator, "conditionEvaluator");
			Objects.requireNonNull(actionPreflight, "actionPreflight");
			Objects.requireNonNull(actionExecutor, "actionExecutor");
			Objects.requireNonNull(persistence, "persistence");
		}
	}

	/**
	 * 检查条件与全部动作预检，写入 PREPARED 后执行并提交转换。
	 * Checks conditions and all action preflights, then executes and commits after persisting PREPARED.
	 */
	public Status execute(Match match, TransitionContext context) {
		Objects.requireNonNull(match, "match");
		Objects.requireNonNull(context, "context");
		if (match.event().playerId() != context.playerId() || !supportsPlayerState(match.graph())) {
			return Status.FAILED;
		}
		synchronized (context.states()) {
			try {
				PlayerQuestGraphState current = context.states().get(match.graph().questId());
				if (current != match.state()) {
					return Status.FAILED;
				}
				Status conditionStatus = evaluateConditions(match, context);
				if (conditionStatus != Status.APPLIED) {
					return conditionStatus;
				}
				QuestStatus questStatus = current == null ? QuestStatus.NONE : current.getQuestStatus();
				Status preflightStatus = preflight(match.graph(), match.route().transition(), match.event(), questStatus, 0, context);
				if (preflightStatus != Status.APPLIED) {
					return preflightStatus;
				}
				PlayerQuestGraphState prepared = prepare(match, current);
				Long expectedRevision = current == null ? null : current.getRevision();
				if (persist(context, expectedRevision, prepared) != PersistenceResult.APPLIED) {
					return Status.FAILED;
				}
				context.states().put(prepared);
				return resumePrepared(match.graph(), match.route().transition(), match.event(), prepared, context);
			} catch (RuntimeException e) {
				return Status.FAILED;
			}
		}
	}

	/**
	 * 从已持久化 journal 的下一个动作恢复并完成转换。
	 * Resumes and completes a transition from the next action stored in a persisted journal.
	 */
	public Status recover(CompiledQuestGraph graph, TransitionContext context) {
		Objects.requireNonNull(graph, "graph");
		Objects.requireNonNull(context, "context");
		if (!supportsPlayerState(graph)) {
			return Status.FAILED;
		}
		synchronized (context.states()) {
			try {
				PlayerQuestGraphState state = context.states().get(graph.questId());
				if (state == null) {
					return Status.NO_MATCH;
				}
				if (state.getDefinitionVersion() != graph.version() || state.getLifecycle() != Lifecycle.PREPARED) {
					return Status.FAILED;
				}
				CompiledQuestGraph.Node node = graph.nodes().get(state.getNodeId());
				if (node == null) {
					return Status.FAILED;
				}
				PreparedTransition journal = state.getJournal();
				Transition transition = node.transitions().stream()
					.filter(candidate -> candidate.id().equals(journal.getTransitionId())).findFirst().orElse(null);
				if (transition == null || journal.getNextActionIndex() > transition.actions().size()) {
					return Status.FAILED;
				}
				QuestGraphEvent event = QuestGraphEventCodec.decode(journal.getEventPayload());
				if (event.playerId() != context.playerId() || !journal.getEventId().equals(event.eventId()) || !matches(event, transition.event())) {
					return Status.FAILED;
				}
				Status preflightStatus = preflight(graph, transition, event, state.getQuestStatus(), journal.getNextActionIndex(), context);
				return preflightStatus == Status.APPLIED ? resumePrepared(graph, transition, event, state, context) : preflightStatus;
			} catch (RuntimeException e) {
				return Status.FAILED;
			}
		}
	}

	/**
	 * 评估全部只读条件，并将不匹配与能力失败分别映射为路由状态。
	 * Evaluates all read-only conditions and separately maps mismatch and capability failure to routing status.
	 */
	private static Status evaluateConditions(Match match, TransitionContext context) {
		QuestStatus questStatus = match.state() == null ? QuestStatus.NONE : match.state().getQuestStatus();
		for (Condition condition : match.route().transition().conditions()) {
			if (condition instanceof QuestStatusCondition statusCondition && statusCondition.expected() != questStatus) {
				return Status.NO_MATCH;
			}
			ConditionResult result;
			try {
				result = Objects.requireNonNull(context.conditionEvaluator()
					.apply(new ConditionInvocation(condition, match.graph().questId(), questStatus, match.event())), "condition result");
			} catch (RuntimeException e) {
				return Status.FAILED;
			}
			if (result == ConditionResult.NOT_MATCHED) {
				return Status.NO_MATCH;
			}
			if (result == ConditionResult.FAILED) {
				return Status.FAILED;
			}
		}
		return Status.APPLIED;
	}

	/**
	 * 从指定动作位置预检全部剩余动作，不产生状态或业务副作用。
	 * Preflights every remaining action from the given index without state or business side effects.
	 */
	private static Status preflight(CompiledQuestGraph graph, Transition transition, QuestGraphEvent event, QuestStatus questStatus, int startIndex,
		TransitionContext context) {
		for (int index = startIndex; index < transition.actions().size(); index++) {
			Action action = transition.actions().get(index);
			QuestStatus nextStatus;
			try {
				nextStatus = reduceQuestStatus(questStatus, action);
			} catch (RuntimeException e) {
				return Status.FAILED;
			}
			PreflightResult result;
			try {
				result = Objects.requireNonNull(context.actionPreflight().apply(invocation(graph, transition, event, questStatus, index)),
					"preflight result");
			} catch (RuntimeException e) {
				return Status.FAILED;
			}
			if (result == PreflightResult.REJECTED) {
				return Status.REJECTED;
			}
			if (result == PreflightResult.FAILED) {
				return Status.FAILED;
			}
			questStatus = nextStatus;
		}
		return Status.APPLIED;
	}

	/**
	 * 创建包含事件快照和初始变量的下一 revision PREPARED 状态。
	 * Creates the next-revision PREPARED state with an event snapshot and initialized variables.
	 */
	private static PlayerQuestGraphState prepare(Match match, PlayerQuestGraphState current) {
		long baseRevision = current == null ? -1 : current.getRevision();
		long preparedRevision = Math.addExact(baseRevision, 1);
		Map<String, VariableValue> variables = current == null ? initialVariables(match.graph()) : current.getVariables();
		Map<String, Long> deadlines = current == null ? Map.of() : current.getDeadlines();
		Map<String, PlayerQuestGraphState.CleanupLease> leases = current == null ? Map.of() : current.getCleanupLeases();
		Long instanceRunId = current == null ? null : current.getInstanceRunId();
		String nodeId = current == null ? match.graph().initialNode() : current.getNodeId();
		PreparedTransition journal = new PreparedTransition(baseRevision, match.event().eventId(), match.route().transition().id(), 0,
			QuestGraphEventCodec.encode(match.event()));
		QuestStatus questStatus = current == null ? QuestStatus.NONE : current.getQuestStatus();
		QuestHistory history = current == null ? QuestHistory.EMPTY : current.getHistory();
		return new PlayerQuestGraphState(match.graph().questId(), match.graph().version(), preparedRevision, nodeId, questStatus, history, instanceRunId,
			Lifecycle.PREPARED, variables, deadlines, journal, leases, null);
	}

	/**
	 * 顺序执行剩余幂等动作，每个成功动作后持久化 journal 进度，最后提交目标节点。
	 * Executes remaining idempotent actions in order, persists journal progress after each success, then commits the target node.
	 */
	private static Status resumePrepared(CompiledQuestGraph graph, Transition transition, QuestGraphEvent event,
		PlayerQuestGraphState prepared, TransitionContext context) {
		PlayerQuestGraphState current = prepared;
		for (int index = prepared.getJournal().getNextActionIndex(); index < transition.actions().size(); index++) {
			ActionResult result;
			try {
				result = Objects.requireNonNull(context.actionExecutor()
					.apply(invocation(graph, transition, event, current.getQuestStatus(), index)), "action result");
			} catch (RuntimeException e) {
				return Status.FAILED;
			}
			if (result == ActionResult.REJECTED) {
				return Status.REJECTED;
			}
			if (result == ActionResult.FAILED) {
				return Status.FAILED;
			}
			QuestStatus questStatus = reduceQuestStatus(current.getQuestStatus(), transition.actions().get(index));
			PlayerQuestGraphState progressed = copy(current, current.getRevision() + 1, current.getNodeId(), questStatus, Lifecycle.PREPARED,
				new PreparedTransition(current.getJournal().getBaseRevision(), event.eventId(), transition.id(), index + 1,
					current.getJournal().getEventPayload()));
			if (persist(context, current.getRevision(), progressed) != PersistenceResult.APPLIED) {
				return Status.FAILED;
			}
			context.states().put(progressed);
			current = progressed;
		}
		PlayerQuestGraphState committed = copy(current, current.getRevision() + 1, transition.targetNode(), current.getQuestStatus(),
			Lifecycle.ACTIVE, null);
		if (persist(context, current.getRevision(), committed) != PersistenceResult.APPLIED) {
			return Status.FAILED;
		}
		context.states().put(committed);
		return Status.APPLIED;
	}

	/**
	 * 构造包含动作序号的稳定幂等调用。
	 * Builds a stable idempotent invocation that includes the action index.
	 */
	private static ActionInvocation invocation(CompiledQuestGraph graph, Transition transition, QuestGraphEvent event,
		QuestStatus questStatus, int actionIndex) {
		String key = event.eventId().length() + ":" + event.eventId() + ':' + graph.questId() + ':' + transition.id() + ':'
			+ event.playerId() + ':' + actionIndex;
		return new ActionInvocation(transition.actions().get(actionIndex), graph.questId(), actionIndex, questStatus, event, key);
	}

	/**
	 * 将已成功动作归约到 canonical 任务状态；未知或非法状态组合显式失败。
	 * Reduces a successful action into canonical quest status and explicitly fails unknown or illegal combinations.
	 */
	private static QuestStatus reduceQuestStatus(QuestStatus current, Action action) {
		return switch (action.type()) {
			case START_QUEST -> switch (current) {
				case NONE, COMPLETE, LOCKED -> QuestStatus.START;
				case START -> QuestStatus.START;
				case REWARD -> throw new IllegalStateException("Cannot start a quest awaiting reward");
			};
		};
	}

	/**
	 * 仅接受当前持久化与锁模型已实现的纯 PLAYER 状态图。
	 * Accepts only pure PLAYER-state graphs supported by the current persistence and locking model.
	 */
	private static boolean supportsPlayerState(CompiledQuestGraph graph) {
		return graph.scope() == StateScope.PLAYER && graph.variables().values().stream().allMatch(variable -> variable.scope() == StateScope.PLAYER);
	}

	/**
	 * 调用显式 CAS callback，并将异常转换为失败结果。
	 * Invokes the explicit CAS callback and converts exceptions into failure results.
	 */
	private static PersistenceResult persist(TransitionContext context, Long expectedRevision, PlayerQuestGraphState state) {
		try {
			return Objects.requireNonNull(context.persistence().apply(expectedRevision, state), "persistence result");
		} catch (RuntimeException e) {
			return PersistenceResult.FAILED;
		}
	}

	/**
	 * 从图定义建立确定排序的玩家初始变量。
	 * Builds deterministically ordered initial player variables from the graph definition.
	 */
	private static Map<String, VariableValue> initialVariables(CompiledQuestGraph graph) {
		Map<String, VariableValue> values = new LinkedHashMap<>();
		graph.variables().forEach((name, variable) -> values.put(name, switch (variable) {
			case IntVariable integer -> new IntValue(integer.initial());
			case BooleanVariable bool -> new BooleanValue(bool.initial());
		}));
		return values;
	}

	/**
	 * 复制状态并只替换转换推进字段。
	 * Copies state while replacing only transition-progress fields.
	 */
	private static PlayerQuestGraphState copy(PlayerQuestGraphState source, long revision, String nodeId, QuestStatus questStatus,
		Lifecycle lifecycle, PreparedTransition journal) {
		return new PlayerQuestGraphState(source.getQuestId(), source.getDefinitionVersion(), revision, nodeId, questStatus,
			source.getHistory(), source.getInstanceRunId(), lifecycle, source.getVariables(), source.getDeadlines(), journal,
			source.getCleanupLeases(), null);
	}

	/**
	 * 验证恢复事件与已编译转换事件完全一致。
	 * Verifies that a recovered event exactly matches the compiled transition event.
	 */
	private static boolean matches(QuestGraphEvent event, Event expected) {
		if (event.type() != expected.type() || event.targetId() != expected.npcId()) {
			return false;
		}
		return !(event instanceof QuestGraphEvent.DialogEvent dialog) || Objects.equals(dialog.dialog(), expected.dialog());
	}
}
