package com.aionemu.gameserver.questEngine.e2e.journey;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.QuestDrop;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestNode;
import com.aionemu.gameserver.questEngine.definition.QuestTimerPolicy;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.e2e.client.ClientResourceOracle;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 只根据生产目录编译出的任务图和 Aion 5.8 客户端可见按钮，规划一条从 NONE 到 COMPLETE 的
 * 确定性连续路径；它不读取手写 Golden Journey，也不修改任务状态。
 * Plans one deterministic continuous path from NONE to COMPLETE using only a quest graph compiled from the
 * production directory and Aion 5.8 client-visible buttons; it never reads handwritten golden journeys or mutates
 * quest state.
 */
public final class QuestProductionJourneyPlanner {
	private static final int DEFAULT_MAX_STEPS = 256;
	private static final int MAX_EXPANDED_STATES = 4_096;

	/** 规划步骤使用的客户端或世界入口。 / Client or world ingress used by one planned step. */
	public enum StepKind {
		INTERACT,
		TARGETLESS_ACTION,
		PAGE_ACTION,
		CLIENT_LOCAL_FINISH_DIALOG,
		NATIVE_REWARD_ACTION,
		USE_OBJECT,
		USE_OBJECT_DROP,
		USE_ITEM,
		ITEM_PLAY,
		WORLD_EVENT
	}

	/** 一步生产 transition 及其真实入口形态。 / One production transition and its real ingress shape. */
	public record PlannedStep(StepKind kind, QuestTransition transition, QuestDrop metadataDrop) {
		public PlannedStep(StepKind kind, QuestTransition transition) {
			this(kind, transition, null);
		}

		public PlannedStep {
			kind = Objects.requireNonNull(kind, "kind");
			if (kind == StepKind.CLIENT_LOCAL_FINISH_DIALOG) {
				if (transition != null || metadataDrop != null) {
					throw new IllegalArgumentException("client-local finish dialog must not invent a production transition");
				}
			} else {
				transition = Objects.requireNonNull(transition, "transition");
				if (kind == StepKind.USE_OBJECT_DROP) {
					metadataDrop = Objects.requireNonNull(metadataDrop, "metadataDrop");
				} else if (metadataDrop != null) {
					throw new IllegalArgumentException("only object-drop steps may carry metadata drops");
				}
			}
		}
	}

	/** 成功规划出的单条完整路径。 / One successfully planned complete path. */
	public record Plan(int questId, String startNode, String completeNode, PlayerClass playerClass,
			Map<Integer, Integer> initialInventory, List<PlannedStep> steps) {
		public Plan(int questId, String startNode, String completeNode, List<PlannedStep> steps) {
			this(questId, startNode, completeNode, PlayerClass.GLADIATOR, Map.of(), steps);
		}

		public Plan(int questId, String startNode, String completeNode, PlayerClass playerClass,
				List<PlannedStep> steps) {
			this(questId, startNode, completeNode, playerClass, Map.of(), steps);
		}

		public Plan {
			if (questId <= 0) throw new IllegalArgumentException("questId must be positive");
			startNode = requireText(startNode, "startNode");
			completeNode = requireText(completeNode, "completeNode");
			playerClass = Objects.requireNonNull(playerClass, "playerClass");
			initialInventory = Map.copyOf(initialInventory);
			steps = List.copyOf(steps);
			if (steps.isEmpty()) throw new IllegalArgumentException("journey plan must not be empty");
		}
	}

	/**
	 * 无法从权威生产图规划完整路径时的首个确定性阻塞点。
	 * First deterministic blocker when the production graph cannot yield a complete path.
	 */
	public record Failure(int questId, String node, int page, String reason) {
		public Failure {
			if (questId <= 0) throw new IllegalArgumentException("questId must be positive");
			node = Objects.requireNonNullElse(node, "");
			if (page < 0) throw new IllegalArgumentException("page must be non-negative");
			reason = requireText(reason, "reason");
		}
	}

