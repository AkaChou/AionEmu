package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Structural compiler shared by XML and Java DSL front ends. */
public final class QuestDefinitionCompiler {
	private QuestDefinitionCompiler() {
	}

	public static CompiledQuestDefinition compile(QuestDefinition definition) {
		definition = restoreRewardPreviewContract(Objects.requireNonNull(definition, "definition"));
		if (definition.nodes().isEmpty()) {
			fail("NO_NODES", "executable definition has no nodes");
		}
		if (definition.transitions().isEmpty()) {
			fail("NO_TRANSITIONS", "executable definition has no transitions");
		}
		if (definition.metadata().maxCountLimitedQuest() > 1) {
			fail("LIMITED_QUEST_START_UNSUPPORTED",
				"limited quest quota acquisition is not part of the shared transaction");
		}

		Map<String, QuestNode> nodes = new HashMap<>();
		Set<String> projections = new HashSet<>();
		for (QuestNode node : definition.nodes()) {
			if (nodes.putIfAbsent(node.label(), node) != null) {
				fail("DUPLICATE_NODE", "duplicate node: " + node.label());
			}
			for (String variable : node.projection().variables().keySet()) {
				if (definition.progressLayout().field(variable) == null) {
					fail("UNKNOWN_PROGRESS_FIELD", "node references unknown field: " + variable);
				}
			}
			int packedProjection = definition.progressLayout().pack(node.projection().variables());
			String projectionKey = node.projection().status().name() + ":" + packedProjection;
			if (!projections.add(projectionKey)) {
				fail("DUPLICATE_NODE_PROJECTION", "nodes share the same QuestStatus + quest_vars projection: "
						+ projectionKey);
			}
		}

		Map<String, Set<String>> outgoing = new HashMap<>();
		for (QuestTransition transition : definition.transitions()) {
			if (transition.sourceNode() != null && !nodes.containsKey(transition.sourceNode())) {
				fail("BAD_NODE_REFERENCE", "transition has unknown source node: " + transition.sourceNode());
			}
			if (!nodes.containsKey(transition.targetNode())) {
				fail("BAD_NODE_REFERENCE", "transition points to unknown node: " + transition.targetNode());
			}
			for (QuestCondition condition : transition.conditions()) {
				if (condition instanceof QuestCondition.PvpVictimLevelDelta
						&& !(transition.event() instanceof QuestEvent.KillInWorld)) {
					fail("PVP_CONDITION_EVENT_MISMATCH",
						"pvp-victim-level-delta is only valid on kill-in-world events");
				}
				if (condition instanceof QuestCondition.PvpRecipientInZone
						&& !(transition.event() instanceof QuestEvent.KillRanked
						|| transition.event() instanceof QuestEvent.KillInWorld)) {
					fail("PVP_CONDITION_EVENT_MISMATCH",
						"pvp-recipient-in-zone is only valid on PvP kill events");
				}
				if (condition instanceof QuestCondition.QuestVariableIs variable) {
					validateProgressField(definition, variable.field(), "condition");
					definition.progressLayout().pack(Map.of(variable.field(), variable.value()));
				}
				if (condition instanceof QuestCondition.VariableAtLeast variable) {
					validateProgressField(definition, variable.field(), "condition");
				}
				if (condition instanceof QuestCondition.VariableBelow variable) {
					validateProgressField(definition, variable.field(), "condition");
				}
				if (condition instanceof QuestCondition.VariableSumIs variable) {
					validateProgressFields(definition, variable.fields(), "condition");
				}
				if (condition instanceof QuestCondition.VariableSumBelow variable) {
					validateProgressFields(definition, variable.fields(), "condition");
				}
			}
			for (QuestAction action : transition.actions()) {
				if (action instanceof QuestAction.GrantSelectedReward selected
						&& selected.rewardIndex() >= definition.metadata().rewards().size()) {
					fail("SELECTED_REWARD_INDEX_OUT_OF_RANGE",
						"selected reward index " + selected.rewardIndex() + " is not present in quest metadata");
				}
				if (action instanceof QuestAction.SetVariable variable) {
					if (definition.progressLayout().field(variable.field()) == null) {
						fail("UNKNOWN_PROGRESS_FIELD", "action references unknown field: " + variable.field());
					}
					definition.progressLayout().pack(Map.of(variable.field(), variable.value()));
				}
				if (action instanceof QuestAction.GrantReward reward
						&& reward.amountMode() == QuestRewardAmountMode.QUEST_BASE
						&& reward.rewardKind() != QuestRewardKind.GOLD
						&& reward.rewardKind() != QuestRewardKind.KINAH
						&& reward.rewardKind() != QuestRewardKind.AP
						&& reward.rewardKind() != QuestRewardKind.GP
						&& reward.rewardKind() != QuestRewardKind.EXP) {
					fail("QUEST_BASE_REWARD_KIND_UNSUPPORTED",
						"QUEST_BASE amount mode is unsupported for " + reward.rewardKind());
				}
			}
			QuestStatus effectiveStatus = nodes.get(transition.targetNode()).projection().status();
			for (QuestAction action : transition.actions()) {
				if (action instanceof QuestAction.SetStatus setStatus) {
					effectiveStatus = setStatus.status();
				}
			}
			long completions = transition.actions().stream()
				.filter(QuestAction.CompleteQuest.class::isInstance).count();
			boolean blockOnly = transition.actions().size() == 1
				&& transition.actions().get(0) instanceof QuestAction.BlockDefaultItemUse;
			if (blockOnly && !(transition.event() instanceof QuestEvent.UseItem)) {
				fail("BLOCK_DEFAULT_ITEM_USE_EVENT_MISMATCH",
					"block-default-item-use is only valid on use-item events");
			}
			boolean preservesCompletedProjection = transition.sourceNode() != null
				&& transition.sourceNode().equals(transition.targetNode())
				&& nodes.get(transition.sourceNode()).projection().status() == QuestStatus.COMPLETE
				&& effectiveStatus == QuestStatus.COMPLETE
				&& (transition.actions().isEmpty() || blockOnly);
			if (effectiveStatus == QuestStatus.COMPLETE && completions != 1 && !preservesCompletedProjection) {
				fail("COMPLETE_QUEST_ACTION_REQUIRED",
					"a COMPLETE projection requires exactly one complete-quest action");
			}
			if (effectiveStatus != QuestStatus.COMPLETE && completions != 0) {
				fail("COMPLETE_QUEST_STATUS_MISMATCH",
					"complete-quest action requires a COMPLETE projection");
			}
			boolean abandons = transition.actions().stream()
				.anyMatch(QuestAction.AbandonQuest.class::isInstance);
			if (abandons && effectiveStatus != QuestStatus.NONE) {
				fail("ABANDON_QUEST_STATUS_MISMATCH",
					"abandon-quest action requires a NONE projection");
			}
			List<AfterCommitAction.SyncQuestState> stateSyncs = transition.afterCommit().stream()
				.filter(AfterCommitAction.SyncQuestState.class::isInstance)
				.map(AfterCommitAction.SyncQuestState.class::cast).toList();
			if (stateSyncs.size() > 1) {
				fail("DUPLICATE_QUEST_STATE_SYNC", "a transition may synchronize quest state only once");
			}
			boolean completionSync = stateSyncs.size() == 1
				&& stateSyncs.get(0).mode() == QuestStateSyncMode.COMPLETION;
			if (effectiveStatus == QuestStatus.COMPLETE && !completionSync && !preservesCompletedProjection) {
				fail("COMPLETE_QUEST_SYNC_REQUIRED",
					"a COMPLETE projection requires one COMPLETION quest-state sync");
			}
			if (effectiveStatus != QuestStatus.COMPLETE && completionSync) {
				fail("COMPLETE_QUEST_SYNC_STATUS_MISMATCH",
					"COMPLETION quest-state sync requires a COMPLETE projection");
			}
			for (String source : sourceLabels(definition, transition)) {
				outgoing.computeIfAbsent(source, ignored -> new HashSet<>()).add(transition.targetNode());
			}
		}
		validateCraftLifecycle(definition, nodes);
		Set<String> reached = new HashSet<>();
		java.util.ArrayDeque<String> pending = new java.util.ArrayDeque<>();
		pending.add(definition.nodes().get(0).label());
		while (!pending.isEmpty()) {
			String current = pending.removeFirst();
			if (!reached.add(current)) {
				continue;
			}
			for (String target : outgoing.getOrDefault(current, Set.of())) {
				pending.addLast(target);
			}
		}
		if (reached.size() != nodes.size()) {
			String unreachable = nodes.keySet().stream().filter(label -> !reached.contains(label)).sorted().findFirst()
				.orElse("unknown");
			fail("UNREACHABLE_NODE", "node is not reachable from the first node: " + unreachable);
		}
		validateTransitionConflicts(definition.transitions(), nodes);
		return new CompiledQuestDefinition(definition);
	}

