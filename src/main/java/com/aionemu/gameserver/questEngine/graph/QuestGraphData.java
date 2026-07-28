package com.aionemu.gameserver.questEngine.graph;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlList;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * 定义任务图 XML 的 JAXB 输入模型；该模型仅用于反序列化，运行时使用编译后的不可变数据。
 * Defines the JAXB input model for quest graph XML; runtime code uses the compiled immutable data instead.
 */
@XmlRootElement(name = "quest_graphs")
@XmlAccessorType(XmlAccessType.FIELD)
@Getter
public final class QuestGraphData {

	/**
	 * XML 中声明的任务图。
	 * Quest graphs declared in XML.
	 */
	@XmlElement(name = "quest_graph")
	private List<GraphData> graphs = new ArrayList<>();

	/**
	 * 表示一个任务所有者的原始图定义。
	 * Represents the raw graph definition for one quest owner.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(propOrder = { "variables", "nodes" })
	@Getter
	public static final class GraphData {
		/**
		 * 任务所有者标识。
		 * Quest owner identifier.
		 */
		@XmlAttribute(name = "quest_id", required = true)
		private Integer questId;
		/**
		 * 图格式版本。
		 * Graph format version.
		 */
		@XmlAttribute(required = true)
		private Integer version;
		/**
		 * 图状态的归属范围名称。
		 * Graph state scope name.
		 */
		@XmlAttribute(required = true)
		private String scope;
		/**
		 * 初始节点标识。
		 * Initial node identifier.
		 */
		@XmlAttribute(name = "initial_node", required = true)
		private String initialNode;
		/**
		 * XML 变量包装。
		 * XML variable wrapper.
		 */
		@XmlElement
		private VariablesData variables;
		/**
		 * 图节点定义。
		 * Graph node definitions.
		 */
		@XmlElement(name = "node", required = true)
		private List<NodeData> nodes = new ArrayList<>();

