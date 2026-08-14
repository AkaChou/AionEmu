package com.aionemu.gameserver.questEngine.definition;

/**
 * 玩家技能使用成功回调的不可变事实。
 * Immutable facts for a successful player skill-use callback.
 */
public record QuestSkillFacts(int casterId, int skillId, int targetObjectId,
		int targetTemplateId, int targetPlayerId, int worldId, int instanceId,
		boolean castSucceeded) {
	public QuestSkillFacts {
		positive(casterId, "casterId");
		positive(skillId, "skillId");
		nonNegative(targetObjectId, "targetObjectId");
		nonNegative(targetTemplateId, "targetTemplateId");
		nonNegative(targetPlayerId, "targetPlayerId");
		positive(worldId, "worldId");
		positive(instanceId, "instanceId");
		if (!castSucceeded) throw new IllegalArgumentException("skill event requires a successful cast");
	}

	private static void positive(int value, String field) {
		if (value <= 0) throw new IllegalArgumentException(field + " must be positive");
	}

	private static void nonNegative(int value, String field) {
		if (value < 0) throw new IllegalArgumentException(field + " must be non-negative");
	}
}
