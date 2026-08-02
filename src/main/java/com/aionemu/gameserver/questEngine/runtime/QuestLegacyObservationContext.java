package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import java.util.Objects;
import java.util.Optional;

/**
 * Explicit, thread-scoped bridge used by legacy helper adapters during shadow
 * capture. Without an installed recorder it does nothing; that absence only
 * disables observation and never changes the legacy result.
 */
public final class QuestLegacyObservationContext {
	private static final ThreadLocal<QuestLegacyObservationRecorder> CURRENT = new ThreadLocal<>();

	private QuestLegacyObservationContext() {
	}

	public static Scope open(QuestLegacyObservationRecorder recorder) {
		Objects.requireNonNull(recorder, "recorder");
		QuestLegacyObservationRecorder previous = CURRENT.get();
		CURRENT.set(recorder);
		return new Scope(previous);
	}

	public static Optional<QuestLegacyObservationRecorder> current() {
		return Optional.ofNullable(CURRENT.get());
	}

	public static void requiredAction(int questId, QuestAction action) {
		QuestLegacyObservationRecorder recorder = CURRENT.get();
		if (recorder != null) {
			recorder.requiredAction(questId, action);
		}
	}

	public static void afterCommitAction(int questId, AfterCommitAction action) {
		QuestLegacyObservationRecorder recorder = CURRENT.get();
		if (recorder != null) {
			recorder.afterCommitAction(questId, action);
		}
	}

	public static void state(int questId, QuestStatus status, int packedVariables) {
		QuestLegacyObservationRecorder recorder = CURRENT.get();
		if (recorder != null) {
			recorder.state(questId, status, packedVariables);
		}
	}

	public static void result(int questId, QuestRouteResult result) {
		QuestLegacyObservationRecorder recorder = CURRENT.get();
		if (recorder != null) {
			recorder.result(questId, result);
		}
	}

	public static void conditionMatched(int questId, boolean matched) {
		QuestLegacyObservationRecorder recorder = CURRENT.get();
		if (recorder != null) {
			recorder.conditionMatched(questId, matched);
		}
	}

	public static final class Scope implements AutoCloseable {
		private final QuestLegacyObservationRecorder previous;
		private boolean closed;

		private Scope(QuestLegacyObservationRecorder previous) {
			this.previous = previous;
		}

		@Override
		public void close() {
			if (closed) {
				return;
			}
			closed = true;
			if (previous == null) {
				CURRENT.remove();
			} else {
				CURRENT.set(previous);
			}
		}
	}
}
