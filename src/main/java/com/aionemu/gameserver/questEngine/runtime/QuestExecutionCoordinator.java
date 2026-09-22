package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * 按快照、计划、事务和提交后动作的固定顺序执行一个 owner。
 * Runs one owner under the required snapshot/plan/transaction/afterCommit sequence.
 */
public final class QuestExecutionCoordinator {
	private final PlayerSerialExecutor serialExecutor;
	private final BiConsumer<Integer, Integer> terminalCleanup;

	public QuestExecutionCoordinator(PlayerSerialExecutor serialExecutor) {
		this(serialExecutor, QuestRuntimeResources::cleanupQuest);
	}

	QuestExecutionCoordinator(PlayerSerialExecutor serialExecutor, BiConsumer<Integer, Integer> terminalCleanup) {
		this.serialExecutor = serialExecutor;
		this.terminalCleanup = Objects.requireNonNull(terminalCleanup, "terminalCleanup");
	}

	public QuestExecutionResult execute(Connection connection, int playerId,
			CompiledQuestDefinition definition, QuestEvent event, QuestTransition transition,
			QuestEventPort eventPort, QuestActionPort actionPort, QuestStatePort statePort,
			QuestAfterCommitPort afterCommitPort) throws Exception {
		Objects.requireNonNull(connection, "connection");
		return execute(() -> connection, playerId, definition, event, transition, eventPort, actionPort,
			statePort, afterCommitPort);
	}

	QuestExecutionResult execute(Supplier<Connection> connections, int playerId,
			CompiledQuestDefinition definition, QuestEvent event, QuestTransition transition,
			QuestEventPort eventPort, QuestActionPort actionPort, QuestStatePort statePort,
			QuestAfterCommitPort afterCommitPort) throws Exception {
		return executeValidated(connections, playerId, definition, event, transition, eventPort, actionPort,
			statePort, afterCommitPort, QuestEvent.matches(transition.event(), event), false, false);
	}

	/**
	 * 执行奖励窗口确认动作的恢复路由：调用方已按 questId + action 选定转换，允许事件携带的交互对象
	 * 不是完成路由绑定的 NPC。
	 * Executes the recovery route of a reward-window confirmation action: the caller already selected the
	 * transition by questId + action, so the event's interaction object is allowed to differ from the
	 * completion route's NPC.
	 * <p>玩家状态、条件与动作可行性仍由 {@link QuestMutationPlanner} 按真实快照校验，
	 * 未满足来源状态/条件时不会提交。</p>
	 * <p>The player's state, conditions, and action feasibility are still validated against the real
	 * snapshot by {@link QuestMutationPlanner}; nothing commits when the source state or conditions do
	 * not hold.</p>
	 */
	QuestExecutionResult executeRewardWindowAction(Supplier<Connection> connections, int playerId,
			CompiledQuestDefinition definition, QuestEvent event, QuestTransition transition,
			QuestEventPort eventPort, QuestActionPort actionPort, QuestStatePort statePort,
			QuestAfterCommitPort afterCommitPort) throws Exception {
		return executeValidated(connections, playerId, definition, event, transition, eventPort, actionPort,
			statePort, afterCommitPort, false, false, true);
	}

	QuestExecutionResult executeSharedQuestAccept(Connection connection, int playerId,
			CompiledQuestDefinition definition, QuestEvent.QuestDialog event, QuestTransition transition,
			QuestEventPort eventPort, QuestActionPort actionPort, QuestStatePort statePort,
			QuestAfterCommitPort afterCommitPort) throws Exception {
		Objects.requireNonNull(connection, "connection");
		return executeSharedQuestAccept(() -> connection, playerId, definition, event, transition, eventPort,
			actionPort, statePort, afterCommitPort);
	}

