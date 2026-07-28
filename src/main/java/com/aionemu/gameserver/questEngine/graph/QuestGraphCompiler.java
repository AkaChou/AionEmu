package com.aionemu.gameserver.questEngine.graph;

import static com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.ActionType.START_QUEST;
import static com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EventType.DIALOG;
import static com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EventType.KILL;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.validation.SchemaFactory;

import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Action;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.BooleanVariable;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Condition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Event;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Node;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayerClassCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayerGenderCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayerLevelCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayerRaceCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.IntVariable;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatus;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatusCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.StateScope;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Transition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Variable;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraphData.EventKey;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraphData.EventRoute;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.DialogEventData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.GraphData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.KillEventData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.NodeData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.PlayerClassConditionData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.PlayerGenderConditionData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.PlayerLevelConditionData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.PlayerRaceConditionData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.QuestStatusConditionData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.StartQuestActionData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.TransitionData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.VariableData;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;

/**
 * 将经过 XSD 校验的 XML 任务图编译为强类型、不可变且引用闭合的运行时数据。
 * Compiles XSD-validated XML quest graphs into typed, immutable, reference-closed runtime data.
 */
public final class QuestGraphCompiler {

	/**
	 * 禁止实例化纯静态编译器。
	 * Prevents instantiation of this static compiler.
	 */
	private QuestGraphCompiler() {
	}

	/**
	 * 保存编译时允许引用的任务与 NPC 标识集合。
	 * Holds the quest and NPC identifiers allowed during compilation.
	 */
	public record References(Set<Integer> questIds, Set<Integer> npcIds) {
		/**
		 * 复制引用集合，保证一次编译期间引用闭包稳定。
		 * Copies reference sets so the reference closure stays stable during compilation.
		 */
		public References {
			questIds = Set.copyOf(questIds);
			npcIds = Set.copyOf(npcIds);
		}
	}

	/**
	 * 安全读取并校验 XML 文件，然后编译全部任务图。
	 * Safely reads and validates an XML file, then compiles all quest graphs.
	 *
	 * @param xmlFile 任务图 XML 文件 / quest graph XML file
	 * @param schemaFile 任务图 XSD 文件 / quest graph XSD file
	 * @param references 可引用的任务与 NPC / allowed quest and NPC references
	 * @return 已编译任务图数据 / compiled quest graph data
	 */
	public static CompiledQuestGraphData load(Path xmlFile, Path schemaFile, References references) {
		try {
			SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			Unmarshaller unmarshaller = JAXBContext.newInstance(QuestGraphData.class).createUnmarshaller();
			unmarshaller.setSchema(schemaFactory.newSchema(schemaFile.toFile()));
			XMLInputFactory inputFactory = XMLInputFactory.newFactory();
			inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
			inputFactory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
			try (InputStream stream = Files.newInputStream(xmlFile)) {
				XMLStreamReader reader = inputFactory.createXMLStreamReader(stream);
				try {
					return compile((QuestGraphData) unmarshaller.unmarshal(reader), references);
				} finally {
					reader.close();
				}
			}
		} catch (Exception e) {
			throw new IllegalArgumentException("Failed to load quest graphs from " + xmlFile, e);
		}
	}

	/**
	 * 校验 JAXB 数据的图结构、能力和引用，并生成确定性索引。
	 * Validates JAXB graph structure, capabilities, and references, then builds a deterministic index.
	 *
	 * @param source JAXB 任务图数据 / JAXB quest graph data
	 * @param references 可引用的任务与 NPC / allowed quest and NPC references
	 * @return 已编译任务图数据 / compiled quest graph data
	 */
	public static CompiledQuestGraphData compile(QuestGraphData source, References references) {
		if (source == null) {
			throw new IllegalArgumentException("Quest graph data is missing");
		}
		if (references == null) {
			throw new IllegalArgumentException("Quest graph references are missing");
		}
		Map<Integer, CompiledQuestGraph> graphs = new TreeMap<>();
		for (GraphData graph : source.getGraphs()) {
			CompiledQuestGraph compiled = compileGraph(graph, references);
			if (graphs.putIfAbsent(compiled.questId(), compiled) != null) {
				throw new IllegalArgumentException("Duplicate quest owner " + compiled.questId());
			}
		}
		Map<Integer, CompiledQuestGraph> immutableGraphs = Collections.unmodifiableMap(new LinkedHashMap<>(graphs));
		return new CompiledQuestGraphData(immutableGraphs, buildEventIndex(immutableGraphs));
	}

