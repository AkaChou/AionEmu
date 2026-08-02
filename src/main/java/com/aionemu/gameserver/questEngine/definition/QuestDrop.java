package com.aionemu.gameserver.questEngine.definition;

/** A statically declared quest drop. */
public record QuestDrop(int npcId, int itemId, int chance, boolean eachMember, int collectingStep,
	QuestDropScope scope) {
	public QuestDrop(int npcId, int itemId, int chance, boolean eachMember, int collectingStep) {
		this(npcId, itemId, chance, eachMember, collectingStep,
			eachMember ? QuestDropScope.GROUP : QuestDropScope.NONE);
	}

	public QuestDrop {
		if (npcId <= 0 || itemId <= 0) {
			throw new IllegalArgumentException("quest drop references must be positive");
		}
		if (chance < 0 || chance > 100) {
			throw new IllegalArgumentException("quest drop chance must be between 0 and 100");
		}
		if (collectingStep < 0) {
			throw new IllegalArgumentException("collecting step must be non-negative");
		}
		if (scope == null) {
			throw new IllegalArgumentException("quest drop scope must not be null");
		}
		if (scope == QuestDropScope.NONE && eachMember) {
			throw new IllegalArgumentException("eachMember cannot be true for NONE scope");
		}
	}
}
