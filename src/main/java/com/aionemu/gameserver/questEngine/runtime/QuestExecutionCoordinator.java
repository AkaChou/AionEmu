package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import java.sql.Connection;
import java.util.Optional;
import java.util.Objects;
import java.util.function.BiConsumer;

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
		// 统一入口保证所有正式 owner 使用同一执行顺序。
		// The single entry point guarantees one execution order for every production owner.
		Objects.requireNonNull(connection, "connection");
		if (playerId <= 0) {
			throw new IllegalArgumentException("playerId must be positive");
		}
		Objects.requireNonNull(definition, "definition");
		Objects.requireNonNull(event, "event");
		Objects.requireNonNull(transition, "transition");
		if (!definition.definition().transitions().contains(transition)) {
			throw new IllegalArgumentException("transition does not belong to definition " + definition.id());
		}
		if (!QuestEvent.matches(transition.event(), event)) {
			throw new IllegalArgumentException("event does not match transition");
		}
		Objects.requireNonNull(eventPort, "eventPort");
		Objects.requireNonNull(actionPort, "actionPort");
		Objects.requireNonNull(statePort, "statePort");
		Objects.requireNonNull(afterCommitPort, "afterCommitPort");
		return serialExecutor.execute(playerId, () -> executeSerialized(connection, playerId, definition, event,
				transition, eventPort, actionPort, statePort, afterCommitPort));
	}

	private QuestExecutionResult executeSerialized(Connection connection, int playerId,
			CompiledQuestDefinition definition, QuestEvent event, QuestTransition transition,
			QuestEventPort eventPort, QuestActionPort actionPort, QuestStatePort statePort,
			QuestAfterCommitPort afterCommitPort) throws Exception {
		QuestTransactionParticipant participant = QuestTransactionParticipant.none();
		QuestMutationPlan appliedPlan = null;
		boolean committed = false;
		try (QuestUnitOfWork unit = QuestUnitOfWork.open(connection)) {
			QuestSnapshot snapshot = eventPort.snapshot(unit.connection(), playerId, definition.id(), event);
			if (snapshot == null) {
				throw new IllegalStateException("event port returned no snapshot");
			}
			if (snapshot.playerId() != playerId || snapshot.questId() != definition.id()) {
				throw new IllegalStateException("event snapshot does not belong to player/quest");
			}
			Optional<QuestMutationPlan> plan = QuestMutationPlanner.plan(definition, snapshot, event, transition);
			if (plan.isEmpty()) {
				unit.rollback();
				return new QuestExecutionResult(QuestExecutionStatus.NO_MATCH, null, unit.afterCommitFailures());
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
			if (!durableActions.isEmpty()) {
				actionPort.preflight(unit.connection(), snapshot, durableActions);
				participant = actionPort.apply(unit.connection(), snapshot, durableActions);
			}
			if (persistState) {
				statePort.apply(unit.connection(), playerId, resolved);
				appliedPlan = resolved;
			}
			for (AfterCommitAction action : resolved.afterCommit()) {
				unit.afterCommit(() -> afterCommitPort.execute(action, snapshot, resolved));
			}
			if (persistState
					&& (resolved.nextStatus() == QuestStatus.COMPLETE || resolved.nextStatus() == QuestStatus.NONE)) {
				// Cleanup is an ordered, best-effort post-commit action too. Register it last so
				// domain effects can still resolve their quest-owned resources before teardown.
				unit.afterCommit(() -> terminalCleanup.accept(playerId, resolved.questId()));
			}
			unit.commit();
			committed = true;
			// 只有提交成功后内存才前进;commit 失败时 publish 不执行,内存保持事件前值。
			// required participant 先清理已提交的 dirty 状态，再发布 quest state 和协议动作。
			try {
				participant.afterCommit();
			} finally {
				if (persistState) {
					statePort.publish(playerId, resolved);
				}
			}
			unit.runAfterCommit();
			return new QuestExecutionResult(QuestExecutionStatus.COMMITTED, resolved, unit.afterCommitFailures());
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
			throw failure;
		}
	}

	private static boolean requiresStatePersistence(QuestSnapshot snapshot, QuestMutationPlan plan) {
		return snapshot.status() != plan.nextStatus()
			|| snapshot.packedVariables() != plan.nextPackedVariables()
			|| plan.requiredActions().stream().anyMatch(QuestAction.CompleteQuest.class::isInstance);
	}
}
