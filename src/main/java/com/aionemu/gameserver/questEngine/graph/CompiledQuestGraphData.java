package com.aionemu.gameserver.questEngine.graph;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EventType;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Transition;

public record CompiledQuestGraphData(Map<Integer, CompiledQuestGraph> graphs, Map<EventKey, List<EventRoute>> eventIndex) {

	public CompiledQuestGraphData {
		graphs = Collections.unmodifiableMap(new LinkedHashMap<>(graphs));
		Map<EventKey, List<EventRoute>> immutableIndex = new LinkedHashMap<>();
		eventIndex.forEach((key, routes) -> immutableIndex.put(key, List.copyOf(routes)));
		eventIndex = Collections.unmodifiableMap(immutableIndex);
	}

	public record EventKey(EventType type, int targetId) {
	}

	public record EventRoute(int questId, String nodeId, Transition transition) {
	}
}
