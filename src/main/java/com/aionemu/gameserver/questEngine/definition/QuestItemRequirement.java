package com.aionemu.gameserver.questEngine.definition;

/** A statically declared quest item requirement. */
public record QuestItemRequirement(int itemId, int count) {
	public QuestItemRequirement {
		if (itemId <= 0 || count <= 0) {
			throw new IllegalArgumentException("quest item id and count must be positive");
		}
	}
}
