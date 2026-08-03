package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestCatalog;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Deterministic event-to-owner index built only from compiled definitions. */
public final class QuestEventIndex {
	private final Map<QuestEvent, List<Route>> routes;

	public QuestEventIndex(QuestCatalog catalog) {
		List<CompiledQuestDefinition> definitions = new ArrayList<>(catalog.all());
		definitions.sort(Comparator.comparingInt(CompiledQuestDefinition::id));
		Map<QuestEvent, List<Route>> mutable = new LinkedHashMap<>();
		for (CompiledQuestDefinition definition : definitions) {
			for (QuestTransition transition : definition.definition().transitions()) {
				mutable.computeIfAbsent(QuestEvent.routeKey(transition.event()), ignored -> new ArrayList<>())
						.add(new Route(definition.id(), transition));
			}
		}
		Map<QuestEvent, List<Route>> frozen = new LinkedHashMap<>();
		Comparator<Route> routeOrder = Comparator.comparingInt(Route::questId)
			.thenComparing(route -> route.transition().priority(), Comparator.nullsLast(Integer::compareTo));
		mutable.forEach((event, entries) -> frozen.put(event,
			entries.stream().sorted(routeOrder).toList()));
		this.routes = Collections.unmodifiableMap(frozen);
	}

	public List<Route> routesFor(QuestEvent event) {
		return routes.getOrDefault(QuestEvent.routeKey(event), List.of());
	}

	public List<Route> routesFor(QuestEvent event, int questId) {
		if (questId <= 0) {
			throw new IllegalArgumentException("questId must be positive");
		}
		return routesFor(event).stream().filter(route -> route.questId() == questId).toList();
	}

	public Map<QuestEvent, List<Route>> routes() {
		return routes;
	}

	public record Route(int questId, QuestTransition transition) {
	}
}
