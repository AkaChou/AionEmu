package com.aionemu.gameserver.questEngine.runtime;

import java.util.Set;

/** Immutable NPC-presence facts captured from the player's current world instance. */
public record QuestWorldFacts(Set<Integer> npcTemplateIds) {
	public QuestWorldFacts {
		if (npcTemplateIds == null || npcTemplateIds.stream().anyMatch(id -> id == null || id <= 0)) {
			throw new IllegalArgumentException("npcTemplateIds must contain only positive ids");
		}
		npcTemplateIds = Set.copyOf(npcTemplateIds);
	}

	public boolean containsNpc(int npcTemplateId) {
		if (npcTemplateId <= 0) {
			throw new IllegalArgumentException("npcTemplateId must be positive");
		}
		return npcTemplateIds.contains(npcTemplateId);
	}
}
