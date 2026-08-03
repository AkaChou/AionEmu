package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.NodeProjection;
import com.aionemu.gameserver.questEngine.definition.ProgressLayout;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestNode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Builds a plan without mutating player state or performing external effects. */
public final class QuestMutationPlanner {
	private QuestMutationPlanner() {
	}

	public static Optional<QuestMutationPlan> plan(CompiledQuestDefinition definition,
			QuestSnapshot snapshot, QuestTransition transition) {
		if (definition.id() != snapshot.questId()) {
			return Optional.empty();
		}
		return build(definition, snapshot, transition);
	}

	public static Optional<QuestMutationPlan> plan(CompiledQuestDefinition definition,
			QuestSnapshot snapshot, QuestEvent event, QuestTransition transition) {
		if (!QuestEvent.matches(transition.event(), event)) {
			return Optional.empty();
		}
		return plan(definition, snapshot, transition);
	}

	private static Optional<QuestMutationPlan> build(CompiledQuestDefinition definition,
			QuestSnapshot snapshot, QuestTransition transition) {
		ProgressLayout layout = definition.definition().progressLayout();
		if (!matchesSourceNode(definition, layout, snapshot, transition)) {
			return Optional.empty();
		}
		if (!QuestConditionEvaluator.matches(layout, snapshot, transition.conditions())) {
			return Optional.empty();
		}
		QuestNode target = definition.definition().nodes().stream()
				.filter(node -> node.label().equals(transition.targetNode())).findFirst().orElseThrow();
		NodeProjection projection = target.projection();
		Map<String, Integer> variables = new LinkedHashMap<>(layout.unpack(snapshot.packedVariables()));
		variables.putAll(projection.variables());
		for (QuestAction action : transition.actions()) {
				switch (action) {
					case QuestAction.RemoveItem remove -> {
						if (!removalFeasible(snapshot, remove)) {
							return Optional.empty();
						}
					}
				case QuestAction.SetVariable set -> variables.put(set.field(), set.value());
				case QuestAction.SetStatus ignored -> {
				}
				case QuestAction.GrantReward ignored -> {
				}
				case QuestAction.LearnRecipe ignored -> {
				}
				case QuestAction.ForgetRecipe ignored -> {
				}
					case QuestAction.GrantCraftSkill ignored -> {
					}
					case QuestAction.CompleteQuest ignored -> {
					}
			}
		}
		int packed = layout.pack(variables);
		var status = projection.status();
		for (QuestAction action : transition.actions()) {
			if (action instanceof QuestAction.SetStatus setStatus) {
				status = setStatus.status();
			}
		}
		return Optional.of(new QuestMutationPlan(definition.id(), status, packed, transition.actions(), transition.afterCommit()));
	}

	private static boolean matchesSourceNode(CompiledQuestDefinition definition, ProgressLayout layout,
			QuestSnapshot snapshot, QuestTransition transition) {
		if (transition.sourceNode() == null) {
			return true;
		}
		QuestNode source = definition.definition().nodes().stream()
			.filter(node -> node.label().equals(transition.sourceNode())).findFirst().orElseThrow();
		if (snapshot.status() != source.projection().status()) {
			return false;
		}
		Map<String, Integer> actual = layout.unpack(snapshot.packedVariables());
		return source.projection().variables().entrySet().stream()
			.allMatch(entry -> entry.getValue().equals(actual.get(entry.getKey())));
	}

	/**
	 * A removal is feasible only when the snapshot actually captured inventory
	 * facts. Unknown facts (player being logged out) never guess a zero balance:
	 * the removal is treated as infeasible instead of inventing a matching plan.
	 */
	private static boolean removalFeasible(QuestSnapshot snapshot, QuestAction.RemoveItem remove) {
		try {
			return snapshot.itemCount(remove.itemId()) >= remove.count();
		} catch (IllegalStateException unknownFacts) {
			return false;
		}
	}
}