	/**
	 * 按事件目标建立稳定排序的分发索引。
	 * Builds a stably ordered dispatch index by event target.
	 */
	private static Map<EventKey, List<EventRoute>> buildEventIndex(Map<Integer, CompiledQuestGraph> graphs) {
		Comparator<EventKey> keyOrder = Comparator.comparing(EventKey::type).thenComparingInt(EventKey::targetId);
		Map<EventKey, List<EventRoute>> index = new TreeMap<>(keyOrder);
		graphs.values().forEach(graph -> graph.nodes().values().forEach(node -> node.transitions().forEach(transition ->
			index.computeIfAbsent(new EventKey(transition.event().type(), transition.event().npcId()), key -> new ArrayList<>())
				.add(new EventRoute(graph.questId(), node.id(), transition)))));
		Comparator<EventRoute> routeOrder = Comparator.comparingInt((EventRoute route) -> route.transition().priority())
			.thenComparingInt(EventRoute::questId).thenComparing(route -> route.transition().id());
		index.values().forEach(routes -> routes.sort(routeOrder));
		return index;
	}

	/**
	 * 编译并完整校验单个任务所有者的图定义。
	 * Compiles and fully validates one quest owner's graph definition.
	 */
	private static CompiledQuestGraph compileGraph(GraphData source, References references) {
		Integer questIdValue = source.getQuestId();
		Integer version = source.getVersion();
		if (questIdValue == null || questIdValue <= 0 || version == null || version <= 0) {
			throw new IllegalArgumentException("Quest graph has a missing or invalid id/version");
		}
		int questId = questIdValue;
		if (!references.questIds().contains(questId)) {
			throw new IllegalArgumentException("Quest graph owner references missing quest " + questId);
		}
		StateScope scope;
		try {
			scope = StateScope.valueOf(source.getScope());
		} catch (RuntimeException e) {
			throw new IllegalArgumentException("Quest " + questId + " has an invalid scope", e);
		}

		Map<String, NodeData> sourceNodes = new TreeMap<>();
		for (NodeData node : source.getNodes()) {
			if (sourceNodes.putIfAbsent(node.getId(), node) != null) {
				throw new IllegalArgumentException("Quest " + questId + " has duplicate node " + node.getId());
			}
		}
		if (!sourceNodes.containsKey(source.getInitialNode())) {
			throw new IllegalArgumentException("Quest " + questId + " has missing initial node " + source.getInitialNode());
		}
		Map<String, Variable> variables = compileVariables(questId, source.getVariables());

		Map<String, Node> nodes = new LinkedHashMap<>();
		Set<String> transitionIds = new HashSet<>();
		for (NodeData sourceNode : sourceNodes.values()) {
			List<Transition> transitions = new ArrayList<>();
			Set<String> priorities = new HashSet<>();
			for (TransitionData sourceTransition : sourceNode.getTransitions()) {
				if (sourceTransition.getPriority() == null || sourceTransition.getPriority() < 0) {
					throw new IllegalArgumentException("Quest " + questId + " transition " + sourceTransition.getId()
						+ " has a missing or invalid priority");
				}
				if (!transitionIds.add(sourceTransition.getId())) {
					throw new IllegalArgumentException("Quest " + questId + " has duplicate transition " + sourceTransition.getId());
				}
				Event event = compileEvent(questId, sourceTransition.getEvent(), references);
				String priorityKey = event.type() + ":" + sourceTransition.getPriority();
				if (!priorities.add(priorityKey)) {
					throw new IllegalArgumentException("Quest " + questId + " node " + sourceNode.getId()
						+ " has ambiguous " + event.type() + " priority " + sourceTransition.getPriority());
				}
				if (!sourceNodes.containsKey(sourceTransition.getTargetNode())) {
					throw new IllegalArgumentException("Quest " + questId + " transition " + sourceTransition.getId()
						+ " targets missing node " + sourceTransition.getTargetNode());
				}
				transitions.add(new Transition(sourceTransition.getId(), sourceTransition.getPriority(), sourceTransition.getTargetNode(), event,
					sourceTransition.getConditions().stream().map(value -> compileCondition(questId, value)).toList(),
					sourceTransition.getActions().stream().map(value -> compileAction(questId, value)).toList()));
			}
			transitions.sort(Comparator.comparingInt(Transition::priority).thenComparing(Transition::id));
			if (sourceNode.isTerminal() && !transitions.isEmpty()) {
				throw new IllegalArgumentException("Quest " + questId + " terminal node " + sourceNode.getId() + " has transitions");
			}
			if (!sourceNode.isTerminal() && transitions.isEmpty()) {
				throw new IllegalArgumentException("Quest " + questId + " non-terminal node " + sourceNode.getId() + " has no transitions");
			}
			nodes.put(sourceNode.getId(), new Node(sourceNode.getId(), sourceNode.isTerminal(), transitions));
		}

		validateReachability(questId, source.getInitialNode(), nodes);
		return new CompiledQuestGraph(questId, version, scope, source.getInitialNode(), variables, nodes);
	}

