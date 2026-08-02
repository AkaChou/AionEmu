package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * Captures one legacy owner call without changing the legacy result contract.
 *
 * <p>When the sink is {@link QuestLegacyObservationSink#NOOP}, invocation is
 * direct. With a sink installed the bridge opens the explicit observation
 * scope, records the actual state projection and normalizes only the result
 * contract supplied by the caller. It never calls a service or executes a
 * candidate action.</p>
 */
@Slf4j(topic = "QUEST_SHADOW")
public final class QuestLegacyInvocationBridge {
	@FunctionalInterface
	public interface Invocation<T> {
		T run();
	}

	@FunctionalInterface
	public interface ResultClassifier<T> {
		QuestRouteResult classify(T result, boolean stateChanged, QuestLegacyObservationRecorder recorder);
	}

	private final QuestLegacyObservationSink sink;

	public QuestLegacyInvocationBridge() {
		this(QuestLegacyObservationSink.NOOP);
	}

	public QuestLegacyInvocationBridge(QuestLegacyObservationSink sink) {
		this.sink = Objects.requireNonNull(sink, "sink");
	}

	public <T> T invoke(Player player, int questId, String eventType, QuestDispatchContract contract,
		Invocation<T> invocation, ResultClassifier<T> classifier) {
		Objects.requireNonNull(player, "player");
		Objects.requireNonNull(invocation, "invocation");
		Objects.requireNonNull(classifier, "classifier");
		if (sink == QuestLegacyObservationSink.NOOP) {
			return invocation.run();
		}

		QuestState beforeState = state(player, questId);
		int beforePacked = beforeState == null ? 0 : beforeState.getQuestVars().getQuestVars();
		QuestStatus beforeStatus = beforeState == null ? null : beforeState.getStatus();
		QuestLegacyObservationRecorder recorder = new QuestLegacyObservationRecorder();
		recorder.beginOwner(questId);
		T result;
		try (QuestLegacyObservationContext.Scope ignored = QuestLegacyObservationContext.open(recorder)) {
			try {
				result = invocation.run();
			} catch (RuntimeException failure) {
				recordFailure(player, questId, recorder);
				safeRecord(player, questId, eventType, contract, recorder);
				throw failure;
			}
			QuestState afterState = state(player, questId);
			boolean stateChanged = changed(beforeState, beforeStatus, beforePacked, afterState);
			if (afterState != null) {
				QuestLegacyObservationContext.state(questId, afterState.getStatus(),
					afterState.getQuestVars().getQuestVars());
			}
			QuestRouteResult routeResult = Objects.requireNonNull(
				classifier.classify(result, stateChanged, recorder), "classified result");
			// A successful protocol-only callback is observable, but it is not a
			// candidate state match without a canonical QuestStatus + quest_vars
			// projection. Keep the result/action facts so shadow can report it.
			boolean matched = afterState != null
				&& ((routeResult == QuestRouteResult.HANDLED) || stateChanged || recorder.hasEffects(questId));
			QuestLegacyObservationContext.conditionMatched(questId, matched);
			QuestLegacyObservationContext.result(questId, routeResult);
			recorder.completeOwner(questId);
			safeRecord(player, questId, eventType, contract, recorder);
			return result;
		}
	}

	private void safeRecord(Player player, int questId, String eventType, QuestDispatchContract contract,
		QuestLegacyObservationRecorder recorder) {
		try {
			sink.record(new QuestLegacyInvocation(player.getObjectId(), questId, eventType, contract,
				recorder.snapshot()));
		} catch (RuntimeException failure) {
			// Observation is diagnostic only; report the loss without altering legacy behavior.
			log.warn("Failed to record legacy quest observation for player {} quest {} event {}",
				player.getObjectId(), questId, eventType, failure);
		}
	}

	private static void recordFailure(Player player, int questId, QuestLegacyObservationRecorder recorder) {
		QuestState state = state(player, questId);
		if (state != null) {
			QuestLegacyObservationContext.state(questId, state.getStatus(), state.getQuestVars().getQuestVars());
		}
		QuestLegacyObservationContext.conditionMatched(questId, false);
		QuestLegacyObservationContext.result(questId, QuestRouteResult.FAILED);
		recorder.completeOwner(questId);
	}

	private static QuestState state(Player player, int questId) {
		return player.getQuestStateList().getQuestState(questId);
	}

	private static boolean changed(QuestState before, QuestStatus beforeStatus, int beforePacked, QuestState after) {
		if (before == null || after == null) {
			return before != after;
		}
		return beforeStatus != after.getStatus()
			|| beforePacked != after.getQuestVars().getQuestVars();
	}
}
