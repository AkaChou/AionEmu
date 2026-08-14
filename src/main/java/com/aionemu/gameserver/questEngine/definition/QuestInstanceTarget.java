package com.aionemu.gameserver.questEngine.definition;

/**
 * 传送与生成动作共用的显式实例选择策略。
 * Explicit instance-selection strategy shared by teleport and spawn actions.
 */
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

	/**
	 * 复用同一世界中的当前实例，否则选择默认实例。
	 * Reuse the current instance in the same world, otherwise select the default instance.
	 */
	enum CurrentOrDefault implements QuestInstanceTarget {
		INSTANCE
	}

	/**
	 * 选择一个具体实例。
	 * Select one concrete instance.
	 */
	record Fixed(int instanceId) implements QuestInstanceTarget {
		public Fixed {
			if (instanceId <= 0) {
				throw new IllegalArgumentException("instanceId must be positive");
			}
		}
	}

	/**
	 * 分配给定世界的下一个可用实例。
	 * Allocate the next available instance of the given world.
	 */
	record NextAvailable(int worldId) implements QuestInstanceTarget {
		public NextAvailable {
			if (worldId <= 0) {
				throw new IllegalArgumentException("worldId must be positive");
			}
		}
	}
}
