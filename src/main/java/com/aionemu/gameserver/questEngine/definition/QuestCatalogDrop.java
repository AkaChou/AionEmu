package com.aionemu.gameserver.questEngine.definition;

import java.util.Objects;
import java.util.Optional;

/**
 * 与产出它的目录快照绑定的不可变运行时掉落条目。
 * Immutable runtime drop entry tied to the catalog snapshot that produced it.
 */
public record QuestCatalogDrop(int questId, int npcId, int itemId, int chance,
		QuestDropScope scope, int collectingStep, int neededAmount, Optional<QuestMetadata> metadata) {
	public QuestCatalogDrop {
		if (questId <= 0 || npcId <= 0 || itemId <= 0) {
			throw new IllegalArgumentException("quest drop references must be positive");
		}
		if (chance < 0 || chance > 100) {
			throw new IllegalArgumentException("quest drop chance must be between 0 and 100");
		}
		if (collectingStep < 0 || neededAmount < 0) {
			throw new IllegalArgumentException("quest drop step and needed amount must be non-negative");
		}
		scope = Objects.requireNonNull(scope, "scope");
		metadata = Objects.requireNonNull(metadata, "metadata");
	}

	public static QuestCatalogDrop catalog(int questId, QuestMetadata metadata, QuestDrop drop) {
		Objects.requireNonNull(metadata, "metadata");
		Objects.requireNonNull(drop, "drop");
		return new QuestCatalogDrop(questId, drop.npcId(), drop.itemId(), drop.chance(), drop.scope(),
			drop.collectingStep(), 0, Optional.of(metadata));
	}

	public boolean dropEachGroupMember() {
		return scope == QuestDropScope.GROUP;
	}

	public boolean dropEachAllianceMember() {
		return scope == QuestDropScope.ALLIANCE;
	}
}
