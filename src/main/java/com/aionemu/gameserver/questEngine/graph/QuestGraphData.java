package com.aionemu.gameserver.questEngine.graph;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlRootElement(name = "quest_graphs")
@XmlAccessorType(XmlAccessType.FIELD)
public final class QuestGraphData {

	@XmlElement(name = "quest_graph")
	private List<GraphData> graphs = new ArrayList<>();

	public List<GraphData> getGraphs() {
		return graphs;
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(propOrder = { "variables", "nodes" })
	public static final class GraphData {
		@XmlAttribute(name = "quest_id", required = true)
		private Integer questId;
		@XmlAttribute(required = true)
		private Integer version;
		@XmlAttribute(required = true)
		private String scope;
		@XmlAttribute(name = "initial_node", required = true)
		private String initialNode;
		@XmlElement
		private VariablesData variables;
		@XmlElement(name = "node", required = true)
		private List<NodeData> nodes = new ArrayList<>();

		public Integer getQuestId() {
			return questId;
		}

		public Integer getVersion() {
			return version;
		}

		public String getScope() {
			return scope;
		}

		public String getInitialNode() {
			return initialNode;
		}

		public List<VariableData> getVariables() {
			return variables == null ? List.of() : variables.values;
		}

		public List<NodeData> getNodes() {
			return nodes;
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static final class VariablesData {
		@XmlElement(name = "variable", required = true)
		private List<VariableData> values = new ArrayList<>();
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static final class VariableData {
		@XmlAttribute(required = true)
		private String name;
		@XmlAttribute(required = true)
		private String type;
		@XmlAttribute(required = true)
		private String scope;
		@XmlAttribute(required = true)
		private String initial;
		@XmlAttribute
		private Integer min;
		@XmlAttribute
		private Integer max;

		public String getName() {
			return name;
		}

		public String getType() {
			return type;
		}

		public String getScope() {
			return scope;
		}

		public String getInitial() {
			return initial;
		}

		public Integer getMin() {
			return min;
		}

		public Integer getMax() {
			return max;
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static final class NodeData {
		@XmlAttribute(required = true)
		private String id;
		@XmlAttribute
		private boolean terminal;
		@XmlElement(name = "transition")
		private List<TransitionData> transitions = new ArrayList<>();

		public String getId() {
			return id;
		}

		public boolean isTerminal() {
			return terminal;
		}

		public List<TransitionData> getTransitions() {
			return transitions;
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static final class TransitionData {
		@XmlAttribute(required = true)
		private String id;
		@XmlAttribute(required = true)
		private Integer priority;
		@XmlAttribute(name = "to", required = true)
		private String targetNode;
		@XmlElements(@XmlElement(name = "dialog", type = DialogEventData.class, required = true))
		private Object event;
		@XmlElement
		private ConditionsData conditions;
		@XmlElement
		private ActionsData actions;

		public String getId() {
			return id;
		}

		public Integer getPriority() {
			return priority;
		}

		public String getTargetNode() {
			return targetNode;
		}

		public Object getEvent() {
			return event;
		}

		public List<Object> getConditions() {
			return conditions == null ? List.of() : conditions.values;
		}

		public List<Object> getActions() {
			return actions == null ? List.of() : actions.values;
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static final class DialogEventData {
		@XmlAttribute(name = "npc_id", required = true)
		private Integer npcId;
		@XmlAttribute(required = true)
		private String dialog;

		public Integer getNpcId() {
			return npcId;
		}

		public String getDialog() {
			return dialog;
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static final class ConditionsData {
		@XmlElements(@XmlElement(name = "quest-status", type = QuestStatusConditionData.class))
		private List<Object> values = new ArrayList<>();
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static final class QuestStatusConditionData {
		@XmlAttribute(required = true)
		private String value;

		public String getValue() {
			return value;
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static final class ActionsData {
		@XmlElements(@XmlElement(name = "start-quest", type = StartQuestActionData.class))
		private List<Object> values = new ArrayList<>();
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static final class StartQuestActionData {
	}
}