	/**
	 * 规划结果；成功时只有 plan，失败时只有 failure。
	 * Planning result containing exactly one plan or failure.
	 */
	public record Result(Plan plan, Failure failure) {
		public Result {
			if ((plan == null) == (failure == null)) {
				throw new IllegalArgumentException("result must contain exactly one plan or failure");
			}
		}

		public boolean planned() {
			return plan != null;
		}
	}

	/** 使用默认步骤上限规划一条路径。 / Plans one path with the default step limit. */
	public Result plan(CompiledQuestDefinition definition, ClientResourceOracle oracle) {
		return plan(definition, oracle, DEFAULT_MAX_STEPS);
	}

	/**
	 * 使用显式步骤上限规划一条路径，避免生产图中的恢复环或自循环无限展开。
	 * Plans one path with an explicit step limit so recovery edges and self-loops in production graphs cannot expand
	 * forever.
	 */
	public Result plan(CompiledQuestDefinition definition, ClientResourceOracle oracle, int maxSteps) {
		Objects.requireNonNull(definition, "definition");
		Objects.requireNonNull(oracle, "oracle");
		if (maxSteps <= 0) throw new IllegalArgumentException("maxSteps must be positive");

		Map<String, QuestNode> nodes = new LinkedHashMap<>();
		for (QuestNode node : definition.definition().nodes()) nodes.put(node.label(), node);
		List<QuestNode> starts = nodes.values().stream()
			.filter(node -> node.projection().status() == QuestStatus.NONE)
			.toList();
		if (starts.isEmpty()) {
			return failed(definition.id(), "", 0, "production quest has no NONE entry node");
		}

		ArrayDeque<PathState> pending = new ArrayDeque<>();
		Set<StateKey> visited = new HashSet<>();
		for (QuestNode start : starts) {
			for (PlayerClass playerClass : playerClasses(definition)) {
				Map<String, Integer> variables = new LinkedHashMap<>(
					definition.definition().progressLayout().unpack(0));
				variables.putAll(start.projection().variables());
				PathState state = new PathState(start.label(), start.projection().status(), 0, 0, 0, false, false,
					playerClass, variables, Map.of(), List.of());
				pending.addLast(state);
				visited.add(state.key());
			}
		}
		Failure firstBlocker = null;
		int expandedStates = 0;
		while (!pending.isEmpty()) {
			PathState current = pending.removeFirst();
			if (++expandedStates > MAX_EXPANDED_STATES) {
				return failed(definition.id(), current.node(), current.page(),
					"journey planning exceeded the deterministic state budget " + MAX_EXPANDED_STATES);
			}
			if (current.status() == QuestStatus.COMPLETE && !current.steps().isEmpty()) {
				QuestTransition ingress = current.steps().stream().map(PlannedStep::transition)
					.filter(Objects::nonNull).findFirst().orElseThrow();
				return new Result(new Plan(definition.id(), ingress.sourceNode(),
					current.node(), current.playerClass(), requiredInitialInventory(current.steps()), current.steps()), null);
			}
			if (current.steps().size() >= maxSteps) {
				if (firstBlocker == null) {
					firstBlocker = new Failure(definition.id(), current.node(), current.page(),
						"journey exceeded the configured step limit " + maxSteps);
				}
				continue;
			}

			List<Choice> choices = choices(definition, oracle, current, nodes);
			if (choices.isEmpty() && current.page() > 0) {
				List<ClientResourceOracle.ClientAction> visible = oracle.visibleActions(definition.id(), current.page());
				if (isClientLocalFinishDialog(visible)) {
					PathState next = finishDialogLocally(current);
					if (visited.add(next.key())) pending.addLast(next);
					continue;
				}
				if (!visible.isEmpty() || current.page() == QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id()) {
					if (firstBlocker == null) {
						firstBlocker = new Failure(definition.id(), current.node(), current.page(),
							visible.isEmpty() ? "native reward window has no completion route"
								: "client-visible page actions have no route from the current production node");
					}
					continue;
				}
				choices = externalChoices(definition, oracle, current, nodes);
			}
			for (Choice choice : choices) {
				PathState next = advance(definition, current, choice, nodes);
				if (visited.add(next.key())) pending.addLast(next);
			}
		}
		return firstBlocker == null
			? failed(definition.id(), starts.getFirst().label(), 0,
				"production graph has no client-reachable path to a COMPLETE node")
			: new Result(null, firstBlocker);
	}

