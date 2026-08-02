package com.aionemu.gameserver.questEngine.runtime;

import java.util.Objects;

/** Immutable, typed result of one legacy handler invocation. */
public record QuestLegacyInvocation(int playerId, int questId, String eventType,
		QuestDispatchContract contract, QuestShadowObservation observation) {
	public QuestLegacyInvocation {
		if (playerId <= 0 || questId <= 0) {
			throw new IllegalArgumentException("playerId and questId must be positive");
		}
		eventType = requireText(eventType, "eventType");
		contract = Objects.requireNonNull(contract, "contract");
		observation = Objects.requireNonNull(observation, "observation");
	}

	private static String requireText(String value, String field) {
		Objects.requireNonNull(value, field);
		if (value.isBlank()) {
			throw new IllegalArgumentException(field + " must not be blank");
		}
		return value;
	}
}
