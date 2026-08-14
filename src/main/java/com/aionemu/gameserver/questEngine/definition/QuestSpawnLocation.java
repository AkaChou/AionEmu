package com.aionemu.gameserver.questEngine.definition;

import java.util.Objects;

/**
 * 封闭的权威生成位置集合。
 * Closed set of authoritative spawn locations.
 */
public sealed interface QuestSpawnLocation permits QuestSpawnLocation.Fixed,
		QuestSpawnLocation.PlayerPosition {

	/**
	 * 固定世界坐标中的具体生成位置。
	 * A fixed spawn position with explicit world coordinates.
	 */
	record Fixed(int worldId, QuestInstanceTarget instanceTarget, float x, float y, float z, byte heading)
			implements QuestSpawnLocation {
		public Fixed {
			if (worldId <= 0) {
				throw new IllegalArgumentException("worldId must be positive");
			}
			instanceTarget = Objects.requireNonNull(instanceTarget, "instanceTarget");
			if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
				throw new IllegalArgumentException("spawn coordinates must be finite");
			}
		}
	}

	/**
	 * 事件发生时冻结在 {@code QuestSnapshot} 中的玩家位置。
	 * Spawn at the event-time player position frozen in {@code QuestSnapshot}.
	 */
	record PlayerPosition(byte heading) implements QuestSpawnLocation {
	}
}
