package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestCatalog;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestStartCondition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 任务状态变化到受影响 LEVEL_UP owner 的不可变反向索引。 / Immutable reverse index from quest-state changes to affected LEVEL_UP owners. */
final class QuestDependencyIndex {
	private final Map<Integer, List<Integer>> dependents;

	QuestDependencyIndex(QuestCatalog catalog) {
		Map<Integer, Set<Integer>> mutable = new LinkedHashMap<>();
		for (CompiledQuestDefinition definition : catalog.executables()) {
			var levelTransitions = definition.definition().transitions().stream()
				.filter(transition -> transition.event() instanceof QuestEvent.LevelUp)
				.toList();
			if (levelTransitions.isEmpty()) {
				continue;
			}
			Set<Integer> dependencies = new LinkedHashSet<>(definition.definition().metadata().prerequisites());
			for (QuestStartCondition condition : definition.definition().metadata().startConditions()) {
				if (!"equipped".equalsIgnoreCase(condition.type())) {
					dependencies.add(condition.questId());
				}
			}
			levelTransitions.stream().flatMap(transition -> transition.conditions().stream())
				.forEach(condition -> addConditionDependencies(dependencies, condition));
			for (int dependency : dependencies) {
				mutable.computeIfAbsent(dependency, ignored -> new LinkedHashSet<>()).add(definition.id());
			}
		}
		Map<Integer, List<Integer>> frozen = new LinkedHashMap<>();
		mutable.forEach((questId, owners) -> {
			List<Integer> sorted = new ArrayList<>(owners);
			Collections.sort(sorted);
			frozen.put(questId, List.copyOf(sorted));
		});
		this.dependents = Collections.unmodifiableMap(frozen);
	}

	List<Integer> dependentsOf(int changedQuestId) {
		if (changedQuestId <= 0) {
			throw new IllegalArgumentException("changedQuestId must be positive");
		}
		return dependents.getOrDefault(changedQuestId, List.of());
	}

	private static void addConditionDependencies(Set<Integer> dependencies, QuestCondition condition) {
		switch (condition) {
			case QuestCondition.QuestsFinished quests -> dependencies.addAll(quests.questIds());
			case QuestCondition.UnfinishedQuest quests -> dependencies.addAll(quests.questIds());
			case QuestCondition.NoAcquiredQuest quests -> dependencies.addAll(quests.questIds());
			case QuestCondition.AcquiredQuest quests -> dependencies.addAll(quests.questIds());
			default -> {
			}
		}
	}
}
