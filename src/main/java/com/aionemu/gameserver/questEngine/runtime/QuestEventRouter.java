package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** 应用调用方的路由契约，并将一个 owner 与其他 owner 隔离。 / Applies a caller's routing contract and isolates one owner from the others. */
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
					// 布尔旧调用方在 false 时继续；UNKNOWN 也是非结论性路由结果。
					// 任何结论性结果独占该事件并阻止对话回退。
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
				auditSink.record(auditEvent(event, contract, route, QuestRouteResult.FAILED, failure));
			} catch (RuntimeException auditFailure) {
				metrics.onAuditFailure(route.questId(), auditFailure.getClass().getName());
			}
			return new OwnerResult(route.questId(), QuestRouteResult.FAILED, failure);
		}
	}

	static QuestAuditEvent auditEvent(QuestEvent event, QuestDispatchContract contract,
			QuestEventIndex.Route route, QuestRouteResult result, Throwable failure) {
		QuestExecutionFailureException executionFailure = cause(failure, QuestExecutionFailureException.class);
		QuestPostCommitFailure postCommitFailure = cause(failure, QuestPostCommitFailure.class);
		QuestFailureStage stage = executionFailure != null ? executionFailure.stage()
			: postCommitFailure != null ? postCommitFailure.stage() : QuestFailureStage.ROUTING;
		boolean committed = executionFailure != null ? executionFailure.committed() : postCommitFailure != null;
		Throwable root = rootCause(failure);
		int npcId = switch (event) {
			case QuestEvent.TalkToNpc talk -> talk.npcId();
			case QuestEvent.KillNpc kill -> kill.npcId();
			case QuestEvent.AttackNpc attack -> attack.npcId();
			case QuestEvent.AtDistance distance -> distance.npcId();
			case QuestEvent.CanAct canAct -> canAct.templateId();
			default -> 0;
		};
		int dialogId = switch (event) {
			case QuestEvent.TalkToNpc talk -> talk.dialogId() == null ? 0 : talk.dialogId();
			case QuestEvent.QuestDialog dialog -> dialog.dialogId();
			default -> 0;
		};
		return new QuestAuditEvent(route.questId(), event.type(), contract, result,
			route.transition().sourceNode(), route.transition().targetNode(), npcId, dialogId,
			stage, committed, root);
	}

	private static Throwable rootCause(Throwable failure) {
		Throwable current = Objects.requireNonNull(failure, "failure");
		while (current.getCause() != null && current.getCause() != current) {
			current = current.getCause();
		}
		return current;
	}

	private static <T extends Throwable> T cause(Throwable failure, Class<T> type) {
		Throwable current = failure;
		while (current != null) {
			if (type.isInstance(current)) {
				return type.cast(current);
			}
			current = current.getCause();
		}
		return null;
	}

	public record DispatchResult(QuestDispatchContract contract, List<OwnerResult> owners) {
		public boolean consumed() {
			return handled();
		}

		public boolean handled() {
			return owners.stream().anyMatch(owner -> owner.result() == QuestRouteResult.HANDLED);
		}

		public boolean failed() {
			return owners.stream().anyMatch(owner -> owner.result() == QuestRouteResult.FAILED);
		}

		/** 已处理或失败的 owner 结论性认领事件，因此任何旧版回退都不得运行。 / A handled or failed owner conclusively claims the event, so no legacy fallback may run. */
		public boolean claimed() {
			return owners.stream().anyMatch(owner -> QuestEventRouter.isConclusive(owner.result()));
		}

		/** 仅返回自身路由结论性认领事件的 owner。 / Returns only the owners whose own route conclusively claimed the event. */
		public Set<Integer> claimedOwners() {
			return Set.copyOf(owners.stream()
				.filter(owner -> QuestEventRouter.isConclusive(owner.result()))
				.map(OwnerResult::questId)
				.toList());
		}

		/** 返回成功完成的 owner；失败与显式阻止的路由被排除。 / Returns owners that completed successfully; failed and explicitly blocked routes are excluded. */
		public Set<Integer> handledOwners() {
			return Set.copyOf(owners.stream()
				.filter(owner -> owner.result() == QuestRouteResult.HANDLED)
				.map(OwnerResult::questId)
				.toList());
		}
	}

	public record OwnerResult(int questId, QuestRouteResult result, RuntimeException failure) {
	}
}
