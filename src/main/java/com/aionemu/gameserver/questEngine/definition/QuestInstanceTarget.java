package com.aionemu.gameserver.questEngine.definition;

/** Explicit instance-selection strategy shared by teleport and spawn actions. */
public sealed interface QuestInstanceTarget permits QuestInstanceTarget.CurrentOrDefault,
		QuestInstanceTarget.Fixed {

	static QuestInstanceTarget currentOrDefault() {
		return CurrentOrDefault.INSTANCE;
	}

	static QuestInstanceTarget fixed(int instanceId) {
		return new Fixed(instanceId);
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
}