	QuestExecutionResult executeSharedQuestAccept(Supplier<Connection> connections, int playerId,
			CompiledQuestDefinition definition, QuestEvent.QuestDialog event, QuestTransition transition,
			QuestEventPort eventPort, QuestActionPort actionPort, QuestStatePort statePort,
			QuestAfterCommitPort afterCommitPort) throws Exception {
		boolean matchesSharedAccept = transition.event() instanceof QuestEvent.TalkToNpc talk
			&& talk.dialogId() != null && talk.dialogId() == event.dialogId();
		return executeValidated(connections, playerId, definition, event, transition, eventPort, actionPort,
			statePort, afterCommitPort, matchesSharedAccept, true, false);
	}

	private QuestExecutionResult executeValidated(Supplier<Connection> connections, int playerId,
			CompiledQuestDefinition definition, QuestEvent event, QuestTransition transition,
			QuestEventPort eventPort, QuestActionPort actionPort, QuestStatePort statePort,
			QuestAfterCommitPort afterCommitPort, boolean eventMatches, boolean sharedQuestAccept,
			boolean unpinnedRewardWindow) throws Exception {
		// 统一入口保证所有正式 owner 使用同一执行顺序。
		// The single entry point guarantees one execution order for every production owner.
		Objects.requireNonNull(connections, "connections");
		if (playerId <= 0) {
			throw new IllegalArgumentException("playerId must be positive");
		}
		Objects.requireNonNull(definition, "definition");
		Objects.requireNonNull(event, "event");
		Objects.requireNonNull(transition, "transition");
		if (!definition.definition().transitions().contains(transition)) {
			throw new IllegalArgumentException("transition does not belong to definition " + definition.id());
		}
		if (!eventMatches && !unpinnedRewardWindow) {
			throw new IllegalArgumentException("event does not match transition");
		}
		Objects.requireNonNull(eventPort, "eventPort");
		Objects.requireNonNull(actionPort, "actionPort");
		Objects.requireNonNull(statePort, "statePort");
		Objects.requireNonNull(afterCommitPort, "afterCommitPort");
		return serialExecutor.execute(playerId, () -> executeSerialized(connections, playerId, definition, event,
				transition, eventPort, actionPort, statePort, afterCommitPort, sharedQuestAccept,
				unpinnedRewardWindow));
	}

