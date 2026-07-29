package com.aionemu.gameserver.questEngine.graph.runtime;

import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.ToLongFunction;

import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Action;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.ActionPhase;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.AbandonQuestAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.AddCompletionCountAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.AddQuestVariableAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.IncrementPackedCounterAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.BooleanVariable;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Condition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.CraftSkillEligibilityCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Event;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EndQuestTimerAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.IntVariable;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.FinishQuestAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.GiveQuestItemAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.GrantCraftSkillRewardAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.NoRepeatDeadlinePolicy;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestCompletionCountCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestRewardCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestRepeatAvailableCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatus;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatusCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestVariableCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PackedCounterCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.RepeatPrivilegeMode;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.RemoveCollectedItemsAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.RemoveQuestWorkItemsAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.LearnRecipeAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.DeleteRecipeAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.NotifyRecipeRejectionAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.RecipeLearnableCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.RemoveQuestItemAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.SetCompletionCountAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.SetQuestStatusAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.SetQuestVariableAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.StartQuestAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.StartEventQuestAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.StartQuestTimerAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.StateScope;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.SyncQuestTimerAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.SyncCraftSkillRewardAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Transition;
import com.aionemu.gameserver.questEngine.graph.runtime.DispatchResult.Status;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphRouter.Match;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.BooleanValue;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.CleanupLease;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.IntValue;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.ItemMutationKind;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.ItemMutationPlan;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.Lifecycle;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.PreparedTransition;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.QuestHistory;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.RepeatDeadlineDisposition;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.RepeatDeadlineResolution;
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
	 * 保存只读查询所需的玩家 owner、状态快照和类型化条件端点。
	 * Holds the player owner, state snapshot, and typed condition endpoint required by a read-only query.
	 */
	public record ReadOnlyContext(int playerId, PlayerQuestGraphStateList states,
			Function<ConditionInvocation, ConditionResult> conditionEvaluator) {
		/** 校验只读查询依赖。 / Validates read-only query dependencies. */
		public ReadOnlyContext {
			if (playerId <= 0) {
				throw new IllegalArgumentException("Read-only query player id is invalid");
			}
			Objects.requireNonNull(states, "states");
			Objects.requireNonNull(conditionEvaluator, "conditionEvaluator");
		}
	}

	/**
	 * 保存一次动作预检或执行所需的不可变输入和稳定幂等键。
	 * Holds immutable action input and its stable idempotency key for preflight or execution.
	 */
	public record ActionInvocation(Action action, int questId, int actionIndex, QuestStatus questStatus, QuestGraphEvent event,
		RepeatDeadlineResolution repeatDeadlineResolution, ItemMutationPlan itemMutationPlan, Map<String, CleanupLease> cleanupLeases,
		String idempotencyKey) {
		/** 创建不携带 cleanup 资源的兼容调用。 / Creates a compatibility invocation without cleanup resources. */
		public ActionInvocation(Action action, int questId, int actionIndex, QuestStatus questStatus, QuestGraphEvent event,
				RepeatDeadlineResolution repeatDeadlineResolution, ItemMutationPlan itemMutationPlan, String idempotencyKey) {
			this(action, questId, actionIndex, questStatus, event, repeatDeadlineResolution, itemMutationPlan, Map.of(), idempotencyKey);
		}

		/**
		 * 校验动作调用输入。
		 * Validates action invocation input.
		 */
		public ActionInvocation {
			Objects.requireNonNull(action, "action");
			Objects.requireNonNull(questStatus, "questStatus");
			Objects.requireNonNull(event, "event");
			Objects.requireNonNull(repeatDeadlineResolution, "repeatDeadlineResolution");
			cleanupLeases = Map.copyOf(Objects.requireNonNull(cleanupLeases, "cleanup leases"));
			if (actionIndex < 0 || idempotencyKey == null || idempotencyKey.isBlank()) {
				throw new IllegalArgumentException("Action index/idempotency key is invalid");
			}
		}
	}

	/**
	 * 组合玩家状态、类型化能力和 CAS 持久化回调，不引入单实现接口。
	 * Composes player state, typed capabilities, and CAS persistence callbacks without single-use interfaces.
	 */
	public record TransitionContext(int playerId, int accessLevel, ZoneId serverZoneId, PlayerQuestGraphStateList states,
		Function<ConditionInvocation, ConditionResult> conditionEvaluator,
		Function<ActionInvocation, PreflightResult> actionPreflight,
		Function<ActionInvocation, ActionResult> actionExecutor,
		ToLongFunction<Integer> itemCountReader,
		Function<Map<Integer, ItemMutationPlan>, PreflightResult> itemMutationPreflight,
		BiFunction<Long, PlayerQuestGraphState, PersistenceResult> persistence) {
		/**
		 * 创建不支持物品 journal 的兼容上下文；遇到物品动作时会显式失败。
		 * Creates a compatibility context without item journaling; item actions fail explicitly.
		 */
		public TransitionContext(int playerId, int accessLevel, ZoneId serverZoneId, PlayerQuestGraphStateList states,
				Function<ConditionInvocation, ConditionResult> conditionEvaluator, Function<ActionInvocation, PreflightResult> actionPreflight,
				Function<ActionInvocation, ActionResult> actionExecutor,
				BiFunction<Long, PlayerQuestGraphState, PersistenceResult> persistence) {
			this(playerId, accessLevel, serverZoneId, states, conditionEvaluator, actionPreflight, actionExecutor, itemId -> -1,
				plans -> plans.isEmpty() ? PreflightResult.READY : PreflightResult.FAILED, persistence);
		}

		/**
		 * 组合正式物品、计时器、影片、lifecycle 与 recipe typed adapter。
		 * Composes production item, timer, movie, lifecycle, and recipe typed adapters.
		 */
		public TransitionContext(int playerId, int accessLevel, ZoneId serverZoneId, PlayerQuestGraphStateList states,
				Function<ConditionInvocation, ConditionResult> conditionEvaluator, Function<ActionInvocation, PreflightResult> actionPreflight,
				Function<ActionInvocation, ActionResult> actionExecutor, QuestGraphItemActionAdapter itemActions,
				QuestGraphTimerActionAdapter timerActions, QuestGraphMovieActionAdapter movieActions,
				QuestGraphLifecycleActionAdapter lifecycleActions, QuestGraphRecipeBridge recipeBridge,
				BiFunction<Long, PlayerQuestGraphState, PersistenceResult> persistence) {
			this(playerId, accessLevel, serverZoneId, states,
				invocation -> invocation.condition() instanceof RecipeLearnableCondition
					? recipeBridge.evaluate(invocation) : conditionEvaluator.apply(invocation),
				invocation -> invocation.itemMutationPlan() != null ? itemActions.preflight(invocation)
					: isRequiredTimerAction(invocation.action()) ? timerActions.preflight(invocation)
						: isLifecycleAction(invocation.action()) ? lifecycleActions.preflight(invocation)
							: isRequiredRecipeAction(invocation.action()) ? recipeBridge.preflight(invocation)
								: actionPreflight.apply(invocation),
				invocation -> invocation.itemMutationPlan() != null ? itemActions.execute(invocation)
					: isTimerAction(invocation.action()) ? timerActions.execute(invocation)
						: invocation.action() instanceof com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayMovieAction
							? movieActions.execute(invocation)
							: isLifecycleAction(invocation.action()) ? lifecycleActions.execute(invocation)
								: isRecipeAction(invocation.action()) ? recipeBridge.execute(invocation)
									: actionExecutor.apply(invocation),
				itemActions::itemCount, itemActions::preflight, persistence);
		}

		/**
		 * 组合通用能力与正式制作技能奖励 typed bridge。
		 * Composes generic capabilities with the formal craft-skill reward typed bridge.
		 */
		public TransitionContext(int playerId, int accessLevel, ZoneId serverZoneId, PlayerQuestGraphStateList states,
				Function<ConditionInvocation, ConditionResult> conditionEvaluator, Function<ActionInvocation, PreflightResult> actionPreflight,
				Function<ActionInvocation, ActionResult> actionExecutor, QuestGraphCraftSkillRewardBridge craftSkillRewards,
				BiFunction<Long, PlayerQuestGraphState, PersistenceResult> persistence) {
			this(playerId, accessLevel, serverZoneId, states,
				craftConditionEvaluator(conditionEvaluator, craftSkillRewards),
				craftActionPreflight(actionPreflight, craftSkillRewards),
				craftActionExecutor(actionExecutor, craftSkillRewards),
				itemId -> -1, plans -> plans.isEmpty() ? PreflightResult.READY : PreflightResult.FAILED, persistence);
		}

		/**
		 * 组合正式物品、计时器、影片、lifecycle、recipe 与制作技能奖励 typed adapter。
		 * Composes production item, timer, movie, lifecycle, recipe, and craft-skill reward typed adapters.
		 */
		public TransitionContext(int playerId, int accessLevel, ZoneId serverZoneId, PlayerQuestGraphStateList states,
				Function<ConditionInvocation, ConditionResult> conditionEvaluator, Function<ActionInvocation, PreflightResult> actionPreflight,
				Function<ActionInvocation, ActionResult> actionExecutor, QuestGraphItemActionAdapter itemActions,
				QuestGraphTimerActionAdapter timerActions, QuestGraphMovieActionAdapter movieActions,
				QuestGraphLifecycleActionAdapter lifecycleActions, QuestGraphRecipeBridge recipeBridge,
				QuestGraphCraftSkillRewardBridge craftSkillRewards,
				BiFunction<Long, PlayerQuestGraphState, PersistenceResult> persistence) {
			this(playerId, accessLevel, serverZoneId, states,
				invocation -> invocation.condition() instanceof RecipeLearnableCondition
					? recipeBridge.evaluate(invocation)
					: invocation.condition() instanceof CraftSkillEligibilityCondition
						? craftSkillRewards.evaluate(invocation) : conditionEvaluator.apply(invocation),
				invocation -> invocation.itemMutationPlan() != null ? itemActions.preflight(invocation)
					: isRequiredTimerAction(invocation.action()) ? timerActions.preflight(invocation)
						: isLifecycleAction(invocation.action()) ? lifecycleActions.preflight(invocation)
							: isRequiredRecipeAction(invocation.action()) ? recipeBridge.preflight(invocation)
								: isRequiredCraftSkillRewardAction(invocation.action()) ? craftSkillRewards.preflight(invocation)
									: actionPreflight.apply(invocation),
				invocation -> invocation.itemMutationPlan() != null ? itemActions.execute(invocation)
					: isTimerAction(invocation.action()) ? timerActions.execute(invocation)
						: invocation.action() instanceof com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayMovieAction
							? movieActions.execute(invocation)
							: isLifecycleAction(invocation.action()) ? lifecycleActions.execute(invocation)
								: isRecipeAction(invocation.action()) ? recipeBridge.execute(invocation)
									: isCraftSkillRewardAction(invocation.action()) ? craftSkillRewards.execute(invocation)
										: actionExecutor.apply(invocation),
				itemActions::itemCount, itemActions::preflight, persistence);
		}

		/**
		 * 组合非 lifecycle 能力与正式任务生命周期 typed adapter。
		 * Composes non-lifecycle capabilities with the production quest-lifecycle typed adapter.
		 */
		public TransitionContext(int playerId, int accessLevel, ZoneId serverZoneId, PlayerQuestGraphStateList states,
				Function<ConditionInvocation, ConditionResult> conditionEvaluator, Function<ActionInvocation, PreflightResult> actionPreflight,
				Function<ActionInvocation, ActionResult> actionExecutor, QuestGraphLifecycleActionAdapter lifecycleActions,
				BiFunction<Long, PlayerQuestGraphState, PersistenceResult> persistence) {
			this(playerId, accessLevel, serverZoneId, states, conditionEvaluator,
				invocation -> isLifecycleAction(invocation.action()) ? lifecycleActions.preflight(invocation) : actionPreflight.apply(invocation),
				invocation -> isLifecycleAction(invocation.action()) ? lifecycleActions.execute(invocation) : actionExecutor.apply(invocation),
				itemId -> -1, plans -> plans.isEmpty() ? PreflightResult.READY : PreflightResult.FAILED, persistence);
		}

		/**
		 * 组合非物品能力与正式物品 adapter。
		 * Composes non-item capabilities with the production item adapter.
		 */
		public TransitionContext(int playerId, int accessLevel, ZoneId serverZoneId, PlayerQuestGraphStateList states,
				Function<ConditionInvocation, ConditionResult> conditionEvaluator, Function<ActionInvocation, PreflightResult> actionPreflight,
				Function<ActionInvocation, ActionResult> actionExecutor, QuestGraphItemActionAdapter itemActions,
				BiFunction<Long, PlayerQuestGraphState, PersistenceResult> persistence) {
			this(playerId, accessLevel, serverZoneId, states, conditionEvaluator,
				invocation -> invocation.itemMutationPlan() == null ? actionPreflight.apply(invocation) : itemActions.preflight(invocation),
				invocation -> invocation.itemMutationPlan() == null ? actionExecutor.apply(invocation) : itemActions.execute(invocation),
				itemActions::itemCount, itemActions::preflight, persistence);
		}

		/**
		 * 组合正式物品、计时器、影片和 lifecycle typed adapter。
		 * Composes production item, timer, movie, and lifecycle typed adapters.
		 */
		public TransitionContext(int playerId, int accessLevel, ZoneId serverZoneId, PlayerQuestGraphStateList states,
				Function<ConditionInvocation, ConditionResult> conditionEvaluator, Function<ActionInvocation, PreflightResult> actionPreflight,
				Function<ActionInvocation, ActionResult> actionExecutor, QuestGraphItemActionAdapter itemActions,
				QuestGraphTimerActionAdapter timerActions, QuestGraphMovieActionAdapter movieActions,
				QuestGraphLifecycleActionAdapter lifecycleActions, BiFunction<Long, PlayerQuestGraphState, PersistenceResult> persistence) {
			this(playerId, accessLevel, serverZoneId, states, conditionEvaluator,
				invocation -> invocation.itemMutationPlan() != null ? itemActions.preflight(invocation)
					: isRequiredTimerAction(invocation.action()) ? timerActions.preflight(invocation)
						: isLifecycleAction(invocation.action()) ? lifecycleActions.preflight(invocation) : actionPreflight.apply(invocation),
				invocation -> invocation.itemMutationPlan() != null ? itemActions.execute(invocation)
					: isTimerAction(invocation.action()) ? timerActions.execute(invocation)
						: invocation.action() instanceof com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayMovieAction
							? movieActions.execute(invocation)
							: isLifecycleAction(invocation.action()) ? lifecycleActions.execute(invocation) : actionExecutor.apply(invocation),
				itemActions::itemCount, itemActions::preflight, persistence);
		}

		/**
		 * 组合非物品能力与正式物品、任务计时器 typed adapter。
		 * Composes non-item capabilities with production item and quest-timer typed adapters.
		 */
		public TransitionContext(int playerId, int accessLevel, ZoneId serverZoneId, PlayerQuestGraphStateList states,
				Function<ConditionInvocation, ConditionResult> conditionEvaluator, Function<ActionInvocation, PreflightResult> actionPreflight,
				Function<ActionInvocation, ActionResult> actionExecutor, QuestGraphItemActionAdapter itemActions,
				QuestGraphTimerActionAdapter timerActions, BiFunction<Long, PlayerQuestGraphState, PersistenceResult> persistence) {
			this(playerId, accessLevel, serverZoneId, states, conditionEvaluator,
				invocation -> invocation.itemMutationPlan() != null ? itemActions.preflight(invocation)
					: isRequiredTimerAction(invocation.action()) ? timerActions.preflight(invocation) : actionPreflight.apply(invocation),
				invocation -> invocation.itemMutationPlan() != null ? itemActions.execute(invocation)
					: isTimerAction(invocation.action()) ? timerActions.execute(invocation) : actionExecutor.apply(invocation),
				itemActions::itemCount, itemActions::preflight, persistence);
		}

		/**
		 * 组合非物品能力与正式物品、计时器和影片 typed adapter。
		 * Composes non-item capabilities with production item, timer, and movie typed adapters.
		 */
		public TransitionContext(int playerId, int accessLevel, ZoneId serverZoneId, PlayerQuestGraphStateList states,
				Function<ConditionInvocation, ConditionResult> conditionEvaluator, Function<ActionInvocation, PreflightResult> actionPreflight,
				Function<ActionInvocation, ActionResult> actionExecutor, QuestGraphItemActionAdapter itemActions,
				QuestGraphTimerActionAdapter timerActions, QuestGraphMovieActionAdapter movieActions,
				BiFunction<Long, PlayerQuestGraphState, PersistenceResult> persistence) {
			this(playerId, accessLevel, serverZoneId, states, conditionEvaluator,
				invocation -> invocation.itemMutationPlan() != null ? itemActions.preflight(invocation)
					: isRequiredTimerAction(invocation.action()) ? timerActions.preflight(invocation) : actionPreflight.apply(invocation),
				invocation -> invocation.itemMutationPlan() != null ? itemActions.execute(invocation)
					: isTimerAction(invocation.action()) ? timerActions.execute(invocation)
						: invocation.action() instanceof com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayMovieAction
							? movieActions.execute(invocation) : actionExecutor.apply(invocation),
				itemActions::itemCount, itemActions::preflight, persistence);
		}

		/**
		 * 校验转换执行依赖；新状态写入时 persistence 的 expected revision 为 null。
		 * Validates transition dependencies; persistence receives a null expected revision for a new state.
		 */
		public TransitionContext {
			if (playerId <= 0 || accessLevel < 0) {
				throw new IllegalArgumentException("Player id/access level is invalid");
			}
			Objects.requireNonNull(serverZoneId, "serverZoneId");
			Objects.requireNonNull(states, "states");
			Objects.requireNonNull(conditionEvaluator, "conditionEvaluator");
			Objects.requireNonNull(actionPreflight, "actionPreflight");
			Objects.requireNonNull(actionExecutor, "actionExecutor");
			Objects.requireNonNull(itemCountReader, "itemCountReader");
			Objects.requireNonNull(itemMutationPreflight, "itemMutationPreflight");
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
		if (match.event().type() == CompiledQuestGraph.EventType.INTERACTION_ELIGIBILITY
				|| match.event().playerId() != context.playerId() || !supportsPlayerState(match.graph())) {
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
				PreflightOutcome preflight = preflight(match.graph(), match.route().transition(), match.event(), current, 0, Map.of(), context);
				if (preflight.status() != Status.APPLIED) {
					return preflight.status();
				}
				PlayerQuestGraphState prepared = prepare(match, current, context.serverZoneId(), context.accessLevel(), preflight.itemMutationPlans());
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
	 * 只读评估 actionless eligibility self-loop；不创建 journal、revision 或任何业务副作用。
	 * Read-only evaluates an actionless eligibility self-loop without creating a journal, revision, or business side effect.
	 */
	public static Status evaluateReadOnly(Match match, ReadOnlyContext context) {
		Objects.requireNonNull(match, "match");
		Objects.requireNonNull(context, "context");
		if (match.event().type() != CompiledQuestGraph.EventType.INTERACTION_ELIGIBILITY
				|| match.event().playerId() != context.playerId() || !supportsPlayerState(match.graph())) {
			return Status.FAILED;
		}
		synchronized (context.states()) {
			PlayerQuestGraphState current = context.states().get(match.graph().questId());
			String nodeId = current == null ? match.graph().initialNode() : current.getNodeId();
			if (current != match.state() || !match.route().nodeId().equals(nodeId)
					|| !match.route().transition().targetNode().equals(nodeId) || !match.route().transition().actions().isEmpty()) {
				return Status.FAILED;
			}
			return evaluateConditions(match, context.states(), context.conditionEvaluator());
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
			PlayerQuestGraphState state = context.states().get(graph.questId());
			if (state == null) {
				return Status.NO_MATCH;
			}
			if (state.getLifecycle() != Lifecycle.PREPARED) {
				return Status.FAILED;
			}
			try {
				if (state.getDefinitionVersion() != graph.version()) {
					return quarantinePrepared(context, state, "RECOVERY_DEFINITION_VERSION_MISMATCH");
				}
				CompiledQuestGraph.Node node = graph.nodes().get(state.getNodeId());
				if (node == null) {
					return quarantinePrepared(context, state, "RECOVERY_NODE_MISSING");
				}
				PreparedTransition journal = state.getJournal();
				Transition transition = node.transitions().stream()
					.filter(candidate -> candidate.id().equals(journal.getTransitionId())).findFirst().orElse(null);
				if (transition == null || journal.getNextActionIndex() > transition.actions().size()) {
					return quarantinePrepared(context, state, "RECOVERY_TRANSITION_INCOMPATIBLE");
				}
				if (!validItemMutationPlans(transition, journal.getItemMutationPlans())) {
					return quarantinePrepared(context, state, "RECOVERY_ITEM_PLAN_INCOMPATIBLE");
				}
				QuestGraphEvent event = QuestGraphEventCodec.decode(journal.getEventPayload());
				if (event.playerId() != context.playerId()
						|| (event instanceof QuestGraphEvent.ItemDialogEvent itemDialog && itemDialog.questId() != graph.questId())
						|| !journal.getEventId().equals(event.eventId()) || !matches(event, transition.event())) {
					return quarantinePrepared(context, state, "RECOVERY_EVENT_INCOMPATIBLE");
				}
				PreflightOutcome preflight = preflight(graph, transition, event, state, journal.getNextActionIndex(),
					journal.getItemMutationPlans(), context);
				if (preflight.unrecoverable()) {
					return quarantinePrepared(context, state, "RECOVERY_STATE_TRANSITION_INVALID");
				}
				return preflight.status() == Status.APPLIED ? resumePrepared(graph, transition, event, state, context) : preflight.status();
			} catch (RuntimeException e) {
				return quarantinePrepared(context, state, "RECOVERY_STATE_CORRUPT");
			}
		}
	}

	/**
	 * 评估全部只读条件，并将不匹配与能力失败分别映射为路由状态。
	 * Evaluates all read-only conditions and separately maps mismatch and capability failure to routing status.
	 */
	private static Status evaluateConditions(Match match, TransitionContext context) {
		return evaluateConditions(match, context.states(), context.conditionEvaluator());
	}

	/** 评估共享的 canonical 和 typed 条件集合。 / Evaluates the shared canonical and typed condition set. */
	private static Status evaluateConditions(Match match, PlayerQuestGraphStateList states,
			Function<ConditionInvocation, ConditionResult> conditionEvaluator) {
		QuestStatus questStatus = match.state() == null ? QuestStatus.NONE : match.state().getQuestStatus();
		for (Condition condition : match.route().transition().conditions()) {
			ConditionResult canonicalResult = evaluateCanonicalCondition(condition, match.graph().questId(), states, match.event());
			if (canonicalResult != null) {
				if (canonicalResult == ConditionResult.NOT_MATCHED) {
					return Status.NO_MATCH;
				}
				if (canonicalResult == ConditionResult.FAILED) {
					return Status.FAILED;
				}
				continue;
			}
			ConditionResult result;
			try {
				result = Objects.requireNonNull(conditionEvaluator
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
	 * 直接评估 canonical 任务状态条件；非 ACTIVE 引用显式失败，非 canonical 条件返回 null。
	 * Directly evaluates canonical quest-state conditions, fails non-active references, and returns null for other capabilities.
	 */
	private static ConditionResult evaluateCanonicalCondition(Condition condition, int ownerQuestId, PlayerQuestGraphStateList states,
		QuestGraphEvent event) {
		if (condition instanceof QuestStatusCondition statusCondition) {
			int questId = statusCondition.questId() == null ? ownerQuestId : statusCondition.questId();
			PlayerQuestGraphState state = states.get(questId);
			if (state != null && state.getLifecycle() != Lifecycle.ACTIVE) {
				return ConditionResult.FAILED;
			}
			QuestStatus status = state == null ? QuestStatus.NONE : state.getQuestStatus();
			return statusCondition.matches(status) ? ConditionResult.MATCHED : ConditionResult.NOT_MATCHED;
		}
		if (condition instanceof QuestRewardCondition rewardCondition) {
			PlayerQuestGraphState state = states.get(rewardCondition.questId());
			if (state != null && state.getLifecycle() != Lifecycle.ACTIVE) {
				return ConditionResult.FAILED;
			}
			return state != null && state.getHistory().completionCount() > 0
				&& state.getHistory().lastRewardIndex() == rewardCondition.rewardIndex()
					? ConditionResult.MATCHED : ConditionResult.NOT_MATCHED;
		}
		if (condition instanceof QuestCompletionCountCondition completionCondition) {
			PlayerQuestGraphState state = states.get(completionCondition.questId());
			if (state != null && state.getLifecycle() != Lifecycle.ACTIVE) {
				return ConditionResult.FAILED;
			}
			int completionCount = state == null ? 0 : state.getHistory().completionCount();
			return completionCondition.matches(completionCount) ? ConditionResult.MATCHED : ConditionResult.NOT_MATCHED;
		}
		if (condition instanceof QuestVariableCondition variableCondition) {
			PlayerQuestGraphState state = states.get(ownerQuestId);
			if (state == null) {
				return ConditionResult.NOT_MATCHED;
			}
			if (state.getLifecycle() != Lifecycle.ACTIVE
					|| !(state.getVariables().get(variableCondition.variable()) instanceof IntValue value)) {
				return ConditionResult.FAILED;
			}
			return compare(value.value(), variableCondition.operation(), variableCondition.value())
				? ConditionResult.MATCHED : ConditionResult.NOT_MATCHED;
		}
		if (condition instanceof PackedCounterCondition packedCondition) {
			PlayerQuestGraphState state = states.get(ownerQuestId);
			if (state == null) {
				return ConditionResult.NOT_MATCHED;
			}
			if (state.getLifecycle() != Lifecycle.ACTIVE) {
				return ConditionResult.FAILED;
			}
			try {
				int actual = packedCounterValue(state.getVariables(), packedCondition.variables(), packedCondition.radix());
				return compare(actual, packedCondition.operation(), packedCondition.value())
					? ConditionResult.MATCHED : ConditionResult.NOT_MATCHED;
			} catch (RuntimeException e) {
				return ConditionResult.FAILED;
			}
		}
		if (condition instanceof QuestRepeatAvailableCondition repeatCondition) {
			PlayerQuestGraphState state = states.get(ownerQuestId);
			if (state == null) {
				return repeatResult(true, repeatCondition);
			}
			if (state.getLifecycle() != Lifecycle.ACTIVE) {
				return ConditionResult.FAILED;
			}
			if (state.getQuestStatus() == QuestStatus.NONE) {
				return repeatResult(true, repeatCondition);
			}
			if (state.getQuestStatus() != QuestStatus.COMPLETE
					|| repeatCondition.maxCompletions() != 255
						&& state.getHistory().completionCount() >= repeatCondition.maxCompletions()) {
				return repeatResult(false, repeatCondition);
			}
				QuestHistory history = state.getHistory();
				if (repeatCondition.requiresDeadline()) {
					return switch (history.repeatDeadlineDisposition()) {
						case DEADLINE -> repeatResult(history.nextRepeatAt() <= event.occurredAt(), repeatCondition);
						case PRIVILEGED_BYPASS -> repeatResult(true, repeatCondition);
						case NOT_APPLICABLE -> ConditionResult.FAILED;
					};
				}
				if (history.repeatDeadlineDisposition() != RepeatDeadlineDisposition.NOT_APPLICABLE) {
					return ConditionResult.FAILED;
				}
				return repeatResult(true, repeatCondition);
		}
		return null;
	}

	/**
	 * 将实际重复资格与条件期望值比较。
	 * Compares actual repeat eligibility with the condition's expected value.
	 */
	private static ConditionResult repeatResult(boolean available, QuestRepeatAvailableCondition condition) {
		return available == condition.expectedAvailable() ? ConditionResult.MATCHED : ConditionResult.NOT_MATCHED;
	}

	/**
	 * 从指定动作位置预检全部剩余动作，不产生状态或业务副作用。
	 * Preflights every remaining action from the given index without state or business side effects.
	 */
	private static PreflightOutcome preflight(CompiledQuestGraph graph, Transition transition, QuestGraphEvent event, PlayerQuestGraphState current,
		int startIndex, Map<Integer, ItemMutationPlan> frozenPlans, TransitionContext context) {
		RepeatDeadlineResolution repeatResolution = current != null && current.getLifecycle() == Lifecycle.PREPARED
			? current.getJournal().getRepeatDeadlineResolution()
			: resolveRepeatDeadline(transition, event, context.serverZoneId(), context.accessLevel());
		ActionState preview = new ActionState(current == null ? QuestStatus.NONE : current.getQuestStatus(),
			current == null ? initialVariables(graph) : current.getVariables(), current == null ? QuestHistory.EMPTY : current.getHistory(),
			current == null ? Map.of() : current.getDeadlines(), current == null ? Map.of() : current.getCleanupLeases());
		Map<Integer, ItemMutationPlan> plans = new LinkedHashMap<>(frozenPlans);
		Map<Integer, Long> projectedCounts = new LinkedHashMap<>();
		for (int index = startIndex; index < transition.actions().size(); index++) {
			Action action = transition.actions().get(index);
			QuestStatus invocationStatus = preview.questStatus();
			Map<String, CleanupLease> invocationLeases = preview.cleanupLeases();
			try {
				preview = reduceActionState(graph, preview, action, event.occurredAt(), repeatResolution);
			} catch (RuntimeException e) {
				return new PreflightOutcome(Status.FAILED, Map.of(), true);
			}
			if (action.type().phase() != ActionPhase.REQUIRED && !isLifecycleAction(action)) {
				continue;
			}
			PreflightResult result;
			try {
				ItemMutationPlan plan = plans.get(index);
				if (isItemMutation(action) && plan == null) {
					plan = createItemMutationPlan(action, index, projectedCounts, context.itemCountReader());
					plans.put(index, plan);
				}
				result = Objects.requireNonNull(context.actionPreflight().apply(invocation(graph, transition, event,
					invocationStatus, index, repeatResolution, plan, invocationLeases)),
					"preflight result");
			} catch (ItemMutationRejectedException e) {
					return new PreflightOutcome(Status.REJECTED, Map.of(), false);
			} catch (RuntimeException e) {
					return new PreflightOutcome(Status.FAILED, Map.of(), false);
			}
			if (result == PreflightResult.REJECTED) {
					return new PreflightOutcome(Status.REJECTED, Map.of(), false);
			}
			if (result == PreflightResult.FAILED) {
					return new PreflightOutcome(Status.FAILED, Map.of(), false);
			}
		}
		PreflightResult itemResult;
		try {
			Map<Integer, ItemMutationPlan> remainingPlans = plans.entrySet().stream()
				.filter(entry -> entry.getKey() >= startIndex)
				.collect(java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
			itemResult = Objects.requireNonNull(context.itemMutationPreflight().apply(remainingPlans), "item mutation preflight result");
		} catch (RuntimeException e) {
			return new PreflightOutcome(Status.FAILED, Map.of(), false);
		}
		return switch (itemResult) {
			case READY -> new PreflightOutcome(Status.APPLIED, Map.copyOf(plans), false);
			case REJECTED -> new PreflightOutcome(Status.REJECTED, Map.of(), false);
			case FAILED -> new PreflightOutcome(Status.FAILED, Map.of(), false);
		};
	}

	/**
	 * 创建包含事件快照和初始变量的下一 revision PREPARED 状态。
	 * Creates the next-revision PREPARED state with an event snapshot and initialized variables.
	 */
	private static PlayerQuestGraphState prepare(Match match, PlayerQuestGraphState current, ZoneId serverZoneId, int accessLevel,
		Map<Integer, ItemMutationPlan> itemMutationPlans) {
		long baseRevision = current == null ? -1 : current.getRevision();
		long preparedRevision = Math.addExact(baseRevision, 1);
		Map<String, VariableValue> variables = current == null ? initialVariables(match.graph()) : current.getVariables();
		Map<String, Long> deadlines = current == null ? Map.of() : current.getDeadlines();
		Map<String, PlayerQuestGraphState.CleanupLease> leases = current == null ? Map.of() : current.getCleanupLeases();
		Long instanceRunId = current == null ? null : current.getInstanceRunId();
		String nodeId = current == null ? match.graph().initialNode() : current.getNodeId();
		RepeatDeadlineResolution repeatResolution = resolveRepeatDeadline(match.route().transition(), match.event(), serverZoneId, accessLevel);
		PreparedTransition journal = new PreparedTransition(baseRevision, match.event().eventId(), match.route().transition().id(), 0,
			repeatResolution, itemMutationPlans, QuestGraphEventCodec.encode(match.event()));
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
		RepeatDeadlineResolution repeatResolution = prepared.getJournal().getRepeatDeadlineResolution();
		int protocolStart = transition.actions().size();
		for (int index = prepared.getJournal().getNextActionIndex(); index < transition.actions().size(); index++) {
			Action action = transition.actions().get(index);
			if (action.type().phase() == ActionPhase.POST_COMMIT_PROTOCOL) {
				protocolStart = index;
				break;
			}
			if (action.type().phase() == ActionPhase.REQUIRED || isLifecycleAction(action)) {
				ActionResult result;
				try {
					result = Objects.requireNonNull(context.actionExecutor()
						.apply(invocation(graph, transition, event, current.getQuestStatus(), index, repeatResolution,
							current.getJournal().getItemMutationPlans().get(index), current.getCleanupLeases())), "action result");
				} catch (RuntimeException e) {
					return Status.FAILED;
				}
				if (result == ActionResult.REJECTED) {
					return Status.REJECTED;
				}
				if (result == ActionResult.FAILED) {
					return Status.FAILED;
				}
			}
			PlayerQuestGraphState reduced;
			try {
				reduced = reduceActionState(graph, current, action, event.occurredAt(), repeatResolution);
			} catch (RuntimeException e) {
				return Status.FAILED;
			}
			PlayerQuestGraphState progressed = copy(reduced, current.getRevision() + 1, current.getNodeId(), reduced.getQuestStatus(), Lifecycle.PREPARED,
				new PreparedTransition(current.getJournal().getBaseRevision(), event.eventId(), transition.id(), index + 1,
					repeatResolution, current.getJournal().getItemMutationPlans(), current.getJournal().getEventPayload()));
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
		for (int index = protocolStart; index < transition.actions().size(); index++) {
			try {
				context.actionExecutor().apply(invocation(graph, transition, event, committed.getQuestStatus(), index, repeatResolution, null,
					committed.getCleanupLeases()));
			} catch (RuntimeException ignored) {
				// Protocol is a post-commit projection; the owning adapter is responsible for logging/retry.
			}
		}
		return Status.APPLIED;
	}

	/**
	 * 构造包含动作序号的稳定幂等调用。
	 * Builds a stable idempotent invocation that includes the action index.
	 */
	private static ActionInvocation invocation(CompiledQuestGraph graph, Transition transition, QuestGraphEvent event,
		QuestStatus questStatus, int actionIndex, RepeatDeadlineResolution repeatDeadlineResolution, ItemMutationPlan itemMutationPlan,
		Map<String, CleanupLease> cleanupLeases) {
		String key = event.eventId().length() + ":" + event.eventId() + ':' + graph.questId() + ':' + transition.id() + ':'
			+ event.playerId() + ':' + actionIndex;
		return new ActionInvocation(transition.actions().get(actionIndex), graph.questId(), actionIndex, questStatus, event,
			repeatDeadlineResolution, itemMutationPlan, cleanupLeases, key);
	}

	/** 保存动作预检结果及准备写入 journal 的冻结物品计划。 / Holds preflight status and frozen item plans to persist in the journal. */
	private record PreflightOutcome(Status status, Map<Integer, ItemMutationPlan> itemMutationPlans, boolean unrecoverable) {
	}

	/** 判断动作是否为显式物品数量变更。 / Returns whether an action is an explicit item-count mutation. */
	private static boolean isItemMutation(Action action) {
		return action instanceof GiveQuestItemAction || action instanceof RemoveQuestItemAction;
	}

	/** 判断动作是否需要任务计时器 scheduler/cancel bridge。 / Returns whether an action requires the quest-timer scheduler/cancel bridge. */
	private static boolean isRequiredTimerAction(Action action) {
		return action instanceof StartQuestTimerAction || action instanceof EndQuestTimerAction;
	}

	/** 判断动作是否属于任务计时器 bridge，包括提交后协议。 / Returns whether an action belongs to the quest-timer bridge, including protocol. */
	private static boolean isTimerAction(Action action) {
		return isRequiredTimerAction(action) || action instanceof SyncQuestTimerAction;
	}

	/** 判断动作是否必须由封闭 lifecycle adapter 执行。 / Returns whether an action must execute through the closed lifecycle adapter. */
	private static boolean isLifecycleAction(Action action) {
		return action instanceof StartQuestAction || action instanceof StartEventQuestAction || action instanceof RemoveCollectedItemsAction
			|| action instanceof RemoveQuestWorkItemsAction
			|| action instanceof FinishQuestAction || action instanceof AbandonQuestAction;
	}

	/** 判断动作是否为 PREPARED 前必须验证的 recipe 持久化动作。 / Returns whether an action is a recipe persistence action that must preflight before PREPARED. */
	private static boolean isRequiredRecipeAction(Action action) {
		return action instanceof LearnRecipeAction || action instanceof DeleteRecipeAction;
	}

	/** 判断动作是否属于 recipe bridge，包括提交后拒绝协议。 / Returns whether an action belongs to the recipe bridge, including rejection protocol. */
	private static boolean isRecipeAction(Action action) {
		return isRequiredRecipeAction(action) || action instanceof NotifyRecipeRejectionAction;
	}

	/** 判断动作是否为 PREPARED 前必须验证的制作技能奖励。 / Returns whether an action is a craft-skill reward that must preflight before PREPARED. */
	private static boolean isRequiredCraftSkillRewardAction(Action action) {
		return action instanceof GrantCraftSkillRewardAction;
	}

	/** 判断动作是否属于制作技能奖励 bridge，包括提交后协议。 / Returns whether an action belongs to the craft-skill reward bridge, including protocol. */
	private static boolean isCraftSkillRewardAction(Action action) {
		return isRequiredCraftSkillRewardAction(action) || action instanceof SyncCraftSkillRewardAction;
	}

	/** 组合制作资格条件并立即校验 bridge 依赖。 / Composes craft eligibility and eagerly validates the bridge dependency. */
	private static Function<ConditionInvocation, ConditionResult> craftConditionEvaluator(
			Function<ConditionInvocation, ConditionResult> delegate, QuestGraphCraftSkillRewardBridge craftSkillRewards) {
		Objects.requireNonNull(delegate, "condition evaluator");
		Objects.requireNonNull(craftSkillRewards, "craft skill rewards");
		return invocation -> invocation.condition() instanceof CraftSkillEligibilityCondition
			? craftSkillRewards.evaluate(invocation) : delegate.apply(invocation);
	}

	/** 组合制作 grant 预检并立即校验 bridge 依赖。 / Composes craft-grant preflight and eagerly validates the bridge dependency. */
	private static Function<ActionInvocation, PreflightResult> craftActionPreflight(
			Function<ActionInvocation, PreflightResult> delegate, QuestGraphCraftSkillRewardBridge craftSkillRewards) {
		Objects.requireNonNull(delegate, "action preflight");
		Objects.requireNonNull(craftSkillRewards, "craft skill rewards");
		return invocation -> isRequiredCraftSkillRewardAction(invocation.action())
			? craftSkillRewards.preflight(invocation) : delegate.apply(invocation);
	}

	/** 组合制作 required/protocol 执行并立即校验 bridge 依赖。 / Composes craft required/protocol execution and eagerly validates the bridge dependency. */
	private static Function<ActionInvocation, ActionResult> craftActionExecutor(
			Function<ActionInvocation, ActionResult> delegate, QuestGraphCraftSkillRewardBridge craftSkillRewards) {
		Objects.requireNonNull(delegate, "action executor");
		Objects.requireNonNull(craftSkillRewards, "craft skill rewards");
		return invocation -> isCraftSkillRewardAction(invocation.action())
			? craftSkillRewards.execute(invocation) : delegate.apply(invocation);
	}

	/**
	 * 从当前或同一转换的投影数量创建确定性物品计划。
	 * Creates a deterministic item plan from current or same-transition projected counts.
	 */
	private static ItemMutationPlan createItemMutationPlan(Action action, int actionIndex, Map<Integer, Long> projectedCounts,
		ToLongFunction<Integer> itemCountReader) {
		int itemId = action instanceof GiveQuestItemAction give ? give.itemId() : ((RemoveQuestItemAction) action).itemId();
		long before = projectedCounts.computeIfAbsent(itemId, id -> itemCountReader.applyAsLong(id));
		if (before < 0) {
			throw new IllegalStateException("Item count reader does not support quest-item actions");
		}
		ItemMutationPlan plan;
		if (action instanceof GiveQuestItemAction give) {
			ItemMutationKind kind = switch (give.mode()) {
				case TOP_UP_TO -> ItemMutationKind.GIVE_TOP_UP_TO;
				case ADD_EXACT -> ItemMutationKind.GIVE_ADD_EXACT;
			};
			long after = switch (give.mode()) {
				case TOP_UP_TO -> Math.max(before, give.count());
				case ADD_EXACT -> Math.addExact(before, give.count());
			};
			plan = new ItemMutationPlan(actionIndex, kind, itemId, give.count(), before, after);
		} else {
			RemoveQuestItemAction remove = (RemoveQuestItemAction) action;
			if (before < remove.count() && remove.mode() == CompiledQuestGraph.QuestItemRemovalMode.EXACT) {
				throw new ItemMutationRejectedException();
			}
			ItemMutationKind kind = switch (remove.mode()) {
				case EXACT -> ItemMutationKind.REMOVE_EXACT;
				case OPTIONAL_EXACT -> ItemMutationKind.REMOVE_OPTIONAL_EXACT;
			};
			long after = before >= remove.count() ? Math.subtractExact(before, remove.count()) : before;
			plan = new ItemMutationPlan(actionIndex, kind, itemId, remove.count(), before, after);
		}
		projectedCounts.put(itemId, plan.afterCount());
		return plan;
	}

	/** 校验 journal 中每个且仅有的物品计划与编译动作一致。 / Validates that the journal has exactly one matching plan per item action. */
	private static boolean validItemMutationPlans(Transition transition, Map<Integer, ItemMutationPlan> plans) {
		for (int index = 0; index < transition.actions().size(); index++) {
			Action action = transition.actions().get(index);
			ItemMutationPlan plan = plans.get(index);
			if (!isItemMutation(action)) {
				if (plan != null) {
					return false;
				}
				continue;
			}
			if (plan == null || action instanceof GiveQuestItemAction give
					&& (plan.kind() != switch (give.mode()) {
						case TOP_UP_TO -> ItemMutationKind.GIVE_TOP_UP_TO;
						case ADD_EXACT -> ItemMutationKind.GIVE_ADD_EXACT;
					} || plan.itemId() != give.itemId() || plan.requestedCount() != give.count())
					|| action instanceof RemoveQuestItemAction remove
						&& (plan.kind() != switch (remove.mode()) {
							case EXACT -> ItemMutationKind.REMOVE_EXACT;
							case OPTIONAL_EXACT -> ItemMutationKind.REMOVE_OPTIONAL_EXACT;
						}
							|| plan.itemId() != remove.itemId() || plan.requestedCount() != remove.count())) {
				return false;
			}
		}
		return plans.size() == transition.actions().stream().filter(QuestGraphTransitionExecutor::isItemMutation).count();
	}

	/** 标记物品动作的可预期业务拒绝，避免把库存不足误报为基础设施失败。 / Marks an expected item-action rejection. */
	private static final class ItemMutationRejectedException extends RuntimeException {
	}

	/**
	 * 将已成功动作归约到 canonical 任务状态；未知或非法状态组合显式失败。
	 * Reduces a successful action into canonical quest status and explicitly fails unknown or illegal combinations.
	 */
	private static ActionState reduceActionState(CompiledQuestGraph graph, ActionState current, Action action, long occurredAt,
		RepeatDeadlineResolution repeatDeadlineResolution) {
		Map<String, VariableValue> variables = new LinkedHashMap<>(current.variables());
		Map<String, Long> deadlines = new LinkedHashMap<>(current.deadlines());
		Map<String, CleanupLease> cleanupLeases = new LinkedHashMap<>(current.cleanupLeases());
		QuestStatus status = current.questStatus();
		QuestHistory history = current.history();
		switch (action) {
			case StartQuestAction ignored -> {
				if (status == QuestStatus.REWARD) {
					throw new IllegalStateException("Cannot start a quest awaiting reward");
				}
				if (status != QuestStatus.START) {
					variables = new LinkedHashMap<>(initialVariables(graph));
				}
				status = QuestStatus.START;
				deadlines.clear();
				cleanupLeases.clear();
			}
			case StartEventQuestAction startEvent -> {
				if (startEvent.targetQuestId() == graph.questId()) {
					status = startEvent.status();
					variables = new LinkedHashMap<>(initialVariables(graph));
					deadlines.clear();
					cleanupLeases.clear();
				}
			}
			case AbandonQuestAction ignored -> {
				if (status == QuestStatus.NONE || status == QuestStatus.COMPLETE) {
					throw new IllegalStateException("Only an active quest can be abandoned");
				}
				status = QuestStatus.NONE;
				variables = new LinkedHashMap<>(initialVariables(graph));
				deadlines.clear();
				cleanupLeases.clear();
			}
			case SetQuestStatusAction set -> {
				if (set.status() == QuestStatus.REWARD && status != QuestStatus.START) {
					throw new IllegalStateException("Only a started quest can become rewardable");
				}
				status = set.status();
				if (status == QuestStatus.COMPLETE && history.completionCount() == 0) {
					history = completionHistory(history, 1, occurredAt);
				}
			}
			case SetQuestVariableAction set -> variables.put(set.variable(), checkedIntValue(graph, set.variable(), set.value()));
			case AddQuestVariableAction add -> {
				if (!(variables.get(add.variable()) instanceof IntValue value)) {
					throw new IllegalStateException("Missing INT variable " + add.variable());
				}
				variables.put(add.variable(), checkedIntValue(graph, add.variable(), Math.addExact(value.value(), add.delta())));
			}
			case IncrementPackedCounterAction increment -> {
				int currentValue = packedCounterValue(variables, increment.variables(), increment.radix());
				if (currentValue >= increment.maximum()) {
					throw new IllegalStateException("Packed counter has reached its maximum");
				}
				int remaining = Math.addExact(currentValue, 1);
				for (String variable : increment.variables()) {
					variables.put(variable, checkedIntValue(graph, variable, remaining % increment.radix()));
					remaining /= increment.radix();
				}
				if (remaining != 0) {
					throw new IllegalStateException("Packed counter increment exceeds declared digits");
				}
			}
			case SetCompletionCountAction set -> history = completionHistory(history, set.count(), occurredAt);
			case AddCompletionCountAction add ->
				history = completionHistory(history, Math.addExact(history.completionCount(), add.delta()), occurredAt);
			case FinishQuestAction finish -> {
				if (status != QuestStatus.REWARD) {
					throw new IllegalStateException("Only a rewardable quest can finish");
				}
				status = QuestStatus.COMPLETE;
				variables = new LinkedHashMap<>(initialVariables(graph));
				if (!matchesRepeatResolution(finish, repeatDeadlineResolution)) {
					throw new IllegalStateException("Resolved repeat deadline does not match finish policy");
				}
				history = new QuestHistory(Math.addExact(history.completionCount(), 1), finish.rewardIndex(), occurredAt,
					repeatDeadlineResolution.deadlineAt(), repeatDeadlineResolution.disposition());
				deadlines.clear();
				cleanupLeases.clear();
			}
			case StartQuestTimerAction timer ->
				deadlines.put(timer.timer(), Math.addExact(occurredAt, Math.multiplyExact(timer.durationSeconds(), 1000)));
			case EndQuestTimerAction timer -> deadlines.remove(timer.timer());
			default -> {
			}
		}
		if (status == QuestStatus.COMPLETE && history.completionCount() == 0) {
			throw new IllegalStateException("COMPLETE state requires completion history");
		}
		return new ActionState(status, variables, history, deadlines, cleanupLeases);
	}

	/** 使用当前状态执行一个内部 state reduction。 / Applies one internal state reduction to the current state. */
	private static PlayerQuestGraphState reduceActionState(CompiledQuestGraph graph, PlayerQuestGraphState current, Action action,
		long occurredAt, RepeatDeadlineResolution repeatDeadlineResolution) {
		ActionState reduced = reduceActionState(graph,
			new ActionState(current.getQuestStatus(), current.getVariables(), current.getHistory(), current.getDeadlines(), current.getCleanupLeases()),
			action, occurredAt,
			repeatDeadlineResolution);
		return new PlayerQuestGraphState(current.getQuestId(), current.getDefinitionVersion(), current.getRevision(), current.getNodeId(),
			reduced.questStatus(), reduced.history(), current.getInstanceRunId(), current.getLifecycle(), reduced.variables(), reduced.deadlines(),
			current.getJournal(), reduced.cleanupLeases(), current.getQuarantineReason());
	}

	/**
	 * 用显式完成次数构造一致 history；首次非零写入使用事件时间作为稳定完成时间。
	 * Builds consistent history for an explicit completion count, using event time for the first non-zero write.
	 */
	private static QuestHistory completionHistory(QuestHistory current, int count, long occurredAt) {
		if (count < 0) {
			throw new IllegalStateException("Completion count cannot be negative");
		}
		if (count == 0) {
			return QuestHistory.EMPTY;
		}
		if (current.completionCount() == 0) {
			return new QuestHistory(count, 0, occurredAt, null, RepeatDeadlineDisposition.NOT_APPLICABLE);
		}
		return new QuestHistory(count, current.lastRewardIndex(), current.completedAt(), current.nextRepeatAt(),
			current.repeatDeadlineDisposition());
	}

	/** 校验冻结结果与 finish policy 的 deadline/权限合同一致。 / Validates the frozen outcome against the finish policy's deadline/privilege contract. */
	private static boolean matchesRepeatResolution(FinishQuestAction finish, RepeatDeadlineResolution resolution) {
		if (finish.repeatDeadlinePolicy() == NoRepeatDeadlinePolicy.INSTANCE) {
			return resolution.disposition() == RepeatDeadlineDisposition.NOT_APPLICABLE;
		}
		return switch (resolution.disposition()) {
			case NOT_APPLICABLE -> false;
			case DEADLINE -> true;
			case PRIVILEGED_BYPASS -> finish.repeatDeadlinePolicy().privilegeMode() == RepeatPrivilegeMode.BYPASS_FOR_PRIVILEGED;
		};
	}

	/**
	 * 在写入 PREPARED 前解析唯一 finish action 的 repeat deadline。
	 * Resolves the unique finish action's repeat deadline before PREPARED is persisted.
	 */
	private static RepeatDeadlineResolution resolveRepeatDeadline(Transition transition, QuestGraphEvent event, ZoneId serverZoneId,
		int accessLevel) {
		FinishQuestAction finish = null;
		for (Action action : transition.actions()) {
			if (!(action instanceof FinishQuestAction candidate)) {
				continue;
			}
			if (finish != null) {
				throw new IllegalStateException("Transition contains multiple finish actions");
			}
			finish = candidate;
		}
		return finish == null ? RepeatDeadlineResolution.NOT_APPLICABLE
			: QuestRepeatDeadlineCalculator.calculate(finish.repeatDeadlinePolicy(), event.occurredAt(), serverZoneId, accessLevel);
	}

	/** 校验整数变量写入仍位于声明边界。 / Validates that an integer-variable write remains within declared bounds. */
	private static IntValue checkedIntValue(CompiledQuestGraph graph, String name, int value) {
		if (!(graph.variables().get(name) instanceof IntVariable variable) || value < variable.min() || value > variable.max()) {
			throw new IllegalStateException("INT variable write is outside declared bounds: " + name);
		}
		return new IntValue(value);
	}

	/**
	 * 从低位到高位强类型变量解码定基数计数器，并拒绝缺失、坏位值和整数溢出。
	 * Decodes a fixed-radix counter from low-to-high typed variables, rejecting missing digits, invalid values, and overflow.
	 */
	private static int packedCounterValue(Map<String, VariableValue> values, java.util.List<String> variables, int radix) {
		long total = 0;
		long multiplier = 1;
		for (String variable : variables) {
			if (!(values.get(variable) instanceof IntValue digit) || digit.value() < 0 || digit.value() >= radix) {
				throw new IllegalStateException("Packed counter contains an invalid digit " + variable);
			}
			total = Math.addExact(total, Math.multiplyExact(digit.value(), multiplier));
			if (total > Integer.MAX_VALUE) {
				throw new IllegalStateException("Packed counter exceeds INT range");
			}
			multiplier = Math.multiplyExact(multiplier, radix);
		}
		return (int) total;
	}

	/** 比较 canonical 整数变量。 / Compares a canonical integer variable. */
	private static boolean compare(int actual, com.aionemu.gameserver.questEngine.model.ConditionOperation operation, int expected) {
		return switch (operation) {
			case EQUAL -> actual == expected;
			case GREATER -> actual > expected;
			case GREATER_EQUAL -> actual >= expected;
			case LESSER -> actual < expected;
			case LESSER_EQUAL -> actual <= expected;
			case NOT_EQUAL -> actual != expected;
			case IN, NOT_IN -> throw new IllegalStateException("Set operation is invalid for a quest variable");
		};
	}

	/** 保存 preflight 使用的纯状态预览。 / Holds the pure state preview used during preflight. */
	private record ActionState(QuestStatus questStatus, Map<String, VariableValue> variables, QuestHistory history, Map<String, Long> deadlines,
		Map<String, CleanupLease> cleanupLeases) {
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
	 * 通过 revision CAS 将不可恢复的 PREPARED 状态持久化为隔离状态。
	 * Persists an unrecoverable PREPARED state as quarantined through revision CAS.
	 */
	private static Status quarantinePrepared(TransitionContext context, PlayerQuestGraphState state, String reason) {
		if (state.getLifecycle() != Lifecycle.PREPARED || state.getRevision() == Long.MAX_VALUE) {
			return Status.FAILED;
		}
		PlayerQuestGraphState quarantined = new PlayerQuestGraphState(state.getQuestId(), state.getDefinitionVersion(), state.getRevision() + 1,
			state.getNodeId(), state.getQuestStatus(), state.getHistory(), state.getInstanceRunId(), Lifecycle.QUARANTINED,
			state.getVariables(), state.getDeadlines(), null, state.getCleanupLeases(), reason);
		if (persist(context, state.getRevision(), quarantined) != PersistenceResult.APPLIED) {
			return Status.FAILED;
		}
		context.states().put(quarantined);
		return Status.FAILED;
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
		if (event.type() != expected.type() || event.targetId() != expected.targetId()) {
			return false;
		}
		return switch (event) {
			case QuestGraphEvent.DialogEvent dialog -> Objects.equals(dialog.dialog(), expected.qualifier());
			case QuestGraphEvent.ItemDialogEvent itemDialog -> Objects.equals(itemDialog.dialog(), expected.qualifier());
			case QuestGraphEvent.ZoneEnteredEvent zoneEntered -> Objects.equals(zoneEntered.zoneName(), expected.qualifier());
			case QuestGraphEvent.ZoneLeftEvent zoneLeft -> Objects.equals(zoneLeft.zoneName(), expected.qualifier());
			default -> true;
		};
	}
}
