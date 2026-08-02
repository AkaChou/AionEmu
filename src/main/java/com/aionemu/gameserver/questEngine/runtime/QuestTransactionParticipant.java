package com.aionemu.gameserver.questEngine.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Reconciles live domain state after the owning JDBC transaction completes. */
public interface QuestTransactionParticipant {
	QuestTransactionParticipant NONE = new QuestTransactionParticipant() {
		@Override
		public void afterCommit() {
		}

		@Override
		public void afterRollback() {
		}
	};

	void afterCommit();

	void afterRollback();

	static QuestTransactionParticipant none() {
		return NONE;
	}

	static QuestTransactionParticipant of(Runnable afterCommit, Runnable afterRollback) {
		Objects.requireNonNull(afterCommit, "afterCommit");
		Objects.requireNonNull(afterRollback, "afterRollback");
		return new QuestTransactionParticipant() {
			@Override
			public void afterCommit() {
				afterCommit.run();
			}

			@Override
			public void afterRollback() {
				afterRollback.run();
			}
		};
	}

	static QuestTransactionParticipant compose(List<QuestTransactionParticipant> participants) {
		List<QuestTransactionParticipant> copy = List.copyOf(participants);
		if (copy.isEmpty()) {
			return none();
		}
		return new QuestTransactionParticipant() {
			@Override
			public void afterCommit() {
				for (QuestTransactionParticipant participant : copy) {
					participant.afterCommit();
				}
			}

			@Override
			public void afterRollback() {
				RuntimeException failure = null;
				for (int i = copy.size() - 1; i >= 0; i--) {
					try {
						copy.get(i).afterRollback();
					} catch (RuntimeException rollbackFailure) {
						if (failure == null) {
							failure = rollbackFailure;
						} else {
							failure.addSuppressed(rollbackFailure);
						}
					}
				}
				if (failure != null) {
					throw failure;
				}
			}
		};
	}

	static void rollbackApplied(List<QuestTransactionParticipant> participants, Exception cause) {
		try {
			compose(new ArrayList<>(participants)).afterRollback();
		} catch (RuntimeException rollbackFailure) {
			cause.addSuppressed(rollbackFailure);
		}
	}
}
