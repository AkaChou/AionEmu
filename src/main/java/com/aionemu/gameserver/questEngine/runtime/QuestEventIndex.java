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
import java.util.OptionalInt;

/** Deterministic event-to-owner index built only from compiled definitions. */
public final class QuestEventIndex {
	private final Map<QuestEvent, List<Route>> routes;

	public QuestEventIndex(QuestCatalog catalog) {
		List<CompiledQuestDefinition> definitions = new ArrayList<>(catalog.all());
		definitions.sort(Comparator.comparingInt(CompiledQuestDefinition::id));
		Map<QuestEvent, List<Route>> mutable = new LinkedHashMap<>();
		for (CompiledQuestDefinition definition : definitions) {
			for (QuestTransition transition : definition.definition().transitions()) {
				for (QuestEvent routeKey : routeKeys(transition.event())) {
					mutable.computeIfAbsent(routeKey, ignored -> new ArrayList<>())
							.add(new Route(definition.id(), transition));
				}
			}
		}
		Map<QuestEvent, List<Route>> frozen = new LinkedHashMap<>();
		Comparator<Route> routeOrder = Comparator.comparingInt(Route::questId)
			.thenComparing(route -> route.transition().priority(), Comparator.nullsLast(Integer::compareTo));
		mutable.forEach((event, entries) -> frozen.put(event,
			entries.stream().sorted(routeOrder).toList()));
		this.routes = Collections.unmodifiableMap(frozen);
	}

	private static List<QuestEvent> routeKeys(QuestEvent event) {
		if (event instanceof QuestEvent.KillNpcSet kills) {
			return kills.npcIds().stream().sorted()
				.map(npcId -> (QuestEvent) new QuestEvent.KillNpc(npcId)).toList();
		}
		return List.of(QuestEvent.routeKey(event));
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

	/**
	 * 返回指定物品的唯一播放时长；不同 owner 声明冲突时拒绝启动。
	 * Returns the unique play duration for an item; conflicting owner declarations fail startup.
	 *
	 * @param itemId 物品模板 ID / item template id
	 * 播放时长；未注册时为空 / play duration, or empty when unregistered
	 */
	public OptionalInt itemPlayAnimationMillis(int itemId) {
		if (itemId <= 0) {
			throw new IllegalArgumentException("itemId must be positive");
		}
		List<Route> candidates = routesFor(new QuestEvent.ItemPlay(itemId, 0));
		if (candidates.isEmpty()) {
			return OptionalInt.empty();
		}
		int duration = -1;
		for (Route route : candidates) {
			if (!(route.transition().event() instanceof QuestEvent.ItemPlay itemPlay)) {
				continue;
			}
			if (duration < 0) {
				duration = itemPlay.animationMillis();
			} else if (duration != itemPlay.animationMillis()) {
				throw new IllegalStateException("item " + itemId
					+ " has conflicting item-play animation durations");
			}
		}
		return OptionalInt.of(duration);
	}

	public Map<QuestEvent, List<Route>> routes() {
		return routes;
	}

	public record Route(int questId, QuestTransition transition) {
	}
}
