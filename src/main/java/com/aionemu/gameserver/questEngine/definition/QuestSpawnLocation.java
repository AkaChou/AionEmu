package com.aionemu.gameserver.questEngine.definition;

import java.util.Objects;

/** Closed set of authoritative spawn locations. */
public sealed interface QuestSpawnLocation permits QuestSpawnLocation.Fixed,
		QuestSpawnLocation.PlayerPosition {

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

	/** Spawn at the event-time player position frozen in {@code QuestSnapshot}. */
	record PlayerPosition(byte heading) implements QuestSpawnLocation {
	}
}
