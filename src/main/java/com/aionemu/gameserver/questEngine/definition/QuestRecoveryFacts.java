package com.aionemu.gameserver.questEngine.definition;

/**
 * 不可变的登出事实；运行时资源由恢复端口清理，而非 quest_vars。
 * Immutable logout facts. Runtime resources are cleaned by the recovery port, not quest vars.
 */
public record QuestRecoveryFacts(int playerId, int worldId, int instanceId,
		boolean spawned, boolean cleanupRequired) {
	public QuestRecoveryFacts {
		if (playerId <= 0) throw new IllegalArgumentException("playerId must be positive");
		if (worldId < 0 || instanceId < 0) throw new IllegalArgumentException("world and instance ids must be non-negative");
		if (!cleanupRequired) throw new IllegalArgumentException("logout recovery must require cleanup");
	}
}
