package com.aionemu.gameserver.questEngine.definition;

/**
 * 服务器接受飞行动作后捕获的不可变移动事实。
 * Immutable movement facts captured after the server accepts a flight action.
 */
public record QuestMovementFacts(int playerId, int worldId, int instanceId,
		float x, float y, float z, boolean spawned, boolean flying, String actionId) {
	public QuestMovementFacts {
		positive(playerId, "playerId");
		positive(worldId, "worldId");
		positive(instanceId, "instanceId");
		if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
			throw new IllegalArgumentException("movement coordinates must be finite");
		}
		if (!spawned) throw new IllegalArgumentException("movement player must be spawned");
		if (actionId == null || actionId.isBlank()) throw new IllegalArgumentException("actionId must not be blank");
	}

	private static void positive(int value, String field) {
		if (value <= 0) throw new IllegalArgumentException(field + " must be positive");
	}
}