	private QuestExecutionResult executeSerialized(Supplier<Connection> connections, int playerId,
			CompiledQuestDefinition definition, QuestEvent event, QuestTransition transition,
			QuestEventPort eventPort, QuestActionPort actionPort, QuestStatePort statePort,
			QuestAfterCommitPort afterCommitPort, boolean sharedQuestAccept,
			boolean unpinnedRewardWindow) throws Exception {
		QuestTransactionParticipant participant = QuestTransactionParticipant.none();
		QuestMutationPlan appliedPlan = null;
		boolean committed = false;
		QuestFailureStage stage = QuestFailureStage.SNAPSHOT;
		List<RuntimeException> committedFailures = new ArrayList<>();
		try {
			stage = QuestFailureStage.SNAPSHOT;
			// 事实需求由 transition 静态推导：不读的事实族不会被采集（读取方 fail-closed）。
			// Fact requirements are derived from the transition: families it never reads are never captured,
			// and reading them fails closed.
			QuestFactRequirements requirements = QuestFactRequirements.of(definition, event, transition);
			QuestSnapshot snapshot = eventPort.snapshot(playerId, definition.id(), event, requirements);
			if (snapshot == null) {
				throw new IllegalStateException("event port returned no snapshot");
			}
			if (snapshot.playerId() != playerId || snapshot.questId() != definition.id()) {
				throw new IllegalStateException("event snapshot does not belong to player/quest");
			}
			stage = QuestFailureStage.PLAN;
			Optional<QuestMutationPlan> plan = sharedQuestAccept
				? QuestMutationPlanner.planSharedQuestAccept(definition, snapshot,
					(QuestEvent.QuestDialog) event, transition)
				: unpinnedRewardWindow
					? QuestMutationPlanner.planValidatedTransition(definition, snapshot, event, transition)
					: QuestMutationPlanner.plan(definition, snapshot, event, transition);
			if (plan.isEmpty()) {
				return new QuestExecutionResult(QuestExecutionStatus.NO_MATCH, null, List.of());
			}
			QuestMutationPlan resolved = plan.orElseThrow();
			var durableActions = resolved.requiredActions().stream()
					.filter(action -> !(action instanceof QuestAction.SetVariable)
						&& !(action instanceof QuestAction.SetStatus)
						&& !(action instanceof QuestAction.CompleteQuest)
						&& !(action instanceof QuestAction.BlockDefaultItemUse)
						&& !(action instanceof QuestAction.AbandonQuest))
					.toList();
			boolean persistState = requiresStatePersistence(snapshot, resolved);
			if (!persistState && durableActions.isEmpty() && isProtocolOnly(resolved.afterCommit())) {
				return executeProtocolOnly(snapshot, resolved, afterCommitPort);
			}
			stage = QuestFailureStage.PREFLIGHT;
			Connection connection = Objects.requireNonNull(connections.get(), "connection provider returned null");
			try (QuestUnitOfWork unit = QuestUnitOfWork.open(connection)) {
				if (!durableActions.isEmpty()) {
					actionPort.preflight(unit.connection(), snapshot, durableActions);
					stage = QuestFailureStage.APPLY_ACTIONS;
					participant = actionPort.apply(unit.connection(), snapshot, durableActions);
				}
				if (persistState) {
					stage = QuestFailureStage.APPLY_STATE;
					statePort.apply(unit.connection(), playerId, resolved);
					appliedPlan = resolved;
				}
				for (AfterCommitAction action : withoutRedundantStateSync(resolved.afterCommit(), persistState,
						durableActions.isEmpty())) {
					unit.afterCommit(() -> {
						try {
							afterCommitPort.execute(action, snapshot, resolved);
						} catch (RuntimeException failure) {
							RuntimeException actionFailure = failure instanceof QuestAfterCommitException
								? failure : new QuestAfterCommitException(action, snapshot, failure);
							throw new QuestPostCommitFailure(QuestFailureStage.AFTER_COMMIT, actionFailure);
						}
					});
				}
				if (persistState
						&& (resolved.nextStatus() == QuestStatus.COMPLETE || resolved.nextStatus() == QuestStatus.NONE)) {
					// 清理也是有序的 best-effort 提交后动作。最后注册它，使领域效果仍可在拆除前解析其任务拥有的资源。
					// Cleanup is an ordered, best-effort post-commit action too. Register it last so
					// domain effects can still resolve their quest-owned resources before teardown.
					unit.afterCommit(() -> {
						try {
							terminalCleanup.accept(playerId, resolved.questId());
						} catch (RuntimeException failure) {
							throw new QuestPostCommitFailure(QuestFailureStage.AFTER_COMMIT, failure);
						}
					});
				}
				stage = QuestFailureStage.COMMIT;
				unit.commit();
				committed = true;
				// 只有提交成功后内存才前进；commit 失败时 publish 不执行，内存保持事件前值。
				// Only after a successful commit does live memory advance; publish never runs on a failed commit, so memory stays at the pre-event value.
				// required participant 先清理已提交的 dirty 状态，再发布 quest state 和协议动作。
				// The required participant first clears committed dirty state, then publishes quest state and protocol actions.
				stage = QuestFailureStage.PARTICIPANT_AFTER_COMMIT;
				try {
					participant.afterCommit();
				} catch (RuntimeException failure) {
					committedFailures.add(new QuestPostCommitFailure(QuestFailureStage.PARTICIPANT_AFTER_COMMIT, failure));
				}
				if (persistState) {
					stage = QuestFailureStage.STATE_PUBLISH;
					try {
						statePort.publish(playerId, resolved);
					} catch (RuntimeException publishFailure) {
						try {
							stage = QuestFailureStage.STATE_RESYNC;
							statePort.resynchronize(playerId, resolved);
						} catch (RuntimeException resyncFailure) {
							publishFailure.addSuppressed(resyncFailure);
							throw new QuestExecutionFailureException(QuestFailureStage.STATE_RESYNC, true,
								publishFailure);
						}
						committedFailures.add(new QuestPostCommitFailure(QuestFailureStage.STATE_PUBLISH,
							publishFailure));
					}
				}
				stage = QuestFailureStage.AFTER_COMMIT;
				unit.runAfterCommit();
				committedFailures.addAll(unit.afterCommitFailures());
				return new QuestExecutionResult(QuestExecutionStatus.COMMITTED, resolved, committedFailures);
			}
		} catch (QuestExecutionFailureException failure) {
			throw failure;
		} catch (Exception failure) {
			if (!committed) {
				if (appliedPlan != null) {
					try {
						statePort.rollback(playerId, appliedPlan);
					} catch (RuntimeException rollbackFailure) {
						failure.addSuppressed(rollbackFailure);
					}
				}
				try {
					participant.afterRollback();
				} catch (RuntimeException rollbackFailure) {
					failure.addSuppressed(rollbackFailure);
				}
			}
			throw new QuestExecutionFailureException(stage, committed, failure);
		}
	}