	/**
	 * 编译任务变量并拒绝重复名称或未知类型。
	 * Compiles quest variables and rejects duplicate names or unknown types.
	 */
	private static Map<String, Variable> compileVariables(int questId, List<VariableData> sourceVariables) {
		Map<String, Variable> variables = new TreeMap<>();
		for (VariableData source : sourceVariables) {
			StateScope scope;
			try {
				scope = StateScope.valueOf(source.getScope());
			} catch (RuntimeException e) {
				throw new IllegalArgumentException("Quest " + questId + " variable " + source.getName() + " has an invalid scope", e);
			}
			Variable variable = switch (source.getType()) {
				case "INT" -> compileIntVariable(questId, source, scope);
				case "BOOLEAN" -> compileBooleanVariable(questId, source, scope);
				case null, default -> throw new IllegalArgumentException("Quest " + questId + " variable " + source.getName()
					+ " has an unsupported type");
			};
			if (variables.putIfAbsent(variable.name(), variable) != null) {
				throw new IllegalArgumentException("Quest " + questId + " has duplicate variable " + variable.name());
			}
		}
		return Collections.unmodifiableMap(new LinkedHashMap<>(variables));
	}

	/**
	 * 校验整数边界和初值并生成强类型变量。
	 * Validates integer bounds and initial value and creates a typed variable.
	 */
	private static IntVariable compileIntVariable(int questId, VariableData source, StateScope scope) {
		if (source.getMin() == null || source.getMax() == null || source.getMin() > source.getMax()) {
			throw new IllegalArgumentException("Quest " + questId + " INT variable " + source.getName() + " has an invalid range");
		}
		try {
			int initial = Integer.parseInt(source.getInitial());
			if (initial < source.getMin() || initial > source.getMax()) {
				throw new IllegalArgumentException("Quest " + questId + " INT variable " + source.getName() + " initial value is out of range");
			}
			return new IntVariable(source.getName(), scope, initial, source.getMin(), source.getMax());
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Quest " + questId + " INT variable " + source.getName() + " has an invalid initial value", e);
		}
	}

	/**
	 * 校验布尔初值并生成强类型变量。
	 * Validates a boolean initial value and creates a typed variable.
	 */
	private static BooleanVariable compileBooleanVariable(int questId, VariableData source, StateScope scope) {
		if (source.getMin() != null || source.getMax() != null || !("true".equals(source.getInitial()) || "false".equals(source.getInitial()))) {
			throw new IllegalArgumentException("Quest " + questId + " BOOLEAN variable " + source.getName() + " is invalid");
		}
		return new BooleanVariable(source.getName(), scope, Boolean.parseBoolean(source.getInitial()));
	}

	/**
	 * 将受支持的 JAXB 事件编译为强类型事件并校验目标引用。
	 * Compiles a supported JAXB event into a typed event and validates its target reference.
	 */
	private static Event compileEvent(int questId, Object source, References references) {
		if (source instanceof DialogEventData dialog) {
			if (dialog.getNpcId() == null || dialog.getNpcId() <= 0 || dialog.getDialog() == null) {
				throw new IllegalArgumentException("Quest " + questId + " has an invalid dialog event");
			}
			if (!references.npcIds().contains(dialog.getNpcId())) {
				throw new IllegalArgumentException("Quest " + questId + " dialog references missing NPC " + dialog.getNpcId());
			}
			return new Event(DIALOG, dialog.getNpcId(), dialog.getDialog());
		}
		if (source instanceof KillEventData kill) {
			if (kill.getNpcId() == null || kill.getNpcId() <= 0) {
				throw new IllegalArgumentException("Quest " + questId + " has an invalid kill event");
			}
			if (!references.npcIds().contains(kill.getNpcId())) {
				throw new IllegalArgumentException("Quest " + questId + " kill references missing NPC " + kill.getNpcId());
			}
			return new Event(KILL, kill.getNpcId(), null);
		}
		throw new IllegalArgumentException("Quest " + questId + " has an unsupported event capability");
	}

