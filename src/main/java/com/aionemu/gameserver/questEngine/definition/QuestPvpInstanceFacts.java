package com.aionemu.gameserver.questEngine.definition;

/** Immutable per-player facts emitted by an authoritative instance settlement callback. */
public record QuestPvpInstanceFacts(int playerId, String instanceKind, int worldId,
		int instanceId, boolean rewardSettled, Integer score, Integer playerKills, Integer monsterKills) {
	public QuestPvpInstanceFacts {
		positive(playerId, "playerId");
		if (instanceKind == null || instanceKind.isBlank()) throw new IllegalArgumentException("instanceKind must not be blank");
		positive(worldId, "worldId");
		positive(instanceId, "instanceId");
		if (!rewardSettled) throw new IllegalArgumentException("instance reward must be settled");
		boolean anyStatistics = score != null || playerKills != null || monsterKills != null;
		boolean allStatistics = score != null && playerKills != null && monsterKills != null;
		if (anyStatistics != allStatistics) throw new IllegalArgumentException("instance statistics must be captured together");
		if (allStatistics && (score < 0 || playerKills < 0 || monsterKills < 0)) {
			throw new IllegalArgumentException("reward values must be non-negative");
		}
	}

	public static QuestPvpInstanceFacts settled(int playerId, String instanceKind, int worldId, int instanceId) {
		return new QuestPvpInstanceFacts(playerId, instanceKind, worldId, instanceId, true, null, null, null);
	}

	public boolean statisticsCaptured() {
		return score != null;
	}

	private static void positive(int value, String field) {
		if (value <= 0) throw new IllegalArgumentException(field + " must be positive");
	}
}
