package com.aionemu.gameserver.questEngine.definition;

import java.util.List;
import java.util.Objects;

/**
 * 静态的顺序击杀目标元数据。
 * Static sequential kill target metadata.
 */
public record QuestKill(int sequence, List<Integer> npcIds) {
	public QuestKill {
		if (sequence <= 0) {
			throw new IllegalArgumentException("kill sequence must be positive");
		}
		npcIds = List.copyOf(Objects.requireNonNull(npcIds, "npcIds"));
		if (npcIds.isEmpty() || npcIds.stream().anyMatch(id -> id == null || id <= 0)) {
			throw new IllegalArgumentException("kill npc ids must be positive");
		}
	}
}