	private static List<Choice> choices(CompiledQuestDefinition definition, ClientResourceOracle oracle,
			PathState state, Map<String, QuestNode> nodes) {
		if (state.activeMovieId() > 0) {
			return firstRoutes(fromNode(definition, state, nodes).stream()
				.filter(transition -> transition.event() instanceof QuestEvent.MovieEnd movie
					&& movie.movieId() == state.activeMovieId())
				.map(transition -> new Choice(StepKind.WORLD_EVENT, transition, null))
				.toList());
		}
		if (state.page() == 0) return externalChoices(definition, oracle, state, nodes);
		Set<Integer> visibleActions = oracle.visibleActions(definition.id(), state.page()).stream()
			.map(ClientResourceOracle.ClientAction::actionId)
			.collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
		boolean rewardWindow = state.page() == QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id();
		List<Choice> choices = new ArrayList<>();
		for (QuestTransition transition : fromNode(definition, state, nodes)) {
			StepKind kind = pageKind(transition.event(), state.npcId(), visibleActions, rewardWindow);
			if (kind != null) choices.add(new Choice(kind, transition, null));
		}
		return firstRoutes(choices);
	}

	private static List<Choice> externalChoices(CompiledQuestDefinition definition, ClientResourceOracle oracle,
			PathState state, Map<String, QuestNode> nodes) {
		List<Choice> choices = new ArrayList<>();
		for (QuestTransition transition : fromNode(definition, state, nodes)) {
			Choice choice = externalChoice(definition, oracle, state, nodes, transition);
			if (choice != null) choices.add(choice);
		}
		return firstRoutes(choices);
	}

	private static Choice externalChoice(CompiledQuestDefinition definition, ClientResourceOracle oracle,
			PathState state, Map<String, QuestNode> nodes, QuestTransition transition) {
		if (transition.event() instanceof QuestEvent.TalkToNpc talk && talk.dialogId() == null) {
			QuestDrop drop = deterministicObjectDrop(definition, state, nodes, talk.npcId());
			if (drop != null) return new Choice(StepKind.USE_OBJECT_DROP, transition, drop);
		}
		StepKind kind = externalKind(transition.event(), state, oracle);
		return kind == null ? null : new Choice(kind, transition, null);
	}

	private static List<QuestTransition> fromNode(CompiledQuestDefinition definition, PathState state,
			Map<String, QuestNode> nodes) {
		return definition.definition().transitions().stream()
			.filter(transition -> sourceMatches(nodes, transition, state))
			.filter(transition -> conditionsMatch(definition, transition, state))
			.toList();
	}

	private static boolean sourceMatches(Map<String, QuestNode> nodes, QuestTransition transition,
			PathState state) {
		QuestNode source = nodes.get(transition.sourceNode());
		if (source == null || source.projection().status() != state.status()) return false;
		return source.projection().variables().entrySet().stream()
			.allMatch(entry -> Objects.equals(state.variables().get(entry.getKey()), entry.getValue()));
	}

