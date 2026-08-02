package com.aionemu.gameserver.questEngine.definition;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/** Atomically replaceable catalog; every reload is compiled and checked first. */
public final class QuestDefinitionRegistry implements QuestCatalog {
	private final AtomicReference<ImmutableQuestCatalog> active = new AtomicReference<>(
			new ImmutableQuestCatalog(List.of()));

	public void reload(Collection<QuestDefinition> sources) {
		Objects.requireNonNull(sources, "sources");
		Map<Integer, CompiledQuestDefinition> compiled = new LinkedHashMap<>();
		for (QuestDefinition source : sources) {
			CompiledQuestDefinition definition = QuestDefinitionCompiler.compile(source);
			if (compiled.putIfAbsent(definition.id(), definition) != null) {
				throw new QuestCompilationException("DUPLICATE_OWNER", "duplicate quest owner: " + definition.id());
			}
		}
		ImmutableQuestCatalog candidate = new ImmutableQuestCatalog(compiled.values());
		validateLayoutCompatibility(active.get(), candidate);
		active.set(candidate);
	}

	@Override
	public Optional<CompiledQuestDefinition> find(int questId) {
		return active.get().find(questId);
	}

	@Override
	public Collection<CompiledQuestDefinition> all() {
		return active.get().all();
	}

	private static void validateLayoutCompatibility(QuestCatalog oldCatalog, QuestCatalog candidate) {
		for (CompiledQuestDefinition oldDefinition : oldCatalog.all()) {
			CompiledQuestDefinition next = candidate.find(oldDefinition.id()).orElse(null);
			if (next != null && !oldDefinition.definition().progressLayout().equals(next.definition().progressLayout())) {
				throw new QuestCompilationException("INCOMPATIBLE_PROGRESS_LAYOUT",
						"reload changes progress layout for quest " + oldDefinition.id());
			}
		}
	}
}