	/**
	 * Restores the legacy turn-in handshake for definitions that spell out reward
	 * confirmation routes but omit the initial {@code -1}/{@code 1009} preview.
	 * The old quest engine handled this uniformly for every REWARD-state end NPC;
	 * keeping the lowering here makes XML and Java DSL owners behave identically.
	 */
	private static QuestDefinition restoreRewardPreviewContract(QuestDefinition definition) {
		Map<String, QuestStatus> statuses = new HashMap<>();
		for (QuestNode node : definition.nodes()) {
			statuses.put(node.label(), node.projection().status());
		}
		Map<RewardPreviewKey, Set<Integer>> completionRewardIndexes = new LinkedHashMap<>();
		for (QuestTransition transition : definition.transitions()) {
			Set<String> sources = rewardSourceLabels(definition, statuses, transition);
			if (sources.isEmpty() || !(transition.event() instanceof QuestEvent.TalkToNpc talk)) {
				continue;
			}
			Integer dialogId = talk.dialogId();
			for (String source : sources) {
				RewardPreviewKey key = new RewardPreviewKey(source, talk.npcId());
				if (dialogId != null && dialogId >= 8 && dialogId <= 23
						&& transition.targetNode() != null
						&& statuses.get(transition.targetNode()) == QuestStatus.COMPLETE) {
					for (QuestAction action : transition.actions()) {
						if (action instanceof QuestAction.CompleteQuest complete) {
							completionRewardIndexes.computeIfAbsent(key, ignored -> new HashSet<>())
								.add(complete.rewardIndex());
						}
					}
				}
			}
		}

		List<QuestTransition> normalized = new ArrayList<>(definition.transitions());
		for (Map.Entry<RewardPreviewKey, Set<Integer>> entry : completionRewardIndexes.entrySet()) {
			RewardPreviewKey key = entry.getKey();
			Set<Integer> indexes = entry.getValue();
			if (indexes.size() != 1) {
				continue;
			}
			int rewardIndex = indexes.iterator().next();
			if (rewardIndex > Integer.MAX_VALUE - 5) {
				continue;
			}
			int rewardPage = 5 + rewardIndex;
			for (int dialogId : List.of(-1, 1009)) {
				if (hasRewardPreview(definition, statuses, key, dialogId)) {
					continue;
				}
				normalized.add(new QuestTransition(new QuestEvent.TalkToNpc(key.npcId(), dialogId),
					List.of(), List.of(), key.source(),
					List.of(new AfterCommitAction.ShowQuestDialog(rewardPage)), null, key.source()));
			}
		}
		if (normalized.size() == definition.transitions().size()) {
			return definition;
		}
		return new QuestDefinition(definition.id(), definition.version(), definition.metadata(),
			definition.progressLayout(), definition.nodes(), normalized);
	}

