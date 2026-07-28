package com.aionemu.gameserver.questEngine.graph;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 表示经过校验、可供运行时直接消费的不可变任务图。
 * Represents an immutable, validated quest graph ready for runtime consumption.
 */
public record CompiledQuestGraph(int questId, int version, StateScope scope, String initialNode, Map<String, Variable> variables,
	Map<String, Node> nodes) {

	/**
	 * 复制图集合，防止编译后的定义被调用方修改。
	 * Copies graph collections so callers cannot mutate compiled definitions.
	 */
	public CompiledQuestGraph {
		variables = Collections.unmodifiableMap(new LinkedHashMap<>(variables));
		nodes = Collections.unmodifiableMap(new LinkedHashMap<>(nodes));
	}

	/**
	 * 定义任务变量和运行状态的归属范围。
	 * Defines the ownership scope of quest variables and runtime state.
	 */
	public enum StateScope {
		PLAYER,
		PARTY,
		ALLIANCE,
		INSTANCE_RUN,
		WORLD
	}

	/**
	 * 列出当前编译器已证明并支持的事件能力。
	 * Lists event capabilities currently proven and supported by the compiler.
	 */
	public enum EventType {
		DIALOG,
		KILL
	}

	/**
	 * 列出当前编译器已证明并支持的条件能力。
	 * Lists condition capabilities currently proven and supported by the compiler.
	 */
	public enum ConditionType {
		QUEST_STATUS
	}

	/**
	 * 列出当前编译器已证明并支持的动作能力。
	 * Lists action capabilities currently proven and supported by the compiler.
	 */
	public enum ActionType {
		START_QUEST
	}

	/**
	 * 表示任务生命周期中可供条件判断的状态。
	 * Represents quest lifecycle states available to conditions.
	 */
	public enum QuestStatus {
		NONE,
		START,
		REWARD,
		COMPLETE
	}

	/**
	 * 定义所有强类型任务变量共享的最小合同。
	 * Defines the minimal contract shared by all typed quest variables.
	 */
	public sealed interface Variable permits IntVariable, BooleanVariable {
		/**
		 * 返回变量在任务图内的唯一名称。
		 * Returns the variable's unique name within the quest graph.
		 */
		String name();

		/**
		 * 返回变量状态的归属范围。
		 * Returns the ownership scope of the variable state.
		 */
		StateScope scope();
	}

	/**
	 * 表示带显式初值和边界的整数变量。
	 * Represents an integer variable with an explicit initial value and bounds.
	 */
	public record IntVariable(String name, StateScope scope, int initial, int min, int max) implements Variable {
	}

	/**
	 * 表示带显式初值的布尔变量。
	 * Represents a boolean variable with an explicit initial value.
	 */
	public record BooleanVariable(String name, StateScope scope, boolean initial) implements Variable {
	}

	/**
	 * 表示任务图节点及其按优先级排序的出边。
	 * Represents a quest graph node and its priority-ordered outgoing transitions.
	 */
	public record Node(String id, boolean terminal, List<Transition> transitions) {
		/**
		 * 复制转换列表，保持节点定义不可变。
		 * Copies the transition list to keep the node definition immutable.
		 */
		public Node {
			transitions = List.copyOf(transitions);
		}
	}

	/**
	 * 表示由事件触发并经过条件、动作后跳转的有向边。
	 * Represents a directed edge triggered by an event and followed by conditions and actions.
	 */
	public record Transition(String id, int priority, String targetNode, Event event, List<Condition> conditions, List<Action> actions) {
		/**
		 * 复制条件和动作列表，保持转换定义不可变。
		 * Copies condition and action lists to keep the transition definition immutable.
		 */
		public Transition {
			conditions = List.copyOf(conditions);
			actions = List.copyOf(actions);
		}
	}

	/**
	 * 表示已类型化的任务事件及其目标参数。
	 * Represents a typed quest event and its target parameters.
	 */
	public record Event(EventType type, int npcId, String dialog) {
	}

	/**
	 * 表示转换执行前必须满足的已类型化条件。
	 * Represents a typed condition that must hold before a transition executes.
	 */
	public record Condition(ConditionType type, QuestStatus questStatus) {
	}

	/**
	 * 表示转换命中后执行的已类型化动作。
	 * Represents a typed action executed after a transition matches.
	 */
	public record Action(ActionType type) {
	}
}
