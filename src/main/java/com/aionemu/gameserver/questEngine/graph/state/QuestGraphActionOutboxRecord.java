package com.aionemu.gameserver.questEngine.graph.state;

import java.util.Objects;

/** Immutable durable state for one quest graph teleport command. */
public record QuestGraphActionOutboxRecord(TeleportOutboxCommand command, long outboxSequence, Status status, long claimGeneration,
		Long leaseUntil, long acceptedAt, Long completedAt, boolean graphAcked) {

	public enum Status {
		ACCEPTED,
		CLAIMED,
		COMPLETED,
		GRAPH_ACKED
	}

	public QuestGraphActionOutboxRecord {
		Objects.requireNonNull(command, "command");
		Objects.requireNonNull(status, "status");
		if (outboxSequence <= 0 || claimGeneration < 0 || acceptedAt <= 0 || leaseUntil != null && leaseUntil <= 0
				|| completedAt != null && completedAt <= 0) {
			throw new IllegalArgumentException("Quest graph action outbox sequence/timestamps/generation are invalid");
		}
		switch (status) {
			case ACCEPTED -> {
				if (claimGeneration != 0 || leaseUntil != null || completedAt != null) {
					throw new IllegalArgumentException("Accepted outbox record carries claim/completion state");
				}
			}
			case CLAIMED -> {
				if (claimGeneration == 0 || leaseUntil == null || completedAt != null) {
					throw new IllegalArgumentException("Claimed outbox record is missing its active lease");
				}
			}
			case COMPLETED -> {
				if (claimGeneration == 0 || leaseUntil != null || completedAt == null || graphAcked) {
					throw new IllegalArgumentException("Completed outbox record is inconsistent");
				}
			}
			case GRAPH_ACKED -> {
				if (claimGeneration == 0 || leaseUntil != null || completedAt == null || !graphAcked) {
					throw new IllegalArgumentException("Graph-acked outbox record is inconsistent");
				}
			}
		}
	}

	public static QuestGraphActionOutboxRecord accepted(TeleportOutboxCommand command, long outboxSequence, long acceptedAt) {
		return new QuestGraphActionOutboxRecord(command, outboxSequence, Status.ACCEPTED, 0, null, acceptedAt, null, false);
	}

	public boolean pendingDelivery() {
		return completedAt == null;
	}

	public boolean deletable() {
		return status == Status.GRAPH_ACKED;
	}
}