	private static boolean conditionsMatch(CompiledQuestDefinition definition, QuestTransition transition,
			PathState state) {
		for (QuestCondition condition : transition.conditions()) {
			boolean matches = switch (condition) {
				case QuestCondition.StatusIs required -> required.status() == state.status();
				case QuestCondition.HasItem ignored when state.steps().isEmpty() -> true;
				case QuestCondition.QuestVariableIs ignored when state.steps().isEmpty() -> true;
				case QuestCondition.VariableAtLeast ignored when state.steps().isEmpty() -> true;
				case QuestCondition.VariableBelow ignored when state.steps().isEmpty() -> true;
				case QuestCondition.VariableSumIs ignored when state.steps().isEmpty() -> true;
				case QuestCondition.VariableSumBelow ignored when state.steps().isEmpty() -> true;
				case QuestCondition.HasItem item ->
					(state.inventory().getOrDefault(item.itemId(), 0) >= item.count()) == item.expected();
				case QuestCondition.QuestVariableIs variable ->
					state.variables().getOrDefault(variable.field(), 0) == variable.value();
				case QuestCondition.VariableAtLeast variable ->
					state.variables().getOrDefault(variable.field(), 0) >= variable.value();
				case QuestCondition.VariableBelow variable ->
					state.variables().getOrDefault(variable.field(), 0) < variable.value();
				case QuestCondition.VariableSumIs sum -> variableSum(state, sum.fields()) == sum.value();
				case QuestCondition.VariableSumBelow sum -> variableSum(state, sum.fields()) < sum.value();
				case QuestCondition.PlayerClassIs playerClass ->
					PlayerClass.getStartingClassFor(state.playerClass()) == playerClass.startingClass();
				case QuestCondition.AdvancedClassIs playerClass ->
					state.playerClass() == playerClass.playerClass();
				default -> true;
			};
			if (!matches) return false;
		}
		return true;
	}

	private static int variableSum(PathState state, List<String> fields) {
		return fields.stream().mapToInt(field -> state.variables().getOrDefault(field, 0)).sum();
	}

	private static StepKind pageKind(QuestEvent event, int currentNpcId, Set<Integer> visibleActions,
			boolean rewardWindow) {
		if (event instanceof QuestEvent.TalkToNpc talk && talk.dialogId() != null) {
			if (rewardWindow && isNativeRewardAction(talk.dialogId()) && currentNpcId == talk.npcId()) {
				return StepKind.NATIVE_REWARD_ACTION;
			}
			if (visibleActions.contains(talk.dialogId()) && currentNpcId == talk.npcId()) {
				return StepKind.PAGE_ACTION;
			}
		}
		if (event instanceof QuestEvent.QuestDialog dialog) {
			if (currentNpcId != 0) return null;
			if (rewardWindow && isNativeRewardAction(dialog.dialogId())) return StepKind.NATIVE_REWARD_ACTION;
			if (visibleActions.contains(dialog.dialogId())) return StepKind.PAGE_ACTION;
		}
		return null;
	}

	private static StepKind externalKind(QuestEvent event, PathState state, ClientResourceOracle oracle) {
		if (event instanceof QuestEvent.TalkToNpc talk) {
			if (talk.dialogId() == null || talk.dialogId() == 31) return StepKind.INTERACT;
			if (talk.dialogId() == -1) return StepKind.USE_OBJECT;
			return null;
		}
		if (event instanceof QuestEvent.QuestDialog dialog) {
			return oracle.actionExists(dialog.dialogId()) ? StepKind.TARGETLESS_ACTION : null;
		}
		if (event instanceof QuestEvent.CanAct) return null;
		if (event instanceof QuestEvent.MovieEnd movie) {
			return movie.movieId() == state.activeMovieId() ? StepKind.WORLD_EVENT : null;
		}
		if (event instanceof QuestEvent.QuestTimerEnd) {
			return state.visibleTimerActive() ? StepKind.WORLD_EVENT : null;
		}
		if (event instanceof QuestEvent.InvisibleTimerEnd) {
			return state.invisibleTimerActive() ? StepKind.WORLD_EVENT : null;
		}
		if (event instanceof QuestEvent.Abandon || event instanceof QuestEvent.LogOut
				|| event instanceof QuestEvent.EnterWorld) {
			return null;
		}
		if (event instanceof QuestEvent.UseItem) return StepKind.USE_ITEM;
		if (event instanceof QuestEvent.ItemPlay) return StepKind.ITEM_PLAY;
		return StepKind.WORLD_EVENT;
	}