	/**
	 * 状态未变化且没有任何必需动作时，任务状态更新包携带的信息与客户端已知状态完全一致，
	 * 但客户端会把它渲染成一次"任务更新"通知（额外击杀已饱和计数器时最明显）。
	 * 这类多余的状态同步在此丢弃；只要状态或持久副作用发生变化，同步照常下发。
	 * When the persisted state is unchanged and no required action ran, the quest-state update packet repeats the
	 * state the client already knows while the client renders it as an unsolicited "quest updated" notice (most
	 * visible when a saturated kill counter is hit again). Such redundant syncs are dropped; any real state or
	 * durable effect change keeps its sync.
	 */
	private static List<AfterCommitAction> withoutRedundantStateSync(List<AfterCommitAction> actions,
			boolean stateChanged, boolean requiredActionsEmpty) {
		if (stateChanged || !requiredActionsEmpty) {
			return actions;
		}
		return actions.stream()
			.filter(action -> !(action instanceof AfterCommitAction.SyncQuestState))
			.toList();
	}

	private static QuestExecutionResult executeProtocolOnly(QuestSnapshot snapshot, QuestMutationPlan plan,
			QuestAfterCommitPort afterCommitPort) {
		List<RuntimeException> failures = new ArrayList<>();
		for (AfterCommitAction action : plan.afterCommit()) {
			try {
				afterCommitPort.execute(action, snapshot, plan);
			} catch (RuntimeException failure) {
				RuntimeException actionFailure = failure instanceof QuestAfterCommitException
					? failure : new QuestAfterCommitException(action, snapshot, failure);
				failures.add(new QuestPostCommitFailure(QuestFailureStage.AFTER_COMMIT, actionFailure));
			}
		}
		return new QuestExecutionResult(QuestExecutionStatus.COMMITTED, plan, failures);
	}

	private static boolean isProtocolOnly(List<AfterCommitAction> actions) {
		return actions.stream().allMatch(action -> action instanceof AfterCommitAction.CloseDialog
			|| action instanceof AfterCommitAction.ShowQuestDialog
			|| action instanceof AfterCommitAction.ShowQuestSelectionDialog
			|| action instanceof AfterCommitAction.ShowDialogWindow);
	}

	static boolean requiresWorldFacts(QuestTransition transition) {
		return transition.conditions().stream().anyMatch(condition -> condition instanceof QuestCondition.WorldNpcIs
			|| condition instanceof QuestCondition.ZoneIs);
	}

	private static boolean requiresStatePersistence(QuestSnapshot snapshot, QuestMutationPlan plan) {
		return snapshot.status() != plan.nextStatus()
			|| snapshot.packedVariables() != plan.nextPackedVariables()
			|| plan.requiredActions().stream().anyMatch(QuestAction.CompleteQuest.class::isInstance);
	}
}