	private static Set<String> rewardSourceLabels(QuestDefinition definition, Map<String, QuestStatus> statuses,
		QuestTransition transition) {
		if (transition.sourceNode() != null) {
			return statuses.get(transition.sourceNode()) == QuestStatus.REWARD
				? Set.of(transition.sourceNode()) : Set.of();
		}
		boolean rewardBound = transition.conditions().stream()
			.anyMatch(condition -> condition instanceof QuestCondition.StatusIs status
				&& status.status() == QuestStatus.REWARD);
		if (!rewardBound) {
			return Set.of();
		}
		return definition.nodes().stream()
			.filter(node -> node.projection().status() == QuestStatus.REWARD)
			.map(QuestNode::label)
			.sorted()
			.collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
	}

	private static boolean hasRewardPreview(QuestDefinition definition, Map<String, QuestStatus> statuses,
		RewardPreviewKey key, int dialogId) {
		return definition.transitions().stream().anyMatch(transition ->
			rewardSourceLabels(definition, statuses, transition).contains(key.source())
				&& transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == key.npcId()
				&& Integer.valueOf(dialogId).equals(talk.dialogId()));
	}

	private record RewardPreviewKey(String source, int npcId) {
	}

	private static void validateProgressField(QuestDefinition definition, String field, String context) {
		if (definition.progressLayout().field(field) == null) {
			fail("UNKNOWN_PROGRESS_FIELD", context + " references unknown field: " + field);
		}
	}