	/**
	 * 只在页面唯一按钮是客户端本地结束动作时关闭页面；若还有其他无 route 按钮，不能借此掩盖 XML 缺口。
	 * Closes a page locally only when its sole visible action is the client-local finish action; another unrouted
	 * button must remain an XML-evidence blocker rather than being hidden by this fallback.
	 */
	private static boolean isClientLocalFinishDialog(List<ClientResourceOracle.ClientAction> visible) {
		return !visible.isEmpty() && visible.stream()
			.allMatch(action -> action.actionId() == QuestDialogAction.FINISH_DIALOG.id());
	}

	/**
	 * 从当前节点可用的 ACTION_ITEM_USE gate 与 100% 生产掉落中选择一个仍被后续条件需要的交互物掉落。
	 * Selects an action-item drop still needed by a later condition, using only a current-node ACTION_ITEM_USE gate
	 * and a deterministic (100%) production metadata drop.
	 */
	private static QuestDrop deterministicObjectDrop(CompiledQuestDefinition definition, PathState state,
			Map<String, QuestNode> nodes, int npcId) {
		boolean gated = fromNode(definition, state, nodes).stream()
			.map(QuestTransition::event)
			.filter(QuestEvent.CanAct.class::isInstance)
			.map(QuestEvent.CanAct.class::cast)
			.anyMatch(canAct -> canAct.templateId() == npcId && "ACTION_ITEM_USE".equals(canAct.actionType()));
		if (!gated) return null;
		return definition.definition().metadata().drops().stream()
			.filter(drop -> drop.npcId() == npcId && drop.chance() == 100)
			.filter(drop -> itemIsStillNeeded(definition, state, nodes, drop.itemId()))
			.findFirst().orElse(null);
	}

	/**
	 * 仅为当前状态可达且确实要求更多该物品的生产 route 模拟掉落，避免无界重复采集。
	 * Simulates a drop only for a route reachable from the current state that genuinely needs more of the item,
	 * preventing unbounded repeated collection.
	 */
	private static boolean itemIsStillNeeded(CompiledQuestDefinition definition, PathState state,
			Map<String, QuestNode> nodes, int itemId) {
		int currentCount = state.inventory().getOrDefault(itemId, 0);
		return definition.definition().transitions().stream()
			.filter(transition -> sourceMatches(nodes, transition, state))
			.flatMap(transition -> transition.conditions().stream())
			.filter(QuestCondition.HasItem.class::isInstance)
			.map(QuestCondition.HasItem.class::cast)
			.anyMatch(item -> item.expected() && item.itemId() == itemId && item.count() > currentCount);
	}

	private static boolean isNativeRewardAction(int actionId) {
		return actionId >= QuestDialogAction.SELECTED_QUEST_REWARD1.id()
			&& actionId <= QuestDialogAction.SELECTED_QUEST_NOREWARD.id();
	}

	private static List<Choice> firstRoutes(List<Choice> choices) {
		Map<QuestEvent, Choice> firstByRoute = new LinkedHashMap<>();
		choices.stream()
			.sorted(java.util.Comparator.comparing(
				choice -> choice.transition().priority(), java.util.Comparator.nullsLast(Integer::compareTo)))
			.forEach(choice -> firstByRoute.putIfAbsent(choice.transition().event(), choice));
		return List.copyOf(firstByRoute.values());
	}

