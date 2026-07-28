package com.aionemu.gameserver.questEngine.graph;

import static com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EventType.DIALOG;
import static com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EventType.ATTACK;
import static com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EventType.HOUSE_ITEM_USE;
import static com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EventType.ITEM_EQUIPPED;
import static com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EventType.ITEM_OBTAINED;
import static com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EventType.ITEM_USE;
import static com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EventType.KILL;
import static com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EventType.KILL_IN_WORLD;
import static com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EventType.LEVEL_UP;
import static com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EventType.MOVIE_ENDED;
import static com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EventType.NPC_PROXIMITY;
import static com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EventType.ESCORT_REACHED_TARGET;
import static com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EventType.ESCORT_LOST_TARGET;
import static com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EventType.PLAYER_DEATH;
import static com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EventType.PLAYER_LOGOUT;
import static com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EventType.QUEST_TIMER_ENDED;
import static com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EventType.WORLD_ENTERED;
import static com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EventType.ZONE_ENTERED;
import static com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EventType.ZONE_LEFT;
import static com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EventType.ZONE_MISSION_ENDED;

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
import java.util.Locale;
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
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.ActionPhase;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.AddCompletionCountAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.AddQuestVariableAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.AnchoredCooldownRepeatDeadlinePolicy;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.BooleanVariable;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.CloseDialogAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Condition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Event;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EventType;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EndQuestTimerAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.DailyRepeatDeadlinePolicy;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Node;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayerAbyssRankCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayerClassCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayerGenderCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayerEquippedCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayerInventoryCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayerLevelCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayerRaceCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayerTitleCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayerMessageChannel;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.IntVariable;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.FinishQuestAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.GiveQuestItemAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.NoRepeatDeadlinePolicy;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestCollectItemsCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestCompletionCountCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestRewardCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatus;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatusCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestRepeatAvailableCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestItemGrantMode;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestItemRemovalMode;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestVariableCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.RemoveCollectedItemsAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.RemoveQuestItemAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.RepeatDeadlinePolicy;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.RepeatTimeBasis;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.RepeatWeekday;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.SendDialogAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.SendPlayerMessageAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.SendRepeatDeadlineMessageAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.SetCompletionCountAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.SetQuestStatusAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.SetQuestVariableAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.ShowQuestListAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.StartQuestAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.StartQuestTimerAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.StateScope;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Transition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.SyncQuestStatusAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.SyncQuestTimerAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Variable;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.WeeklyRepeatDeadlinePolicy;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraphData.EventKey;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraphData.EventRoute;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.DialogEventData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.AttackEventData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.HouseItemUseEventData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.ItemEquippedEventData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.ItemObtainedEventData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.ItemUseEventData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.WorldEnteredEventData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.ZoneEnteredEventData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.ZoneLeftEventData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.ZoneMissionEndedEventData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.AddCompletionCountActionData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.AddQuestVariableActionData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.CloseDialogActionData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.FinishQuestActionData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.EndQuestTimerActionData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.GiveQuestItemActionData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.GraphData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.KillEventData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.KillInWorldEventData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.LevelUpEventData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.MovieEndedEventData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.NpcProximityEventData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.EscortReachedTargetEventData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.EscortLostTargetEventData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.NodeData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.PlayerAbyssRankConditionData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.PlayerClassConditionData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.PlayerGenderConditionData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.PlayerEquippedConditionData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.PlayerInventoryConditionData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.PlayerDeathEventData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.PlayerLogoutEventData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.PlayerLevelConditionData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.PlayerRaceConditionData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.PlayerTitleConditionData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.QuestCompletionCountConditionData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.QuestCollectItemsConditionData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.QuestRepeatAvailableConditionData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.QuestTimerEndedEventData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.QuestRewardConditionData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.QuestStatusConditionData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.QuestVariableConditionData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.RemoveCollectedItemsActionData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.RemoveQuestItemActionData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.SendDialogActionData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.SendPlayerMessageActionData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.SendRepeatDeadlineMessageActionData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.SetCompletionCountActionData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.SetQuestStatusActionData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.SetQuestVariableActionData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.ShowQuestListActionData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.StartQuestActionData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.StartQuestTimerActionData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.SyncQuestStatusActionData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.TransitionData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphData.VariableData;
import com.aionemu.gameserver.utils.stats.AbyssRankEnum;

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
	 * 保存编译时允许引用的任务、NPC、物品、称号、区域和影片标识集合。
	 * Holds the quest, NPC, item, title, zone, and movie identifiers allowed during compilation.
	 */
	public record References(Set<Integer> questIds, Set<Integer> npcIds, Set<Integer> itemIds, Set<Integer> titleIds,
			Set<String> zoneNames, Set<Integer> movieIds) {
		/**
		 * 复制引用集合，保证一次编译期间引用闭包稳定。
		 * Copies reference sets so the reference closure stays stable during compilation.
		 */
		public References {
			questIds = Set.copyOf(questIds);
			npcIds = Set.copyOf(npcIds);
			itemIds = Set.copyOf(itemIds);
			titleIds = Set.copyOf(titleIds);
			zoneNames = Set.copyOf(zoneNames);
			movieIds = Set.copyOf(movieIds);
		}
	}

	/**
	 * 安全读取并校验 XML 文件，然后编译全部任务图。
	 * Safely reads and validates an XML file, then compiles all quest graphs.
	 *
	 * @param xmlFile 任务图 XML 文件 / quest graph XML file
	 * @param schemaFile 任务图 XSD 文件 / quest graph XSD file
	 * @param references 可引用的任务、NPC、物品、称号、区域与影片 / allowed quest, NPC, item, title, zone, and movie references
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
	 * @param references 可引用的任务、NPC、物品、称号与区域 / allowed quest, NPC, item, title, and zone references
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
			index.computeIfAbsent(new EventKey(transition.event().type(), transition.event().targetId()), key -> new ArrayList<>())
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
		requireSupportedScope(questId, "graph", scope);

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
				List<Condition> conditions = sourceTransition.getConditions().stream()
					.map(value -> compileCondition(questId, value, references, variables)).toList();
				List<Action> actions = sourceTransition.getActions().stream()
					.flatMap(value -> compileActions(questId, value, variables, references).stream()).toList();
				validateActionOrder(questId, sourceTransition.getId(), actions);
				validateRepeatDeadlineProtocol(questId, sourceTransition.getId(), actions);
				transitions.add(new Transition(sourceTransition.getId(), sourceTransition.getPriority(), sourceTransition.getTargetNode(), event,
					conditions, actions));
			}
			transitions.sort(Comparator.comparingInt(Transition::priority).thenComparing(Transition::id));
			if (sourceNode.isTerminal() && transitions.stream().anyMatch(transition ->
					!isRepeatEntry(transition) && !isTerminalProtocolSelfLoop(sourceNode.getId(), transition))) {
				throw new IllegalArgumentException("Quest " + questId + " terminal node " + sourceNode.getId()
					+ " has a transition without repeat eligibility or a guarded protocol self-loop");
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
	 * 判断完成节点转换是否由显式的可重复条件保护。
	 * Returns whether a completed-node transition is guarded by explicit repeat availability.
	 */
	private static boolean isRepeatEntry(Transition transition) {
		return transition.conditions().stream().anyMatch(condition ->
			condition instanceof QuestRepeatAvailableCondition repeat && repeat.expectedAvailable());
	}

	/**
	 * 允许完成节点在“不可重复”时执行不改变状态的提交后协议自环。
	 * Allows a completed node to run a state-preserving post-commit protocol self-loop while repeat is unavailable.
	 */
	private static boolean isTerminalProtocolSelfLoop(String nodeId, Transition transition) {
		boolean completeState = transition.conditions().stream().anyMatch(condition ->
			condition instanceof QuestStatusCondition status && status.questId() == null
				&& status.operation() == com.aionemu.gameserver.questEngine.model.ConditionOperation.IN
				&& status.statuses().equals(Set.of(QuestStatus.COMPLETE)));
		boolean repeatUnavailable = transition.conditions().stream().anyMatch(condition ->
			condition instanceof QuestRepeatAvailableCondition repeat && !repeat.expectedAvailable());
		return transition.targetNode().equals(nodeId) && completeState && repeatUnavailable && !transition.actions().isEmpty()
			&& transition.actions().stream().allMatch(action -> action.type().phase() == ActionPhase.POST_COMMIT_PROTOCOL);
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
			requireSupportedScope(questId, "variable " + source.getName(), scope);
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
	 * 在 typed scope bridge 完成前仅允许当前 PLAYER 持久化与锁模型。
	 * Allows only the current PLAYER persistence and locking model until a typed scope bridge exists.
	 */
	private static void requireSupportedScope(int questId, String member, StateScope scope) {
		if (scope != StateScope.PLAYER) {
			throw new IllegalArgumentException("Quest " + questId + ' ' + member + " requires unsupported " + scope
				+ " scope without a typed runtime bridge");
		}
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
		if (source instanceof AttackEventData attack) {
			if (attack.getNpcId() == null || attack.getNpcId() <= 0 || !references.npcIds().contains(attack.getNpcId())) {
				throw new IllegalArgumentException("Quest " + questId + " attack references missing NPC " + attack.getNpcId());
			}
			return new Event(ATTACK, attack.getNpcId(), null);
		}
		if (source instanceof PlayerDeathEventData) {
			return new Event(PLAYER_DEATH, 0, null);
		}
		if (source instanceof KillInWorldEventData killInWorld) {
			if (killInWorld.getWorldId() == null || killInWorld.getWorldId() < 0) {
				throw new IllegalArgumentException("Quest " + questId + " has an invalid kill-in-world event");
			}
			return new Event(KILL_IN_WORLD, killInWorld.getWorldId(), null);
		}
		if (source instanceof ItemUseEventData itemUse) {
			return compileItemEvent(questId, "item-use", ITEM_USE, itemUse.getItemId(), references);
		}
		if (source instanceof ItemObtainedEventData itemObtained) {
			return compileItemEvent(questId, "item-obtained", ITEM_OBTAINED, itemObtained.getItemId(), references);
		}
		if (source instanceof ItemEquippedEventData itemEquipped) {
			return compileItemEvent(questId, "item-equipped", ITEM_EQUIPPED, itemEquipped.getItemId(), references);
		}
		if (source instanceof HouseItemUseEventData houseItemUse) {
			return compileItemEvent(questId, "house-item-use", HOUSE_ITEM_USE, houseItemUse.getItemId(), references);
		}
		if (source instanceof WorldEnteredEventData) {
			return new Event(WORLD_ENTERED, 0, null);
		}
		if (source instanceof ZoneEnteredEventData zoneEntered) {
			return compileZoneEvent(questId, "zone-entered", ZONE_ENTERED, zoneEntered.getZoneName(), references);
		}
		if (source instanceof ZoneLeftEventData zoneLeft) {
			return compileZoneEvent(questId, "zone-left", ZONE_LEFT, zoneLeft.getZoneName(), references);
		}
		if (source instanceof ZoneMissionEndedEventData) {
			return new Event(ZONE_MISSION_ENDED, questId, null);
		}
		if (source instanceof LevelUpEventData) {
			return new Event(LEVEL_UP, 0, null);
		}
		if (source instanceof PlayerLogoutEventData) {
			return new Event(PLAYER_LOGOUT, 0, null);
		}
		if (source instanceof QuestTimerEndedEventData timerEnded) {
			if (timerEnded.getTimer() == null || timerEnded.getTimer().isBlank()) {
				throw new IllegalArgumentException("Quest " + questId + " has an invalid quest-timer-ended event");
			}
			return new Event(QUEST_TIMER_ENDED, questId, timerEnded.getTimer());
		}
		if (source instanceof MovieEndedEventData movieEnded) {
			Integer movieId = movieEnded.getMovieId();
			if (movieId == null || movieId <= 0 || movieId > 0xFFFF || !references.movieIds().contains(movieId)) {
				throw new IllegalArgumentException("Quest " + questId + " movie-ended references missing movie " + movieId);
			}
			return new Event(MOVIE_ENDED, movieId, null);
		}
		if (source instanceof NpcProximityEventData proximity) {
			Integer npcId = proximity.getNpcId();
			if (npcId == null || npcId <= 0 || !references.npcIds().contains(npcId)) {
				throw new IllegalArgumentException("Quest " + questId + " npc-proximity references missing NPC " + npcId);
			}
			return new Event(NPC_PROXIMITY, npcId, null);
		}
		if (source instanceof EscortReachedTargetEventData) {
			return new Event(ESCORT_REACHED_TARGET, questId, null);
		}
		if (source instanceof EscortLostTargetEventData) {
			return new Event(ESCORT_LOST_TARGET, questId, null);
		}
		throw new IllegalArgumentException("Quest " + questId + " has an unsupported event capability");
	}

	/**
	 * 编译以物品模板为路由键的事件并强制引用闭包。
	 * Compiles an item-template-routed event and enforces reference closure.
	 */
	private static Event compileItemEvent(int questId, String eventName, EventType type, Integer itemId, References references) {
		if (itemId == null || itemId <= 0 || !references.itemIds().contains(itemId)) {
			throw new IllegalArgumentException("Quest " + questId + ' ' + eventName + " references missing item " + itemId);
		}
		return new Event(type, itemId, null);
	}

	/**
	 * 编译以规范化区域名称为路由键的事件并强制引用闭包。
	 * Compiles a canonical-zone-name-routed event and enforces reference closure.
	 */
	private static Event compileZoneEvent(int questId, String eventName, EventType type, String zoneName, References references) {
		if (zoneName == null || !zoneName.equals(zoneName.toUpperCase(Locale.ROOT)) || !references.zoneNames().contains(zoneName)) {
			throw new IllegalArgumentException("Quest " + questId + ' ' + eventName + " references missing zone " + zoneName);
		}
		return new Event(type, zoneName.hashCode(), zoneName);
	}

	/**
	 * 将受支持的 JAXB 条件编译为强类型条件。
	 * Compiles a supported JAXB condition into a typed condition.
	 */
	private static Condition compileCondition(int questId, Object source, References references, Map<String, Variable> variables) {
		if (source instanceof QuestStatusConditionData condition) {
			if (condition.getQuestId() != null && !references.questIds().contains(condition.getQuestId())) {
				throw new IllegalArgumentException("Quest " + questId + " status condition references missing quest " + condition.getQuestId());
			}
			try {
				EnumSet<QuestStatus> statuses = EnumSet.noneOf(QuestStatus.class);
				condition.getStatuses().forEach(value -> statuses.add(QuestStatus.valueOf(value)));
				if (statuses.size() != condition.getStatuses().size()) {
					throw new IllegalArgumentException("duplicate quest status");
				}
				return new QuestStatusCondition(condition.getQuestId(), condition.getOperation(), statuses);
			} catch (RuntimeException e) {
				throw new IllegalArgumentException("Quest " + questId + " has an invalid quest status", e);
			}
		}
		if (source instanceof QuestRewardConditionData condition) {
			if (condition.getQuestId() == null || !references.questIds().contains(condition.getQuestId())) {
				throw new IllegalArgumentException("Quest " + questId + " reward condition references missing quest " + condition.getQuestId());
			}
			try {
				return new QuestRewardCondition(condition.getQuestId(), condition.getRewardIndex());
			} catch (RuntimeException e) {
				throw new IllegalArgumentException("Quest " + questId + " has an invalid quest reward condition", e);
			}
		}
		if (source instanceof QuestVariableConditionData condition) {
			if (!(variables.get(condition.getVariable()) instanceof IntVariable) || condition.getValue() == null) {
				throw new IllegalArgumentException("Quest " + questId + " variable condition references a missing INT variable "
					+ condition.getVariable());
			}
			try {
				return new QuestVariableCondition(condition.getVariable(), condition.getOperation(), condition.getValue());
			} catch (RuntimeException e) {
				throw new IllegalArgumentException("Quest " + questId + " has an invalid quest variable condition", e);
			}
		}
		if (source instanceof QuestRepeatAvailableConditionData condition) {
			try {
				return new QuestRepeatAvailableCondition(condition.getMaxCompletions(), condition.getRequiresDeadline(),
					condition.getExpectedAvailable());
			} catch (RuntimeException e) {
				throw new IllegalArgumentException("Quest " + questId + " has an invalid repeat-available condition", e);
			}
		}
		if (source instanceof QuestCollectItemsConditionData) {
			return new QuestCollectItemsCondition();
		}
		if (source instanceof QuestCompletionCountConditionData condition) {
			if (condition.getQuestId() == null || !references.questIds().contains(condition.getQuestId())) {
				throw new IllegalArgumentException("Quest " + questId + " completion condition references missing quest " + condition.getQuestId());
			}
			try {
				return new QuestCompletionCountCondition(condition.getQuestId(), condition.getOperation(), condition.getCount());
			} catch (RuntimeException e) {
				throw new IllegalArgumentException("Quest " + questId + " has an invalid quest completion-count condition", e);
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
		if (source instanceof PlayerTitleConditionData condition) {
			if (condition.getTitleId() == null || condition.getTitleId() <= 0 || !references.titleIds().contains(condition.getTitleId())) {
				throw new IllegalArgumentException("Quest " + questId + " references missing title " + condition.getTitleId());
			}
			return new PlayerTitleCondition(condition.getTitleId());
		}
		if (source instanceof PlayerAbyssRankConditionData condition) {
			try {
				return new PlayerAbyssRankCondition(AbyssRankEnum.getRankById(condition.getMinimum()));
			} catch (RuntimeException e) {
				throw new IllegalArgumentException("Quest " + questId + " has an invalid minimum Abyss rank", e);
			}
		}
		if (source instanceof PlayerInventoryConditionData condition) {
			if (condition.getItemId() == null || condition.getItemId() <= 0 || !references.itemIds().contains(condition.getItemId())) {
				throw new IllegalArgumentException("Quest " + questId + " references missing item " + condition.getItemId());
			}
			try {
				return new PlayerInventoryCondition(condition.getItemId(), condition.getOperation(), condition.getCount());
			} catch (RuntimeException e) {
				throw new IllegalArgumentException("Quest " + questId + " has an invalid player inventory comparison", e);
			}
		}
		if (source instanceof PlayerEquippedConditionData condition) {
			if (condition.getItemId() == null || !references.itemIds().contains(condition.getItemId())) {
				throw new IllegalArgumentException("Quest " + questId + " equipped condition references missing item " + condition.getItemId());
			}
			return new PlayerEquippedCondition(condition.getItemId());
		}
		throw new IllegalArgumentException("Quest " + questId + " has an unsupported condition capability");
	}

	/**
	 * 将受支持的 JAXB 动作编译为强类型动作。
	 * Compiles a supported JAXB action into a typed action.
	 */
	private static Action compileAction(int questId, Object source, Map<String, Variable> variables, References references) {
		if (source instanceof StartQuestActionData) {
			return new StartQuestAction();
		}
		if (source instanceof SetQuestStatusActionData action) {
			try {
				return new SetQuestStatusAction(QuestStatus.valueOf(action.getStatus()));
			} catch (RuntimeException e) {
				throw new IllegalArgumentException("Quest " + questId + " has an invalid set-status action", e);
			}
		}
		if (source instanceof SetQuestVariableActionData action) {
			IntVariable variable = requireIntVariable(questId, action.getVariable(), variables);
			if (action.getValue() == null || action.getValue() < variable.min() || action.getValue() > variable.max()) {
				throw new IllegalArgumentException("Quest " + questId + " set-variable action is outside " + action.getVariable() + " bounds");
			}
			return new SetQuestVariableAction(action.getVariable(), action.getValue());
		}
		if (source instanceof AddQuestVariableActionData action) {
			requireIntVariable(questId, action.getVariable(), variables);
			try {
				return new AddQuestVariableAction(action.getVariable(), action.getDelta());
			} catch (RuntimeException e) {
				throw new IllegalArgumentException("Quest " + questId + " has an invalid add-variable action", e);
			}
		}
		if (source instanceof SetCompletionCountActionData action) {
			try {
				return new SetCompletionCountAction(action.getCount());
			} catch (RuntimeException e) {
				throw new IllegalArgumentException("Quest " + questId + " has an invalid set-completion-count action", e);
			}
		}
		if (source instanceof AddCompletionCountActionData action) {
			try {
				return new AddCompletionCountAction(action.getDelta());
			} catch (RuntimeException e) {
				throw new IllegalArgumentException("Quest " + questId + " has an invalid add-completion-count action", e);
			}
		}
		if (source instanceof GiveQuestItemActionData action) {
			if (action.getItemId() == null || !references.itemIds().contains(action.getItemId())) {
				throw new IllegalArgumentException("Quest " + questId + " give action references missing item " + action.getItemId());
			}
			try {
				return new GiveQuestItemAction(action.getItemId(), action.getCount(), QuestItemGrantMode.valueOf(action.getMode()));
			} catch (RuntimeException e) {
				throw new IllegalArgumentException("Quest " + questId + " has an invalid give-quest-item action", e);
			}
		}
		if (source instanceof RemoveQuestItemActionData action) {
			if (action.getItemId() == null || !references.itemIds().contains(action.getItemId())) {
				throw new IllegalArgumentException("Quest " + questId + " remove action references missing item " + action.getItemId());
			}
			try {
				return new RemoveQuestItemAction(action.getItemId(), action.getCount(), QuestItemRemovalMode.valueOf(action.getMode()));
			} catch (RuntimeException e) {
				throw new IllegalArgumentException("Quest " + questId + " has an invalid remove-quest-item action", e);
			}
		}
		if (source instanceof RemoveCollectedItemsActionData) {
			return new RemoveCollectedItemsAction();
		}
		if (source instanceof FinishQuestActionData action) {
			try {
				return new FinishQuestAction(action.getRewardIndex(), compileRepeatDeadlinePolicy(questId, "finish", action.getRepeatKind(),
					action.getTimeBasis(), action.getResetHour(), action.getWeekdays(), action.getCooldownSeconds(), false));
			} catch (RuntimeException e) {
				throw new IllegalArgumentException("Quest " + questId + " has an invalid finish action", e);
			}
		}
		if (source instanceof SendDialogActionData action) {
			try {
				return new SendDialogAction(action.getDialogId());
			} catch (RuntimeException e) {
				throw new IllegalArgumentException("Quest " + questId + " has an invalid dialog action", e);
			}
		}
		if (source instanceof CloseDialogActionData) {
			return new CloseDialogAction();
		}
		if (source instanceof ShowQuestListActionData) {
			return new ShowQuestListAction();
		}
		if (source instanceof SyncQuestStatusActionData) {
			return new SyncQuestStatusAction();
		}
		if (source instanceof SendRepeatDeadlineMessageActionData action) {
			try {
				return new SendRepeatDeadlineMessageAction(compileRepeatDeadlinePolicy(questId, "repeat message", action.getRepeatKind(),
					action.getTimeBasis(), action.getResetHour(), action.getWeekdays(), action.getCooldownSeconds(), true));
			} catch (RuntimeException e) {
				throw new IllegalArgumentException("Quest " + questId + " has an invalid repeat-deadline message action", e);
			}
		}
		if (source instanceof SendPlayerMessageActionData action) {
			try {
				return new SendPlayerMessageAction(action.getText(), PlayerMessageChannel.valueOf(action.getChannel()));
			} catch (RuntimeException e) {
				throw new IllegalArgumentException("Quest " + questId + " has an invalid player-message action", e);
			}
		}
		throw new IllegalArgumentException("Quest " + questId + " has an unsupported action capability");
	}

	/**
	 * 将一个 XML 动作编译为一个动作或带提交后协议的固定 typed expansion。
	 * Compiles one XML action into one action or a fixed typed expansion with post-commit protocol.
	 */
	private static List<Action> compileActions(int questId, Object source, Map<String, Variable> variables, References references) {
		try {
			if (source instanceof StartQuestTimerActionData timer) {
				return List.of(new StartQuestTimerAction(timer.getTimer(), timer.getDurationSeconds()),
					new SyncQuestTimerAction(timer.getTimer(), timer.getDurationSeconds()));
			}
			if (source instanceof EndQuestTimerActionData timer) {
				return List.of(new EndQuestTimerAction(timer.getTimer()), new SyncQuestTimerAction(timer.getTimer(), 0));
			}
			return List.of(compileAction(questId, source, variables, references));
		} catch (RuntimeException e) {
			if (e instanceof IllegalArgumentException && e.getMessage() != null && e.getMessage().startsWith("Quest ")) {
				throw e;
			}
			throw new IllegalArgumentException("Quest " + questId + " has an invalid quest-timer action", e);
		}
	}

	/**
	 * 将 XML repeat deadline 字段编译为封闭策略，并拒绝缺失、冲突或多余字段。
	 * Compiles XML repeat-deadline fields into a closed policy and rejects missing, conflicting, or extra fields.
	 */
	private static RepeatDeadlinePolicy compileRepeatDeadlinePolicy(int questId, String label, String kind, String timeBasis,
		Integer resetHour, List<String> weekdayValues, Long cooldownSeconds, boolean required) {
		List<String> weekdays = weekdayValues == null ? List.of() : weekdayValues;
		boolean hasAnyField = kind != null || timeBasis != null || resetHour != null || !weekdays.isEmpty() || cooldownSeconds != null;
		if (!hasAnyField) {
			if (required) {
				throw new IllegalArgumentException("Quest " + questId + ' ' + label + " repeat policy is missing");
			}
			return NoRepeatDeadlinePolicy.INSTANCE;
		}
		if (kind == null || timeBasis == null || resetHour == null) {
			throw new IllegalArgumentException("Quest " + questId + ' ' + label + " repeat policy is incomplete");
		}
		RepeatTimeBasis basis = RepeatTimeBasis.valueOf(timeBasis);
		return switch (kind) {
			case "DAILY" -> {
				if (!weekdays.isEmpty() || cooldownSeconds != null) {
					throw new IllegalArgumentException("Daily repeat policy has conflicting fields");
				}
				yield new DailyRepeatDeadlinePolicy(basis, resetHour);
			}
			case "WEEKLY" -> {
				if (weekdays.isEmpty() || cooldownSeconds != null) {
					throw new IllegalArgumentException("Weekly repeat policy has conflicting fields");
				}
				EnumSet<RepeatWeekday> parsed = EnumSet.noneOf(RepeatWeekday.class);
				weekdays.forEach(value -> parsed.add(RepeatWeekday.valueOf(value)));
				if (parsed.size() != weekdays.size()) {
					throw new IllegalArgumentException("Weekly repeat policy contains duplicate weekdays");
				}
				yield new WeeklyRepeatDeadlinePolicy(basis, parsed, resetHour);
			}
			case "ANCHORED_COOLDOWN" -> {
				if (!weekdays.isEmpty() || cooldownSeconds == null) {
					throw new IllegalArgumentException("Anchored cooldown repeat policy has conflicting fields");
				}
				yield new AnchoredCooldownRepeatDeadlinePolicy(basis, cooldownSeconds, resetHour);
			}
			default -> throw new IllegalArgumentException("Unknown repeat deadline kind " + kind);
		};
	}

	/**
	 * 返回已声明的整数变量，否则阻断编译。
	 * Returns a declared integer variable or blocks compilation.
	 */
	private static IntVariable requireIntVariable(int questId, String name, Map<String, Variable> variables) {
		if (!(variables.get(name) instanceof IntVariable variable)) {
			throw new IllegalArgumentException("Quest " + questId + " references a missing INT variable " + name);
		}
		return variable;
	}

	/**
	 * 强制状态/必需副作用位于提交后协议之前，避免发包早于状态提交。
	 * Forces state/required effects before post-commit protocol so packets cannot precede state commit.
	 */
	private static void validateActionOrder(int questId, String transitionId, List<Action> actions) {
		ActionPhase previous = ActionPhase.STATE;
		for (Action action : actions) {
			ActionPhase phase = action.type().phase();
			if (phase.ordinal() < previous.ordinal()) {
				throw new IllegalArgumentException("Quest " + questId + " transition " + transitionId + " has invalid action phase order");
			}
			previous = phase;
		}
	}

	/**
	 * 要求 repeat 完成动作与提交后提示一一对应且使用完全相同的策略。
	 * Requires a one-to-one, identical-policy match between repeat completion and its post-commit message.
	 */
	private static void validateRepeatDeadlineProtocol(int questId, String transitionId, List<Action> actions) {
		List<FinishQuestAction> finishes = actions.stream().filter(FinishQuestAction.class::isInstance)
			.map(FinishQuestAction.class::cast).toList();
		List<SendRepeatDeadlineMessageAction> messages = actions.stream().filter(SendRepeatDeadlineMessageAction.class::isInstance)
			.map(SendRepeatDeadlineMessageAction.class::cast).toList();
		if (finishes.size() > 1 || messages.size() > 1) {
			throw new IllegalArgumentException("Quest " + questId + " transition " + transitionId + " has duplicate finish/repeat protocol actions");
		}
		if (finishes.isEmpty()) {
			if (!messages.isEmpty()) {
				throw new IllegalArgumentException("Quest " + questId + " transition " + transitionId + " has repeat protocol without finish");
			}
			return;
		}
		RepeatDeadlinePolicy policy = finishes.getFirst().repeatDeadlinePolicy();
		if (policy == NoRepeatDeadlinePolicy.INSTANCE) {
			if (!messages.isEmpty()) {
				throw new IllegalArgumentException("Quest " + questId + " transition " + transitionId + " has repeat protocol without deadline");
			}
		} else if (messages.size() != 1 || !policy.equals(messages.getFirst().repeatDeadlinePolicy())) {
			throw new IllegalArgumentException("Quest " + questId + " transition " + transitionId + " has mismatched repeat deadline protocol");
		}
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