	private static void validateProgressFields(QuestDefinition definition, List<String> fields, String context) {
		for (String field : fields) {
			validateProgressField(definition, field, context);
		}
	}

	private static void validateCraftLifecycle(QuestDefinition definition, Map<String, QuestNode> nodes) {
		Set<Integer> questOwnedRecipes = new HashSet<>();
		for (QuestTransition transition : definition.transitions()) {
			for (QuestAction action : transition.actions()) {
				if (action instanceof QuestAction.LearnRecipe learn
						&& learn.ownership() == QuestRecipeOwnership.QUEST_OWNED) {
					questOwnedRecipes.add(learn.recipeId());
				}
			}
		}
		for (int recipeId : questOwnedRecipes) {
			boolean abandonCleanup = definition.metadata().cannotGiveup();
			boolean terminalCleanup = false;
			for (QuestTransition transition : definition.transitions()) {
				boolean forgetsRecipe = transition.actions().stream()
					.anyMatch(action -> action instanceof QuestAction.ForgetRecipe forget
						&& forget.recipeId() == recipeId);
				if (!forgetsRecipe) {
					continue;
				}
				boolean abandonsQuest = transition.actions().stream()
					.anyMatch(QuestAction.AbandonQuest.class::isInstance);
				if (transition.event() instanceof QuestEvent.Abandon || abandonsQuest) {
					abandonCleanup = true;
				} else {
					QuestStatus status = nodes.get(transition.targetNode()).projection().status();
					terminalCleanup |= status == QuestStatus.COMPLETE || status == QuestStatus.NONE;
				}
			}
			if (!abandonCleanup || !terminalCleanup) {
				fail("CRAFT_LIFECYCLE_INCOMPLETE", "quest-owned recipe " + recipeId
					+ " requires explicit abandon and terminal cleanup");
			}
		}
	}

	/**
	 * Returns the graph sources covered by a transition.  A transition without
	 * an explicit source is a deliberate wildcard at runtime (the planner
	 * matches its conditions against the current snapshot).  It is safe to
	 * accept that form when a single status condition bounds the wildcard; the
	 * bounded node set is enough for reachability and conflict analysis.
	 */
	private static Set<String> sourceLabels(QuestDefinition definition, QuestTransition transition) {
		if (transition.sourceNode() != null) {
			return Set.of(transition.sourceNode());
		}
		Set<QuestStatus> statuses = new HashSet<>();
		for (QuestCondition condition : transition.conditions()) {
			if (condition instanceof QuestCondition.StatusIs status) {
				statuses.add(status.status());
			}
		}
		if (statuses.size() == 1) {
			QuestStatus status = statuses.iterator().next();
			Set<String> matches = definition.nodes().stream()
				.filter(node -> node.projection().status() == status)
				.map(QuestNode::label)
				.collect(java.util.stream.Collectors.toUnmodifiableSet());
			if (!matches.isEmpty()) {
				return matches;
			}
		}
		if (definition.nodes().size() == 1) {
			return Set.of(definition.nodes().get(0).label());
		}
		fail("AMBIGUOUS_SOURCE", "transition source cannot be inferred; declare source explicitly or add one status-is condition");
		return Set.of();
	}

	private static void validateTransitionConflicts(List<QuestTransition> transitions,
			Map<String, QuestNode> nodes) {
		for (int left = 0; left < transitions.size(); left++) {
			QuestTransition a = transitions.get(left);
			for (int right = left + 1; right < transitions.size(); right++) {
				QuestTransition b = transitions.get(right);
				if (!QuestEvent.overlaps(a.event(), b.event())) {
					continue;
				}
				if (QuestEvent.hasDeterministicPrecedence(a.event(), b.event())) {
					continue;
				}
				if (sourceNodesAreMutuallyExclusive(a, b, nodes)) {
					continue;
				}
				if (mutuallyExclusive(a.conditions(), b.conditions())) {
					continue;
				}
				if (!a.hasExplicitPriority() || !b.hasExplicitPriority() || a.priority().equals(b.priority())) {
					fail("AMBIGUOUS_TRANSITION", "same event has overlapping transitions without unique priorities: "
						+ a.event().type());
				}
			}
		}
	}

