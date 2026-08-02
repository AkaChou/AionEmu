package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Typed, side-effect-free observation captured at a legacy helper boundary. */
public record QuestShadowObservation(Map<Integer, Owner> owners, boolean consumed) {
	public QuestShadowObservation {
		owners = Map.copyOf(Objects.requireNonNull(owners, "owners"));
		if (owners.keySet().stream().anyMatch(id -> id == null || id <= 0)) {
			throw new IllegalArgumentException("observation owner ids must be positive");
		}
		owners.forEach((id, owner) -> {
			if (owner == null || owner.questId() != id) {
				throw new IllegalArgumentException("observation map key must match owner quest id");
			}
		});
	}

	public record Owner(int questId, boolean conditionMatched, QuestStatus nextStatus,
			int nextPackedVariables, List<QuestAction> requiredActions,
			List<AfterCommitAction> afterCommit, QuestRouteResult result) {
		public Owner {
			if (questId <= 0) {
				throw new IllegalArgumentException("questId must be positive");
			}
			if (conditionMatched && nextStatus == null) {
				throw new NullPointerException("nextStatus is required for a matched observation");
			}
			if (nextPackedVariables < 0) {
				throw new IllegalArgumentException("nextPackedVariables must not be negative");
			}
			requiredActions = List.copyOf(Objects.requireNonNull(requiredActions, "requiredActions"));
			afterCommit = List.copyOf(Objects.requireNonNull(afterCommit, "afterCommit"));
			result = Objects.requireNonNull(result, "result");
		}
	}
}
