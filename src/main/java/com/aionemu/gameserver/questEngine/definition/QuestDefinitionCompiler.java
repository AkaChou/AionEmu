package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Structural compiler shared by XML and Java DSL front ends. */
public final class QuestDefinitionCompiler {
	private QuestDefinitionCompiler() {
	}

	public static CompiledQuestDefinition compile(QuestDefinition definition) {
		Objects.requireNonNull(definition, "definition");
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
		if (definition.metadata().npcFactionId() != 0) {
			fail("NPC_FACTION_START_UNSUPPORTED",
				"NPC faction quest start has a transaction-external side effect");
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
			}
			for (QuestAction action : transition.actions()) {
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
			if (effectiveStatus == QuestStatus.COMPLETE && completions != 1) {
				fail("COMPLETE_QUEST_ACTION_REQUIRED",
					"a COMPLETE projection requires exactly one complete-quest action");
			}
			if (effectiveStatus != QuestStatus.COMPLETE && completions != 0) {
				fail("COMPLETE_QUEST_STATUS_MISMATCH",
					"complete-quest action requires a COMPLETE projection");
			}
			List<AfterCommitAction.SyncQuestState> stateSyncs = transition.afterCommit().stream()
				.filter(AfterCommitAction.SyncQuestState.class::isInstance)
				.map(AfterCommitAction.SyncQuestState.class::cast).toList();
			if (stateSyncs.size() > 1) {
				fail("DUPLICATE_QUEST_STATE_SYNC", "a transition may synchronize quest state only once");
			}
			boolean completionSync = stateSyncs.size() == 1
				&& stateSyncs.get(0).mode() == QuestStateSyncMode.COMPLETION;
			if (effectiveStatus == QuestStatus.COMPLETE && !completionSync) {
				fail("COMPLETE_QUEST_SYNC_REQUIRED",
					"a COMPLETE projection requires one COMPLETION quest-state sync");
			}
			if (effectiveStatus != QuestStatus.COMPLETE && completionSync) {
				fail("COMPLETE_QUEST_SYNC_STATUS_MISMATCH",
					"COMPLETION quest-state sync requires a COMPLETE projection");
			}
			String source = sourceLabel(definition, transition);
			outgoing.computeIfAbsent(source, ignored -> new HashSet<>()).add(transition.targetNode());
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

	private static void validateProgressField(QuestDefinition definition, String field, String context) {
		if (definition.progressLayout().field(field) == null) {
			fail("UNKNOWN_PROGRESS_FIELD", context + " references unknown field: " + field);
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
				if (transition.event() instanceof QuestEvent.Abandon) {
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

	private static String sourceLabel(QuestDefinition definition, QuestTransition transition) {
		if (transition.sourceNode() != null) {
			return transition.sourceNode();
		}
		Set<QuestStatus> statuses = new HashSet<>();
		for (QuestCondition condition : transition.conditions()) {
			if (condition instanceof QuestCondition.StatusIs status) {
				statuses.add(status.status());
			}
		}
		if (statuses.size() == 1) {
			QuestStatus status = statuses.iterator().next();
			List<QuestNode> matches = definition.nodes().stream()
					.filter(node -> node.projection().status() == status).toList();
			if (matches.size() == 1) {
				return matches.get(0).label();
			}
		}
		if (definition.nodes().size() == 1) {
			return definition.nodes().get(0).label();
		}
		fail("AMBIGUOUS_SOURCE", "transition source cannot be inferred; declare source explicitly");
		return null;
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
		if (left.sourceNode() == null || right.sourceNode() == null
				|| left.sourceNode().equals(right.sourceNode())) {
			return false;
		}
		NodeProjection leftProjection = nodes.get(left.sourceNode()).projection();
		NodeProjection rightProjection = nodes.get(right.sourceNode()).projection();
		if (leftProjection.status() != rightProjection.status()) {
			return true;
		}
		for (Map.Entry<String, Integer> variable : leftProjection.variables().entrySet()) {
			Integer rightValue = rightProjection.variables().get(variable.getKey());
			if (rightValue != null && !variable.getValue().equals(rightValue)) {
				return true;
			}
		}
		return false;
	}

	private static boolean mutuallyExclusive(List<QuestCondition> left, List<QuestCondition> right) {
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