	private static PathState advance(CompiledQuestDefinition definition, PathState current, Choice choice,
			Map<String, QuestNode> nodes) {
		int page = current.page();
		boolean dialogStateChanged = false;
		int activeMovieId = current.activeMovieId();
		boolean visibleTimerActive = current.visibleTimerActive();
		boolean invisibleTimerActive = current.invisibleTimerActive();
		if (choice.transition().event() instanceof QuestEvent.MovieEnd) activeMovieId = 0;
		if (choice.transition().event() instanceof QuestEvent.QuestTimerEnd) visibleTimerActive = false;
		if (choice.transition().event() instanceof QuestEvent.InvisibleTimerEnd) invisibleTimerActive = false;
		for (AfterCommitAction action : choice.transition().afterCommit()) {
			if (action instanceof AfterCommitAction.CloseDialog) {
				page = 0;
				dialogStateChanged = true;
			} else if (action instanceof AfterCommitAction.ShowQuestDialog show) {
				page = show.dialogId();
				dialogStateChanged = true;
			} else if (action instanceof AfterCommitAction.ShowQuestSelectionDialog show) {
				page = show.dialogId();
				dialogStateChanged = true;
			} else if (action instanceof AfterCommitAction.ShowDialogWindow show) {
				page = show.dialogId();
				dialogStateChanged = true;
			} else if (action instanceof AfterCommitAction.PlayMovie movie) {
				activeMovieId = movie.movieId();
			} else if (action instanceof AfterCommitAction.PlayMovieRandom random) {
				activeMovieId = random.movieIds().getFirst();
			} else if (action instanceof AfterCommitAction.StartQuestTimer) {
				visibleTimerActive = true;
			} else if (action instanceof AfterCommitAction.StartInvisibleTimer) {
				invisibleTimerActive = true;
			} else if (action instanceof AfterCommitAction.CancelQuestTimer cancel) {
				if (QuestTimerPolicy.VISIBLE_TIMER_ID.equals(cancel.identity().timerId())) {
					visibleTimerActive = false;
				} else if (QuestTimerPolicy.INVISIBLE_TIMER_ID.equals(cancel.identity().timerId())) {
					invisibleTimerActive = false;
				}
			}
		}
		int playedMovieId = activeMovieId;
		if (playedMovieId > 0 && definition.definition().transitions().stream().noneMatch(transition ->
			choice.transition().targetNode().equals(transition.sourceNode())
				&& transition.event() instanceof QuestEvent.MovieEnd movie
				&& movie.movieId() == playedMovieId)) {
			activeMovieId = 0;
		}
		int npcId = page == 0 ? 0 : !dialogStateChanged ? current.npcId()
			: choice.transition().event() instanceof QuestEvent.TalkToNpc talk ? talk.npcId() : 0;
		Map<String, Integer> variables = new LinkedHashMap<>(current.variables());
		Map<Integer, Integer> inventory = new LinkedHashMap<>(current.inventory());
		if (current.steps().isEmpty()) seedInitialConditions(choice.transition(), variables, inventory);
		if (choice.transition().event() instanceof QuestEvent.UseItem use) {
			inventory.putIfAbsent(use.itemId(), 1);
		} else if (choice.transition().event() instanceof QuestEvent.ItemPlay itemPlay) {
			inventory.putIfAbsent(itemPlay.itemId(), 1);
		} else if (choice.transition().event() instanceof QuestEvent.GetItem get) {
			inventory.merge(get.itemId(), 1, Math::max);
		} else if (choice.transition().event() instanceof QuestEvent.CollectItem collect) {
			inventory.merge(collect.itemId(), collect.count(), Math::max);
		}
		if (choice.metadataDrop() != null) {
			inventory.merge(choice.metadataDrop().itemId(), 1, Integer::sum);
		}
		QuestNode target = nodes.get(choice.transition().targetNode());
		QuestStatus status = target == null ? current.status() : target.projection().status();
		Set<String> actionTouchedFields = new HashSet<>();
		for (QuestAction action : choice.transition().actions()) {
			switch (action) {
				case QuestAction.SetStatus set -> status = set.status();
				case QuestAction.SetVariable set -> {
					variables.put(set.field(), set.value());
					actionTouchedFields.add(set.field());
				}
				case QuestAction.IncrementVariable increment -> {
					variables.merge(increment.field(), increment.delta(), Integer::sum);
					actionTouchedFields.add(increment.field());
				}
				case QuestAction.GiveItem give -> inventory.merge(give.itemId(), give.count(), Integer::sum);
				case QuestAction.RemoveItem remove -> {
					if (remove.removeAll()) inventory.remove(remove.itemId());
					else inventory.computeIfPresent(remove.itemId(), (ignored, count) ->
						count <= remove.count() ? null : count - remove.count());
				}
				default -> { }
			}
		}
		if (target != null) {
			target.projection().variables().forEach((field, value) -> {
				if (!actionTouchedFields.contains(field)) variables.put(field, value);
			});
		}
		List<PlannedStep> steps = new ArrayList<>(current.steps());
		steps.add(new PlannedStep(choice.kind(), choice.transition(), choice.metadataDrop()));
		return new PathState(choice.transition().targetNode(), status, page, npcId, activeMovieId,
			visibleTimerActive, invisibleTimerActive, current.playerClass(), Map.copyOf(variables),
			Map.copyOf(inventory), List.copyOf(steps));
	}

