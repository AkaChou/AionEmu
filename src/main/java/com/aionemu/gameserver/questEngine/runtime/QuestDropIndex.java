package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestCatalogDrop;
import com.aionemu.gameserver.questEngine.definition.QuestCatalogEntry;
import com.aionemu.gameserver.questEngine.definition.QuestCatalogRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 从单个规范目录快照编译的不可变 NPC 掉落索引。 / Immutable NPC-to-drop index compiled from one canonical catalog snapshot. */
public final class QuestDropIndex {
	private final Map<Integer, List<QuestCatalogDrop>> dropsByNpc;

	public QuestDropIndex(QuestCatalogRegistry catalog) {
		Objects.requireNonNull(catalog, "catalog");
		Map<Integer, List<QuestCatalogDrop>> mutable = new LinkedHashMap<>();
		catalog.entries().stream().sorted(java.util.Comparator.comparingInt(QuestCatalogEntry::id))
			.forEach(entry -> entry.metadata().drops().forEach(drop -> mutable
				.computeIfAbsent(drop.npcId(), ignored -> new ArrayList<>())
				.add(QuestCatalogDrop.catalog(entry.id(), entry.metadata(), drop))));
		Map<Integer, List<QuestCatalogDrop>> immutable = new LinkedHashMap<>();
		mutable.forEach((npcId, drops) -> immutable.put(npcId, List.copyOf(drops)));
		this.dropsByNpc = Collections.unmodifiableMap(immutable);
	}

	public List<QuestCatalogDrop> dropsFor(int npcId) {
		return dropsByNpc.getOrDefault(npcId, List.of());
	}
}
