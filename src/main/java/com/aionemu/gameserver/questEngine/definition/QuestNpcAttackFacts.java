package com.aionemu.gameserver.questEngine.definition;

/**
 * 在权威攻击回调处捕获的不可变 NPC 战斗事实。
 * Immutable NPC combat facts captured at the authoritative attack callback.
 */
public record QuestNpcAttackFacts(int attackerId, int npcObjectId, int npcTemplateId,
		int currentHp, int maxHp, int worldId, int instanceId) {
	public QuestNpcAttackFacts {
		positive(attackerId, "attackerId");
		positive(npcObjectId, "npcObjectId");
		positive(npcTemplateId, "npcTemplateId");
		if (currentHp < 0 || maxHp <= 0 || currentHp > maxHp) {
			throw new IllegalArgumentException("NPC HP facts are invalid");
		}
		positive(worldId, "worldId");
		positive(instanceId, "instanceId");
	}

	/**
	 * 当前生命值是否低于给定百分比。
	 * Whether current HP is below the given percentage.
	 */
	public boolean belowPercent(int percent) {
		if (percent < 0 || percent > 100) {
			throw new IllegalArgumentException("percent must be between 0 and 100");
		}
		return (long) currentHp * 100L < (long) maxHp * percent;
	}

	private static void positive(int value, String field) {
		if (value <= 0) {
			throw new IllegalArgumentException(field + " must be positive");
		}
	}
}
