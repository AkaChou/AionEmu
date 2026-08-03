package com.aionemu.gameserver.questEngine.definition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Validated immutable IR produced identically by XML and Java DSL inputs. */
public final class CompiledQuestDefinition {
	private final QuestDefinition definition;
	private final Map<String, List<QuestTransition>> transitionsByType;

	CompiledQuestDefinition(QuestDefinition definition) {
		this.definition = Objects.requireNonNull(definition, "definition");
		Map<String, List<QuestTransition>> index = new LinkedHashMap<>();
		for (QuestTransition transition : definition.transitions()) {
			index.computeIfAbsent(transition.event().type(), ignored -> new ArrayList<>()).add(transition);
		}
		Map<String, List<QuestTransition>> frozen = new LinkedHashMap<>();
		index.forEach((type, transitions) -> frozen.put(type, List.copyOf(transitions)));
		transitionsByType = Collections.unmodifiableMap(frozen);
	}

	public QuestDefinition definition() {
		return definition;
	}

	public int id() {
		return definition.id();
	}

	public int version() {
		return definition.version();
	}

	public List<QuestTransition> transitionsFor(String eventType) {
		return transitionsByType.getOrDefault(eventType, List.of());
	}

	public Map<String, List<QuestTransition>> transitionsByType() {
		return transitionsByType;
	}
}