		/**
		 * 返回变量定义；XML 未声明变量时返回空列表。
		 * Returns variable definitions, or an empty list when XML declares none.
		 */
		public List<VariableData> getVariables() {
			return variables == null ? List.of() : variables.values;
		}

	}

	/**
	 * 包装 XML 中的变量列表。
	 * Wraps the variable list in XML.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	public static final class VariablesData {
		@XmlElement(name = "variable", required = true)
		private List<VariableData> values = new ArrayList<>();
	}

	/**
	 * 表示尚未类型化的 XML 变量定义。
	 * Represents an XML variable definition before type compilation.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class VariableData {
		/**
		 * 变量名称。
		 * Variable name.
		 */
		@XmlAttribute(required = true)
		private String name;
		/**
		 * 变量类型名称。
		 * Variable type name.
		 */
		@XmlAttribute(required = true)
		private String type;
		/**
		 * 变量状态范围名称。
		 * Variable state scope name.
		 */
		@XmlAttribute(required = true)
		private String scope;
		/**
		 * 变量的原始初值文本。
		 * Raw initial-value text of the variable.
		 */
		@XmlAttribute(required = true)
		private String initial;
		/**
		 * 可选的整数下界。
		 * Optional integer lower bound.
		 */
		@XmlAttribute
		private Integer min;
		/**
		 * 可选的整数上界。
		 * Optional integer upper bound.
		 */
		@XmlAttribute
		private Integer max;
	}

	/**
	 * 表示 XML 中的任务图节点。
	 * Represents a quest graph node in XML.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class NodeData {
		/**
		 * 节点标识。
		 * Node identifier.
		 */
		@XmlAttribute(required = true)
		private String id;
		/**
		 * 该节点是否为终态。
		 * Whether this node is terminal.
		 */
		@XmlAttribute
		private boolean terminal;
		/**
		 * 节点的出转换。
		 * Outgoing transitions of the node.
		 */
		@XmlElement(name = "transition")
		private List<TransitionData> transitions = new ArrayList<>();
	}

	/**
	 * 表示 XML 中带事件、条件和动作的图转换。
	 * Represents an XML graph transition with an event, conditions, and actions.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class TransitionData {
		/**
		 * 转换标识。
		 * Transition identifier.
		 */
		@XmlAttribute(required = true)
		private String id;
		/**
		 * 转换优先级。
		 * Transition priority.
		 */
		@XmlAttribute(required = true)
		private Integer priority;
		/**
		 * 目标节点标识。
		 * Target node identifier.
		 */
		@XmlAttribute(name = "to", required = true)
		private String targetNode;
		/**
		 * JAXB 事件负载。
		 * JAXB event payload.
		 */
		@XmlElements({
			@XmlElement(name = "dialog", type = DialogEventData.class, required = true),
			@XmlElement(name = "kill", type = KillEventData.class, required = true)
		})
		private Object event;
		/**
		 * XML 条件包装。
		 * XML condition wrapper.
		 */
		@XmlElement
		private ConditionsData conditions;
		/**
		 * XML 动作包装。
		 * XML action wrapper.
		 */
		@XmlElement
		private ActionsData actions;

		/**
		 * 返回条件负载；XML 未声明条件时返回空列表。
		 * Returns condition payloads, or an empty list when XML declares none.
		 */
		public List<Object> getConditions() {
			return conditions == null ? List.of() : conditions.values;
		}

		/**
		 * 返回动作负载；XML 未声明动作时返回空列表。
		 * Returns action payloads, or an empty list when XML declares none.
		 */
		public List<Object> getActions() {
			return actions == null ? List.of() : actions.values;
		}
	}

	/**
	 * 表示 NPC 对话事件的 XML 参数。
	 * Represents XML parameters for an NPC dialog event.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class DialogEventData {
		/**
		 * 对话目标 NPC 标识。
		 * Dialog target NPC identifier.
		 */
		@XmlAttribute(name = "npc_id", required = true)
		private Integer npcId;
		/**
		 * 客户端对话标识。
		 * Client dialog identifier.
		 */
		@XmlAttribute(required = true)
		private String dialog;
	}

	/**
	 * 表示 NPC 击杀事件的 XML 参数。
	 * Represents XML parameters for an NPC kill event.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class KillEventData {
		/**
		 * 被击杀 NPC 标识。
		 * Killed NPC identifier.
		 */
		@XmlAttribute(name = "npc_id", required = true)
		private Integer npcId;
	}

	/**
	 * 包装 XML 中的条件列表。
	 * Wraps the condition list in XML.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	public static final class ConditionsData {
		@XmlElements({
			@XmlElement(name = "quest-status", type = QuestStatusConditionData.class),
			@XmlElement(name = "player-level", type = PlayerLevelConditionData.class),
			@XmlElement(name = "player-race", type = PlayerRaceConditionData.class),
			@XmlElement(name = "player-class", type = PlayerClassConditionData.class),
			@XmlElement(name = "player-gender", type = PlayerGenderConditionData.class)
		})
		private List<Object> values = new ArrayList<>();
	}

	/**
	 * 表示任务状态条件的 XML 参数。
	 * Represents XML parameters for a quest-status condition.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class QuestStatusConditionData {
		/**
		 * 期望的任务状态名称。
		 * Expected quest status name.
		 */
		@XmlAttribute(required = true)
		private String value;
	}

	/**
	 * 表示玩家等级闭区间条件；max 缺失时没有上限。
	 * Represents an inclusive player-level range with no upper bound when max is absent.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class PlayerLevelConditionData {
		/** 最低玩家等级。 / Minimum player level. */
		@XmlAttribute(required = true)
		private Integer min;
		/** 可选最高玩家等级。 / Optional maximum player level. */
		@XmlAttribute
		private Integer max;
	}

	/**
	 * 表示允许的玩家阵营集合。
	 * Represents the allowed player-race set.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class PlayerRaceConditionData {
		/** 空格分隔的允许阵营。 / Space-separated allowed races. */
		@XmlList
		@XmlAttribute(name = "values", required = true)
		private List<String> allowed = new ArrayList<>();
	}

	/**
	 * 表示允许的玩家职业集合。
	 * Represents the allowed player-class set.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class PlayerClassConditionData {
		/** 空格分隔的允许职业。 / Space-separated allowed classes. */
		@XmlList
		@XmlAttribute(name = "values", required = true)
		private List<String> allowed = new ArrayList<>();
	}

	/**
	 * 表示要求的玩家性别。
	 * Represents the required player gender.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@Getter
	public static final class PlayerGenderConditionData {
		/** 期望性别名称。 / Expected gender name. */
		@XmlAttribute(required = true)
		private String value;
	}

	/**
	 * 包装 XML 中的动作列表。
	 * Wraps the action list in XML.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	public static final class ActionsData {
		@XmlElements(@XmlElement(name = "start-quest", type = StartQuestActionData.class))
		private List<Object> values = new ArrayList<>();
	}

	/**
	 * 标记启动任务动作；该动作当前不需要额外参数。
	 * Marks a start-quest action, which currently requires no additional parameters.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	public static final class StartQuestActionData {
	}
}