	private static List<PlayerClass> playerClasses(CompiledQuestDefinition definition) {
		Set<PlayerClass> classes = new java.util.LinkedHashSet<>();
		for (QuestTransition transition : definition.definition().transitions()) {
			for (QuestCondition condition : transition.conditions()) {
				if (condition instanceof QuestCondition.AdvancedClassIs advanced) {
					classes.add(advanced.playerClass());
				} else if (condition instanceof QuestCondition.PlayerClassIs starting) {
					classes.add(representativeClass(starting.startingClass()));
				}
			}
		}
		return classes.isEmpty() ? List.of(PlayerClass.GLADIATOR) : List.copyOf(classes);
	}

	private static Map<Integer, Integer> requiredInitialInventory(List<PlannedStep> steps) {
		Map<Integer, Integer> inventory = new LinkedHashMap<>();
		Map<Integer, Integer> required = new LinkedHashMap<>();
		for (PlannedStep step : steps) {
			if (step.transition() == null) continue;
			for (QuestCondition condition : step.transition().conditions()) {
				if (condition instanceof QuestCondition.HasItem item && item.expected()) {
					int deficit = Math.max(0, item.count() - inventory.getOrDefault(item.itemId(), 0));
					if (deficit > 0) {
						required.merge(item.itemId(), deficit, Integer::sum);
						inventory.merge(item.itemId(), deficit, Integer::sum);
					}
				}
			}
			switch (step.transition().event()) {
				case QuestEvent.UseItem use -> inventory.putIfAbsent(use.itemId(), 1);
				case QuestEvent.ItemPlay itemPlay -> inventory.putIfAbsent(itemPlay.itemId(), 1);
				case QuestEvent.GetItem get -> inventory.merge(get.itemId(), 1, Math::max);
				case QuestEvent.CollectItem collect ->
					inventory.merge(collect.itemId(), collect.count(), Math::max);
				default -> { }
			}
			for (QuestAction action : step.transition().actions()) {
				if (action instanceof QuestAction.GiveItem give) {
					inventory.merge(give.itemId(), give.count(), Integer::sum);
				} else if (action instanceof QuestAction.RemoveItem remove) {
					if (remove.removeAll()) {
						inventory.remove(remove.itemId());
					} else {
						int count = inventory.getOrDefault(remove.itemId(), 0);
						int deficit = Math.max(0, remove.count() - count);
						if (deficit > 0) {
							required.merge(remove.itemId(), deficit, Integer::sum);
							count += deficit;
						}
						if (count == remove.count()) inventory.remove(remove.itemId());
						else inventory.put(remove.itemId(), count - remove.count());
					}
				}
			}
			if (step.metadataDrop() != null) {
				inventory.merge(step.metadataDrop().itemId(), 1, Integer::sum);
			}
		}
		return Map.copyOf(required);
	}

