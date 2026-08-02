package com.aionemu.gameserver.questEngine.definition;

/** Immutable, single-event facts captured at the authoritative proximity boundary. */
public record QuestProximityFacts(int recipientId, int targetObjectId, int targetNpcId,
		int recipientWorldId, int targetWorldId, int recipientInstanceId, int targetInstanceId,
		double distance, double maximumDistance) {
	public QuestProximityFacts {
		checkId(recipientId, "recipientId");
		checkId(targetObjectId, "targetObjectId");
		checkId(targetNpcId, "targetNpcId");
		checkNonNegative(recipientWorldId, "recipientWorldId");
		checkNonNegative(targetWorldId, "targetWorldId");
		checkNonNegative(recipientInstanceId, "recipientInstanceId");
		checkNonNegative(targetInstanceId, "targetInstanceId");
		if (!Double.isFinite(distance) || distance < 0) {
			throw new IllegalArgumentException("distance must be finite and non-negative");
		}
		if (!Double.isFinite(maximumDistance) || maximumDistance <= 0) {
			throw new IllegalArgumentException("maximumDistance must be finite and positive");
		}
		if (recipientWorldId != targetWorldId || recipientInstanceId != targetInstanceId) {
			throw new IllegalArgumentException("proximity facts must share world and instance");
		}
		if (!(distance < maximumDistance)) {
			throw new IllegalArgumentException("distance must be inside the maximum range");
		}
	}

	private static void checkId(int value, String field) {
		if (value <= 0) {
			throw new IllegalArgumentException(field + " must be positive");
		}
	}

	private static void checkNonNegative(int value, String field) {
		if (value < 0) {
			throw new IllegalArgumentException(field + " must be non-negative");
		}
	}
}
