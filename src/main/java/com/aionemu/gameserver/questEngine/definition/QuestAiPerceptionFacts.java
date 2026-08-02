package com.aionemu.gameserver.questEngine.definition;

/** Immutable facts captured when an NPC's authoritative aggro list observes a player. */
public record QuestAiPerceptionFacts(int recipientId, int npcObjectId, int npcTemplateId,
		int aggroSourceObjectId, boolean sourceHostile, boolean sourceSpawned,
		int recipientWorldId, int npcWorldId, int recipientInstanceId, int npcInstanceId,
		double distance, int aggroRange, boolean targetAlive, boolean targetSpawned) {
	public QuestAiPerceptionFacts {
		positive(recipientId, "recipientId");
		positive(npcObjectId, "npcObjectId");
		positive(npcTemplateId, "npcTemplateId");
		positive(aggroSourceObjectId, "aggroSourceObjectId");
		if (!sourceHostile || !sourceSpawned) throw new IllegalArgumentException("aggro source must be hostile and spawned");
		nonNegative(recipientWorldId, "recipientWorldId");
		nonNegative(npcWorldId, "npcWorldId");
		nonNegative(recipientInstanceId, "recipientInstanceId");
		nonNegative(npcInstanceId, "npcInstanceId");
		if (!Double.isFinite(distance) || distance < 0) {
			throw new IllegalArgumentException("distance must be finite and non-negative");
		}
		if (aggroRange <= 0) {
			throw new IllegalArgumentException("aggroRange must be positive");
		}
		if (recipientWorldId != npcWorldId || recipientInstanceId != npcInstanceId) {
			throw new IllegalArgumentException("AI perception facts must share world and instance");
		}
	}

	private static void positive(int value, String field) {
		if (value <= 0) throw new IllegalArgumentException(field + " must be positive");
	}

	private static void nonNegative(int value, String field) {
		if (value < 0) throw new IllegalArgumentException(field + " must be non-negative");
	}
}