	private static PlayerClass representativeClass(PlayerClass startingClass) {
		return java.util.Arrays.stream(PlayerClass.values())
			.filter(playerClass -> playerClass != PlayerClass.ALL && !playerClass.isStartingClass())
			.filter(playerClass -> PlayerClass.getStartingClassFor(playerClass) == startingClass)
			.findFirst().orElse(startingClass);
	}

	private static void seedInitialConditions(QuestTransition transition, Map<String, Integer> variables,
			Map<Integer, Integer> inventory) {
		for (QuestCondition condition : transition.conditions()) {
			switch (condition) {
				case QuestCondition.HasItem item when item.expected() ->
					inventory.merge(item.itemId(), item.count(), Math::max);
				case QuestCondition.HasItem item -> inventory.remove(item.itemId());
				case QuestCondition.QuestVariableIs variable -> variables.put(variable.field(), variable.value());
				case QuestCondition.VariableAtLeast variable ->
					variables.merge(variable.field(), variable.value(), Math::max);
				case QuestCondition.VariableBelow variable when variable.value() > 0 ->
					variables.compute(variable.field(), (ignored, value) ->
						value == null || value >= variable.value() ? variable.value() - 1 : value);
				default -> { }
			}
		}
	}

	private static Result failed(int questId, String node, int page, String reason) {
		return new Result(null, new Failure(questId, node, page, reason));
	}

	/**
	 * 记录已发送真实 FINISH_DIALOG 但服务端按历史合同不处理后的客户端本地页面关闭。
	 * Records the client-side page close after a real FINISH_DIALOG request that the server deliberately leaves
	 * unhandled under the historical contract.
	 */
	private static PathState finishDialogLocally(PathState current) {
		List<PlannedStep> steps = new ArrayList<>(current.steps());
		steps.add(new PlannedStep(StepKind.CLIENT_LOCAL_FINISH_DIALOG, null));
		return new PathState(current.node(), current.status(), 0, current.npcId(), current.activeMovieId(),
			current.visibleTimerActive(), current.invisibleTimerActive(), current.playerClass(), current.variables(),
			current.inventory(), List.copyOf(steps));
	}

	private static String requireText(String value, String field) {
		if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
		return value;
	}

	/** 规划器内部的一条候选边。 / One candidate edge internal to the planner. */
	private record Choice(StepKind kind, QuestTransition transition, QuestDrop metadataDrop) {
	}

	/**
	 * BFS 队列中的任务、客户端页面与权威回调状态。
	 * Quest, client-page, and authoritative callback state in the BFS queue.
	 */
	private record PathState(String node, QuestStatus status, int page, int npcId, int activeMovieId,
			boolean visibleTimerActive, boolean invisibleTimerActive, PlayerClass playerClass,
			Map<String, Integer> variables,
			Map<Integer, Integer> inventory, List<PlannedStep> steps) {
		private PathState {
			status = Objects.requireNonNull(status, "status");
			playerClass = Objects.requireNonNull(playerClass, "playerClass");
			variables = Map.copyOf(variables);
			inventory = Map.copyOf(inventory);
			steps = List.copyOf(steps);
		}

		private StateKey key() {
			return new StateKey(node, status, page, npcId, activeMovieId, visibleTimerActive, invisibleTimerActive,
				playerClass, variables, inventory);
		}
	}

	/**
	 * 防止恢复边、页面和回调自循环重复扩展的稳定状态键。
	 * Stable state key preventing recovery, page, and callback-loop expansion.
	 */
	private record StateKey(String node, QuestStatus status, int page, int npcId, int activeMovieId,
			boolean visibleTimerActive, boolean invisibleTimerActive, PlayerClass playerClass,
			Map<String, Integer> variables,
			Map<Integer, Integer> inventory) {
	}
}
