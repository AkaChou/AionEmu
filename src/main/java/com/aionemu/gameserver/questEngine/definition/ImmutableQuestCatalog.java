package com.aionemu.gameserver.questEngine.definition;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Deterministic immutable catalog; duplicate owners fail during construction. */
public final class ImmutableQuestCatalog implements QuestCatalog {
	private final Map<Integer, CompiledQuestDefinition> definitions;

	public ImmutableQuestCatalog(Collection<CompiledQuestDefinition> definitions) {
		Objects.requireNonNull(definitions, "definitions");
		Map<Integer, CompiledQuestDefinition> index = new LinkedHashMap<>();
		for (CompiledQuestDefinition definition : definitions) {
			Objects.requireNonNull(definition, "definition");
			if (index.putIfAbsent(definition.id(), definition) != null) {
				throw new QuestCompilationException("DUPLICATE_OWNER", "duplicate quest owner: " + definition.id());
			}
		}
		this.definitions = Collections.unmodifiableMap(index);
	}

	@Override
	public Optional<CompiledQuestDefinition> find(int questId) {
		return Optional.ofNullable(definitions.get(questId));
	}

	@Override
	public Collection<CompiledQuestDefinition> all() {
		return definitions.values();
	}
}
