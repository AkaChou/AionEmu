package com.aionemu.gameserver.questEngine.definition;

/**
 * 来自旧版任务元数据的一条规范化起始条件条目。
 * One normalized start-condition entry from legacy quest metadata.
 */
public record QuestStartCondition(String type, int questId, int rewardMode) {
	public QuestStartCondition {
		if (type == null || type.isBlank()) {
			throw new IllegalArgumentException("start condition type must not be blank");
		}
		if (questId <= 0) {
			throw new IllegalArgumentException("start condition quest id must be positive");
		}
		if (rewardMode < 0) {
			throw new IllegalArgumentException("start condition reward mode must be non-negative");
		}
	}
}
