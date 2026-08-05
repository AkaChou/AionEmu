package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Applies a caller's routing contract and isolates one owner from the others. */
public final class QuestEventRouter {
	private final QuestEventIndex index;
	private final QuestAuditSink auditSink;
	private final QuestRuntimeMetrics metrics;

	public QuestEventRouter(QuestEventIndex index, QuestAuditSink auditSink, QuestRuntimeMetrics metrics) {
		this.index = Objects.requireNonNull(index, "index");
		this.auditSink = Objects.requireNonNull(auditSink, "auditSink");
		this.metrics = Objects.requireNonNull(metrics, "metrics");
	}

	public DispatchResult dispatch(QuestEvent event, QuestDispatchContract contract,
			QuestRouteHandler handler) {
		return dispatch(event, contract, handler, index.routesFor(event));
	}

	public DispatchResult dispatchOwner(QuestEvent event, int questId, QuestDispatchContract contract,
			QuestRouteHandler handler) {
		return dispatch(event, contract, handler, index.routesFor(event, questId));
	}

	private DispatchResult dispatch(QuestEvent event, QuestDispatchContract contract,
			QuestRouteHandler handler, List<QuestEventIndex.Route> routes) {
		Objects.requireNonNull(event, "event");
		Objects.requireNonNull(contract, "contract");
		Objects.requireNonNull(handler, "handler");
		metrics.onDispatch(contract, routes.size());
		List<OwnerResult> results = new ArrayList<>();
		switch (contract) {
			case EXCLUSIVE -> {
				for (QuestEventIndex.Route route : routes) {
					OwnerResult result = invoke(event, contract, route, handler);
					results.add(result);
					// Boolean legacy callers continue on false; UNKNOWN is also a
					// non-conclusive route result. Any conclusion owns the
					// exclusive event and prevents dialog fallback.
					if (isConclusive(result.result())) {
						break;
					}
				}
			}
			case FIRST_REGISTERED -> {
				if (!routes.isEmpty()) {
					results.add(invoke(event, contract, routes.get(0), handler));
				}
			}
			case FIRST_NON_UNKNOWN -> {
				for (QuestEventIndex.Route route : routes) {
					OwnerResult result = invoke(event, contract, route, handler);
					results.add(result);
					if (result.result() != QuestRouteResult.UNKNOWN) {
						break;
					}
				}
			}
			case BROADCAST -> {
				Set<Integer> concludedOwners = new HashSet<>();
				for (QuestEventIndex.Route route : routes) {
					if (concludedOwners.contains(route.questId())) {
						continue;
					}
					OwnerResult result = invoke(event, contract, route, handler);
					results.add(result);
					if (isConclusive(result.result())) {
						concludedOwners.add(route.questId());
					}
				}
			}
		}
		return new DispatchResult(contract, List.copyOf(results));
	}

	private static boolean isConclusive(QuestRouteResult result) {
		return result != QuestRouteResult.UNKNOWN && result != QuestRouteResult.NOT_HANDLED;
	}

	private OwnerResult invoke(QuestEvent event, QuestDispatchContract contract,
		QuestEventIndex.Route route, QuestRouteHandler handler) {
		try {
			QuestRouteResult result = handler.handle(route);
			QuestRouteResult normalized = result == null ? QuestRouteResult.UNKNOWN : result;
			metrics.onOwnerResult(contract, route.questId(), normalized);
			return new OwnerResult(route.questId(), normalized, null);
		} catch (RuntimeException failure) {
			metrics.onOwnerResult(contract, route.questId(), QuestRouteResult.FAILED);
			try {
				auditSink.record(new QuestAuditEvent(route.questId(), event.type(), contract,
					QuestRouteResult.FAILED, failure.getClass().getName()));
			} catch (RuntimeException auditFailure) {
				metrics.onAuditFailure(route.questId(), auditFailure.getClass().getName());
			}
			return new OwnerResult(route.questId(), QuestRouteResult.FAILED, failure);
		}
	}

	public record DispatchResult(QuestDispatchContract contract, List<OwnerResult> owners) {
		public boolean consumed() {
			return owners.stream().anyMatch(owner -> owner.result() == QuestRouteResult.HANDLED);
		}

		/** A handled or failed owner conclusively claims the event, so no legacy fallback may run. */
		public boolean claimed() {
			return owners.stream().anyMatch(owner -> QuestEventRouter.isConclusive(owner.result()));
		}

		/** Returns only the owners whose own route conclusively claimed the event. */
		public Set<Integer> claimedOwners() {
			return Set.copyOf(owners.stream()
				.filter(owner -> QuestEventRouter.isConclusive(owner.result()))
				.map(OwnerResult::questId)
				.toList());
		}
	}

	public record OwnerResult(int questId, QuestRouteResult result, RuntimeException failure) {
	}
}
