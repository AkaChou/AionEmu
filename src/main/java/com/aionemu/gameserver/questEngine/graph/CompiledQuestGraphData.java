package com.aionemu.gameserver.questEngine.graph;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EventType;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Transition;

/**
 * 保存全部已编译任务图及用于事件分发的确定性索引。
 * Holds all compiled quest graphs and the deterministic index used for event dispatch.
 */
public record CompiledQuestGraphData(Map<Integer, CompiledQuestGraph> graphs, Map<EventKey, List<EventRoute>> eventIndex) {

	/**
	 * 复制任务图和事件索引，防止加载后的数据被修改。
	 * Copies graphs and the event index so loaded data cannot be mutated.
	 */
	public CompiledQuestGraphData {
		graphs = Collections.unmodifiableMap(new LinkedHashMap<>(graphs));
		Map<EventKey, List<EventRoute>> immutableIndex = new LinkedHashMap<>();
		eventIndex.forEach((key, routes) -> immutableIndex.put(key, List.copyOf(routes)));
		eventIndex = Collections.unmodifiableMap(immutableIndex);
	}

	/**
	 * 表示事件类型与目标对象组成的索引键。
	 * Represents an index key composed of an event type and target object.
	 */
	public record EventKey(EventType type, int targetId) {
	}

	/**
	 * 表示事件命中后可评估的任务图转换位置。
	 * Represents a quest graph transition location eligible for event evaluation.
	 */
	public record EventRoute(int questId, String nodeId, Transition transition) {
	}
}
