package com.aionemu.gameserver.questEngine.graph;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record CompiledQuestGraph(int questId, int version, StateScope scope, String initialNode, Map<String, Variable> variables,
	Map<String, Node> nodes) {

	public CompiledQuestGraph {
		variables = Collections.unmodifiableMap(new LinkedHashMap<>(variables));
		nodes = Collections.unmodifiableMap(new LinkedHashMap<>(nodes));
	}

	public enum StateScope {
		PLAYER,
		PARTY,
		ALLIANCE,
		INSTANCE_RUN,
		WORLD
	}

	public enum EventType {
		DIALOG
	}

	public enum ConditionType {
		QUEST_STATUS
	}

	public enum ActionType {
		START_QUEST
	}

	public enum QuestStatus {
		NONE,
		START,
		REWARD,
		COMPLETE
	}

	public sealed interface Variable permits IntVariable, BooleanVariable {
		String name();

		StateScope scope();
	}

	public record IntVariable(String name, StateScope scope, int initial, int min, int max) implements Variable {
	}

	public record BooleanVariable(String name, StateScope scope, boolean initial) implements Variable {
	}

	public record Node(String id, boolean terminal, List<Transition> transitions) {
		public Node {
			transitions = List.copyOf(transitions);
		}
	}

	public record Transition(String id, int priority, String targetNode, Event event, List<Condition> conditions, List<Action> actions) {
		public Transition {
			conditions = List.copyOf(conditions);
			actions = List.copyOf(actions);
		}
	}

	public record Event(EventType type, int npcId, String dialog) {
	}

	public record Condition(ConditionType type, QuestStatus questStatus) {
	}

	public record Action(ActionType type) {
	}
}
