package com.aionemu.gameserver.questEngine.runtime;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.LongAdder;

/** typed 路由的线程安全进程内指标实现。 Thread-safe in-process metrics for typed routing. */
public final class QuestRuntimeMetricsCollector implements QuestRuntimeMetrics {
	private final LongAdder dispatches = new LongAdder();
	private final LongAdder routedOwners = new LongAdder();
	private final LongAdder auditFailures = new LongAdder();
	private final EnumMap<QuestRouteResult, LongAdder> outcomes = new EnumMap<>(QuestRouteResult.class);

	public QuestRuntimeMetricsCollector() {
		for (QuestRouteResult result : QuestRouteResult.values()) {
			outcomes.put(result, new LongAdder());
		}
	}

	@Override
	public void onDispatch(QuestDispatchContract contract, int ownerCount) {
		Objects.requireNonNull(contract, "contract");
		if (ownerCount < 0) {
			throw new IllegalArgumentException("ownerCount must not be negative");
		}
		dispatches.increment();
		routedOwners.add(ownerCount);
	}

	@Override
	public void onOwnerResult(QuestDispatchContract contract, int questId, QuestRouteResult result) {
		Objects.requireNonNull(contract, "contract");
		if (questId <= 0) {
			throw new IllegalArgumentException("questId must be positive");
		}
		outcomes.get(Objects.requireNonNull(result, "result")).increment();
	}

	@Override
	public void onAuditFailure(int questId, String failureType) {
		if (questId <= 0) {
			throw new IllegalArgumentException("questId must be positive");
		}
		if (failureType == null || failureType.isBlank()) {
			throw new IllegalArgumentException("failureType must not be blank");
		}
		auditFailures.increment();
	}

	public Snapshot snapshot() {
		Map<QuestRouteResult, Long> counts = new EnumMap<>(QuestRouteResult.class);
		outcomes.forEach((result, count) -> counts.put(result, count.sum()));
		return new Snapshot(dispatches.sum(), routedOwners.sum(), Map.copyOf(counts), auditFailures.sum());
	}

	public record Snapshot(long dispatches, long routedOwners, Map<QuestRouteResult, Long> outcomes,
			long auditFailures) {
		public Snapshot(long dispatches, long routedOwners, Map<QuestRouteResult, Long> outcomes) {
			this(dispatches, routedOwners, outcomes, 0);
		}

		public Snapshot {
			if (dispatches < 0 || routedOwners < 0 || auditFailures < 0) {
				throw new IllegalArgumentException("metric counts must not be negative");
			}
			outcomes = Map.copyOf(Objects.requireNonNull(outcomes, "outcomes"));
		}

		public long outcomeCount(QuestRouteResult result) {
			return outcomes.getOrDefault(result, 0L);
		}
	}
}