	/**
	 * 将受支持的 JAXB 条件编译为强类型条件。
	 * Compiles a supported JAXB condition into a typed condition.
	 */
	private static Condition compileCondition(int questId, Object source) {
		if (source instanceof QuestStatusConditionData condition) {
			try {
				return new QuestStatusCondition(QuestStatus.valueOf(condition.getValue()));
			} catch (RuntimeException e) {
				throw new IllegalArgumentException("Quest " + questId + " has an invalid quest status", e);
			}
		}
		if (source instanceof PlayerLevelConditionData condition) {
			if (condition.getMin() == null || condition.getMin() <= 0
					|| condition.getMax() != null && condition.getMax() < condition.getMin()) {
				throw new IllegalArgumentException("Quest " + questId + " has an invalid player level range");
			}
			return new PlayerLevelCondition(condition.getMin(), condition.getMax());
		}
		if (source instanceof PlayerRaceConditionData condition) {
			try {
				EnumSet<Race> allowed = EnumSet.noneOf(Race.class);
				condition.getAllowed().forEach(value -> allowed.add(Race.valueOf(value)));
				if (allowed.isEmpty() || allowed.size() != condition.getAllowed().size()
						|| allowed.stream().anyMatch(race -> race != Race.ELYOS && race != Race.ASMODIANS)) {
					throw new IllegalArgumentException("unsupported player race");
				}
				return new PlayerRaceCondition(allowed);
			} catch (RuntimeException e) {
				throw new IllegalArgumentException("Quest " + questId + " has an invalid player race set", e);
			}
		}
		if (source instanceof PlayerClassConditionData condition) {
			try {
				EnumSet<PlayerClass> allowed = EnumSet.noneOf(PlayerClass.class);
				condition.getAllowed().forEach(value -> allowed.add(PlayerClass.valueOf(value)));
				if (allowed.isEmpty() || allowed.size() != condition.getAllowed().size() || allowed.contains(PlayerClass.ALL)) {
					throw new IllegalArgumentException("unsupported player class");
				}
				return new PlayerClassCondition(allowed);
			} catch (RuntimeException e) {
				throw new IllegalArgumentException("Quest " + questId + " has an invalid player class set", e);
			}
		}
		if (source instanceof PlayerGenderConditionData condition) {
			try {
				Gender expected = Gender.valueOf(condition.getValue());
				if (expected == Gender.DUMMY) {
					throw new IllegalArgumentException("unsupported player gender");
				}
				return new PlayerGenderCondition(expected);
			} catch (RuntimeException e) {
				throw new IllegalArgumentException("Quest " + questId + " has an invalid player gender", e);
			}
		}
		throw new IllegalArgumentException("Quest " + questId + " has an unsupported condition capability");
	}

	/**
	 * 将受支持的 JAXB 动作编译为强类型动作。
	 * Compiles a supported JAXB action into a typed action.
	 */
	private static Action compileAction(int questId, Object source) {
		if (source instanceof StartQuestActionData) {
			return new Action(START_QUEST);
		}
		throw new IllegalArgumentException("Quest " + questId + " has an unsupported action capability");
	}

	/**
	 * 证明所有节点从入口可达且至少存在一个可达终态。
	 * Proves every node is reachable from the entry and at least one reachable terminal state exists.
	 */
	private static void validateReachability(int questId, String initialNode, Map<String, Node> nodes) {
		Set<String> reachable = new HashSet<>();
		ArrayDeque<String> pending = new ArrayDeque<>();
		pending.add(initialNode);
		while (!pending.isEmpty()) {
			String nodeId = pending.removeFirst();
			if (reachable.add(nodeId)) {
				nodes.get(nodeId).transitions().stream().map(Transition::targetNode).forEach(pending::addLast);
			}
		}
		if (reachable.size() != nodes.size()) {
			List<String> unreachable = new ArrayList<>(nodes.keySet());
			unreachable.removeAll(reachable);
			throw new IllegalArgumentException("Quest " + questId + " has unreachable nodes " + unreachable);
		}
		if (reachable.stream().noneMatch(nodeId -> nodes.get(nodeId).terminal())) {
			throw new IllegalArgumentException("Quest " + questId + " has no reachable terminal node");
		}
	}
}
