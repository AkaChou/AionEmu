package com.aionemu.gameserver.questEngine.definition;

/**
 * 从旧版任务模板复制的静态奖励元数据。
 * Static bonus metadata copied from a legacy quest template.
 */
public record QuestBonus(String type, Integer level, Integer skill) {
	public QuestBonus {
		if (type == null || type.isBlank()) {
			throw new IllegalArgumentException("bonus type must not be blank");
		}
		if (level != null && level < 0) {
			throw new IllegalArgumentException("bonus level must be non-negative");
		}
		if (skill != null && skill < 0) {
			throw new IllegalArgumentException("bonus skill must be non-negative");
		}
	}
}
