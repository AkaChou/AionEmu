package com.aionemu.gameserver.questEngine.runtime;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** 从玩家当前位置捕获的不可变世界实例事实。 / Immutable world-instance facts captured from the player's current position. */
public record QuestWorldFacts(Set<Integer> npcTemplateIds, Set<String> zoneNames) {
	public QuestWorldFacts(Set<Integer> npcTemplateIds) {
		this(npcTemplateIds, Set.of());
	}

	public QuestWorldFacts {
		if (npcTemplateIds == null || npcTemplateIds.stream().anyMatch(id -> id == null || id <= 0)) {
			throw new IllegalArgumentException("npcTemplateIds must contain only positive ids");
		}
		npcTemplateIds = Set.copyOf(npcTemplateIds);
		if (zoneNames == null || zoneNames.stream().anyMatch(zone -> zone == null || zone.isBlank())) {
			throw new IllegalArgumentException("zoneNames must contain only non-blank names");
		}
		zoneNames = zoneNames.stream().map(zone -> zone.toUpperCase(Locale.ROOT))
			.collect(Collectors.toUnmodifiableSet());
	}

	public boolean containsNpc(int npcTemplateId) {
		if (npcTemplateId <= 0) {
			throw new IllegalArgumentException("npcTemplateId must be positive");
		}
		return npcTemplateIds.contains(npcTemplateId);
	}

	public boolean containsZone(String zone) {
		if (zone == null || zone.isBlank()) {
			throw new IllegalArgumentException("zone must not be blank");
		}
		return zoneNames.contains(zone.toUpperCase(Locale.ROOT));
	}
}
