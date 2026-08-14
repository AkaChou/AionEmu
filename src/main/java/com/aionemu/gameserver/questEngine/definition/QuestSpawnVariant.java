package com.aionemu.gameserver.questEngine.definition;

import java.util.Objects;

/**
 * 随机任务刷新的一个权威模板/位置备选方案。
 * One authoritative template/location alternative for a random quest spawn.
 */
public record QuestSpawnVariant(int templateId, QuestSpawnLocation location) {
	public QuestSpawnVariant {
		if (templateId <= 0) {
			throw new IllegalArgumentException("templateId must be positive");
		}
		location = Objects.requireNonNull(location, "location");
	}
}