	private static boolean sourceNodesAreMutuallyExclusive(QuestTransition left, QuestTransition right,
			Map<String, QuestNode> nodes) {
		List<QuestNode> leftSources = sourceCandidates(left, nodes);
		List<QuestNode> rightSources = sourceCandidates(right, nodes);
		for (QuestNode leftSource : leftSources) {
			for (QuestNode rightSource : rightSources) {
				if (!sameProjection(leftSource.projection(), rightSource.projection())) {
					continue;
				}
				if (conditionsCannotMatchNode(left.conditions(), leftSource)
						|| conditionsCannotMatchNode(right.conditions(), rightSource)) {
					continue;
				}
				return false;
			}
		}
		return true;
	}

	private static List<QuestNode> sourceCandidates(QuestTransition transition, Map<String, QuestNode> nodes) {
		if (transition.sourceNode() != null) {
			return List.of(nodes.get(transition.sourceNode()));
		}
		Set<QuestStatus> statuses = new HashSet<>();
		for (QuestCondition condition : transition.conditions()) {
			if (condition instanceof QuestCondition.StatusIs status) {
				statuses.add(status.status());
			}
		}
		if (statuses.size() == 1) {
			QuestStatus status = statuses.iterator().next();
			return nodes.values().stream().filter(node -> node.projection().status() == status).toList();
		}
		return List.copyOf(nodes.values());
	}

	private static boolean sameProjection(NodeProjection left, NodeProjection right) {
		if (left.status() != right.status()) {
			return false;
		}
		Set<String> fields = new HashSet<>(left.variables().keySet());
		fields.addAll(right.variables().keySet());
		return fields.stream().allMatch(field -> left.variables().getOrDefault(field, 0)
			.equals(right.variables().getOrDefault(field, 0)));
	}

	private static boolean conditionsCannotMatchNode(List<QuestCondition> conditions, QuestNode node) {
		for (QuestCondition condition : conditions) {
			Boolean matches = conditionMatchesNode(condition, node);
			if (Boolean.FALSE.equals(matches)) {
				return true;
			}
		}
		return false;
	}

	/** Returns null when a condition depends on live facts rather than the quest projection. */
	private static Boolean conditionMatchesNode(QuestCondition condition, QuestNode node) {
		NodeProjection projection = node.projection();
		if (condition instanceof QuestCondition.StatusIs status) {
			return projection.status() == status.status();
		}
		if (condition instanceof QuestCondition.QuestVariableIs variable) {
			return projection.variables().getOrDefault(variable.field(), 0) == variable.value();
		}
		if (condition instanceof QuestCondition.VariableAtLeast variable) {
			return projection.variables().getOrDefault(variable.field(), 0) >= variable.value();
		}
		if (condition instanceof QuestCondition.VariableBelow variable) {
			return projection.variables().getOrDefault(variable.field(), 0) < variable.value();
		}
		if (condition instanceof QuestCondition.VariableSumIs variable) {
			int sum = variable.fields().stream().mapToInt(field -> projection.variables().getOrDefault(field, 0)).sum();
			return sum == variable.value();
		}
		if (condition instanceof QuestCondition.VariableSumBelow variable) {
			int sum = variable.fields().stream().mapToInt(field -> projection.variables().getOrDefault(field, 0)).sum();
			return sum < variable.value();
		}
		return null;
	}

