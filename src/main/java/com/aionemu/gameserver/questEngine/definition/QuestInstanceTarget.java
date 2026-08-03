package com.aionemu.gameserver.questEngine.definition;

/** Explicit instance-selection strategy shared by teleport and spawn actions. */
public sealed interface QuestInstanceTarget permits QuestInstanceTarget.CurrentOrDefault,
		QuestInstanceTarget.Fixed, QuestInstanceTarget.NextAvailable {

	static QuestInstanceTarget currentOrDefault() {
		return CurrentOrDefault.INSTANCE;
	}

	static QuestInstanceTarget fixed(int instanceId) {
		return new Fixed(instanceId);
	}

	static QuestInstanceTarget nextAvailable(int worldId) {
		return new NextAvailable(worldId);
	}

	/** Reuse the current instance in the same world, otherwise select the default instance. */
	enum CurrentOrDefault implements QuestInstanceTarget {
		INSTANCE
	}

	/** Select one concrete instance. */
	record Fixed(int instanceId) implements QuestInstanceTarget {
		public Fixed {
			if (instanceId <= 0) {
				throw new IllegalArgumentException("instanceId must be positive");
			}
		}
	}

	/** Allocate the next available instance of the given world. */
	record NextAvailable(int worldId) implements QuestInstanceTarget {
		public NextAvailable {
			if (worldId <= 0) {
				throw new IllegalArgumentException("worldId must be positive");
			}
		}
	}
}
