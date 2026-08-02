package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Builds a typed observation from an actual legacy owner invocation.
 *
 * <p>The recorder never invokes a handler, reads a service, or mutates player
 * state. A legacy adapter explicitly opens a recorder, records facts at shared
 * helper boundaries, and closes each owner with the state/result that was
 * actually observed. Incomplete owners fail when a snapshot is requested so
 * an observation cannot silently become a clean shadow result.</p>
 */
public final class QuestLegacyObservationRecorder {
	private final Map<Integer, OwnerDraft> owners = new LinkedHashMap<>();

	public void beginOwner(int questId) {
		if (questId <= 0) {
			throw new IllegalArgumentException("questId must be positive");
		}
		if (owners.putIfAbsent(questId, new OwnerDraft(questId)) != null) {
			throw new IllegalStateException("owner already started: " + questId);
		}
	}

	public void conditionMatched(int questId, boolean matched) {
		OwnerDraft owner = owner(questId);
		if (owner.conditionMatched != null && owner.conditionMatched != matched) {
			throw new IllegalStateException("owner condition was recorded twice: " + questId);
		}
		owner.conditionMatched = matched;
	}

	public void requiredAction(int questId, QuestAction action) {
		owner(questId).requiredActions.add(Objects.requireNonNull(action, "action"));
	}

	public void afterCommitAction(int questId, AfterCommitAction action) {
		owner(questId).afterCommit.add(Objects.requireNonNull(action, "action"));
	}

	public void state(int questId, QuestStatus status, int packedVariables) {
		if (packedVariables < 0) {
			throw new IllegalArgumentException("packedVariables must not be negative");
		}
		OwnerDraft owner = owner(questId);
		owner.nextStatus = Objects.requireNonNull(status, "status");
		owner.nextPackedVariables = packedVariables;
	}

	public void result(int questId, QuestRouteResult result) {
		OwnerDraft owner = owner(questId);
		if (owner.result != null && owner.result != result) {
			throw new IllegalStateException("owner result was recorded twice: " + questId);
		}
		owner.result = Objects.requireNonNull(result, "result");
	}

	public boolean hasEffects(int questId) {
		OwnerDraft owner = owner(questId);
		return !owner.requiredActions.isEmpty() || !owner.afterCommit.isEmpty() || owner.nextStatus != null;
	}

	/**
	 * Freezes one owner. The state may be absent for an explicitly unmatched or
	 * UNKNOWN route, but a matched owner must provide its resulting projection.
	 */
	public void completeOwner(int questId) {
		OwnerDraft owner = owner(questId);
		if (owner.completed) {
			throw new IllegalStateException("owner already completed: " + questId);
		}
		if (owner.conditionMatched == null || owner.result == null) {
			throw new IllegalStateException("owner observation is incomplete: " + questId);
		}
		if (owner.conditionMatched && owner.nextStatus == null) {
			throw new IllegalStateException("matched owner has no resulting state: " + questId);
		}
		owner.completed = true;
	}

	public QuestShadowObservation snapshot() {
		Map<Integer, QuestShadowObservation.Owner> frozen = new LinkedHashMap<>();
		boolean consumed = false;
		for (OwnerDraft owner : owners.values()) {
			if (!owner.completed) {
				throw new IllegalStateException("owner observation is incomplete: " + owner.questId);
			}
			QuestShadowObservation.Owner value = new QuestShadowObservation.Owner(owner.questId,
				owner.conditionMatched, owner.nextStatus, owner.nextPackedVariables,
				owner.requiredActions, owner.afterCommit, owner.result);
			frozen.put(owner.questId, value);
			consumed |= owner.result == QuestRouteResult.HANDLED;
		}
		return new QuestShadowObservation(frozen, consumed);
	}

	private OwnerDraft owner(int questId) {
		OwnerDraft owner = owners.get(questId);
		if (owner == null) {
			throw new IllegalStateException("owner was not started: " + questId);
		}
		if (owner.completed) {
			throw new IllegalStateException("owner observation is already complete: " + questId);
		}
		return owner;
	}

	private static final class OwnerDraft {
		private final int questId;
		private final java.util.List<QuestAction> requiredActions = new java.util.ArrayList<>();
		private final java.util.List<AfterCommitAction> afterCommit = new java.util.ArrayList<>();
		private Boolean conditionMatched;
		private QuestStatus nextStatus;
		private int nextPackedVariables;
		private QuestRouteResult result;
		private boolean completed;

		private OwnerDraft(int questId) {
			this.questId = questId;
		}
	}
}