	private static boolean mutuallyExclusive(List<QuestCondition> left, List<QuestCondition> right) {
		for (QuestCondition a : left) {
			for (QuestCondition b : right) {
				if (factConditionsAreMutuallyExclusive(a, b)) {
					return true;
				}
			}
		}
		for (QuestCondition a : left) {
			if (!(a instanceof QuestCondition.StatusIs leftStatus)) {
				continue;
			}
			for (QuestCondition b : right) {
				if (b instanceof QuestCondition.StatusIs rightStatus
						&& leftStatus.status() != rightStatus.status()) {
					return true;
				}
			}
		}
		for (QuestCondition a : left) {
			if (!(a instanceof QuestCondition.QuestVariableIs leftVariable)) {
				continue;
			}
			for (QuestCondition b : right) {
				if (b instanceof QuestCondition.QuestVariableIs rightVariable
						&& leftVariable.field().equals(rightVariable.field())
						&& leftVariable.value() != rightVariable.value()) {
					return true;
				}
			}
		}
		for (QuestCondition a : left) {
			for (QuestCondition b : right) {
				if (variableConditionsAreMutuallyExclusive(a, b)) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean factConditionsAreMutuallyExclusive(QuestCondition left, QuestCondition right) {
		if (left instanceof QuestCondition.PlayerInGroup a && right instanceof QuestCondition.PlayerInGroup b) {
			return a.expected() != b.expected();
		}
		if (left instanceof QuestCondition.HasItem a && right instanceof QuestCondition.HasItem b) {
			return a.itemId() == b.itemId() && a.count() == b.count() && a.expected() != b.expected();
		}
		if (left instanceof QuestCondition.GenderIs a && right instanceof QuestCondition.GenderIs b) {
			return a.gender() != b.gender();
		}
		if (left instanceof QuestCondition.PlayerRaceIs a && right instanceof QuestCondition.PlayerRaceIs b) {
			return a.race() != b.race();
		}
		if (left instanceof QuestCondition.PlayerClassIs a && right instanceof QuestCondition.PlayerClassIs b) {
			return a.startingClass() != b.startingClass();
		}
		if (left instanceof QuestCondition.AdvancedClassIs a && right instanceof QuestCondition.AdvancedClassIs b) {
			return a.playerClass() != b.playerClass();
		}
		if (left instanceof QuestCondition.WorldIs a && right instanceof QuestCondition.WorldIs b) {
			return a.worldId() == b.worldId() && a.expected() != b.expected();
		}
		if (left instanceof QuestCondition.WorldNpcIs a && right instanceof QuestCondition.WorldNpcIs b) {
			return a.npcId() == b.npcId() && a.expected() != b.expected();
		}
		if (left instanceof QuestCondition.ZoneIs a && right instanceof QuestCondition.ZoneIs b) {
			return a.zone().equals(b.zone()) && a.expected() != b.expected();
		}
		if (left instanceof QuestCondition.EquipmentSetEquipped a
				&& right instanceof QuestCondition.EquipmentSetEquipped b) {
			return a.count() == b.count() && a.setIds().equals(b.setIds()) && a.expected() != b.expected();
		}
		if (left instanceof QuestCondition.EquippedItem a && right instanceof QuestCondition.EquippedItem b) {
			return a.itemId() == b.itemId() && a.count() == b.count() && a.expected() != b.expected();
		}
		if (left instanceof QuestCondition.MembershipPermission a
				&& right instanceof QuestCondition.MembershipPermission b) {
			return a.permission() == b.permission() && a.expected() != b.expected();
		}
		if (left instanceof QuestCondition.CompleteCountIs a
				&& right instanceof QuestCondition.CompleteCountIs b) {
			return a.value() == b.value() && a.expected() != b.expected();
		}
		if (left instanceof QuestCondition.EventActive a && right instanceof QuestCondition.EventActive b) {
			return a.questId() == b.questId() && a.expected() != b.expected();
		}
		return false;
	}

	private static boolean variableConditionsAreMutuallyExclusive(QuestCondition left, QuestCondition right) {
		if (left instanceof QuestCondition.QuestVariableIs a
				&& right instanceof QuestCondition.QuestVariableIs b) {
			return a.field().equals(b.field()) && a.value() != b.value();
		}
		if (left instanceof QuestCondition.QuestVariableIs a
				&& right instanceof QuestCondition.VariableAtLeast b) {
			return a.field().equals(b.field()) && a.value() < b.value();
		}
		if (left instanceof QuestCondition.VariableAtLeast a
				&& right instanceof QuestCondition.QuestVariableIs b) {
			return a.field().equals(b.field()) && b.value() < a.value();
		}
		if (left instanceof QuestCondition.QuestVariableIs a
				&& right instanceof QuestCondition.VariableBelow b) {
			return a.field().equals(b.field()) && a.value() >= b.value();
		}
		if (left instanceof QuestCondition.VariableBelow a
				&& right instanceof QuestCondition.QuestVariableIs b) {
			return a.field().equals(b.field()) && b.value() >= a.value();
		}
		if (left instanceof QuestCondition.VariableAtLeast a
				&& right instanceof QuestCondition.VariableBelow b) {
			return a.field().equals(b.field()) && a.value() >= b.value();
		}
		if (left instanceof QuestCondition.VariableBelow a
				&& right instanceof QuestCondition.VariableAtLeast b) {
			return a.field().equals(b.field()) && b.value() >= a.value();
		}
		return false;
	}

	private static void fail(String code, String message) {
		throw new QuestCompilationException(code, message);
	}
}
