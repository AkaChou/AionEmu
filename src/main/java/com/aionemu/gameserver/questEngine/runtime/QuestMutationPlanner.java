package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.NodeProjection;
import com.aionemu.gameserver.questEngine.definition.ProgressLayout;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestNode;
import com.aionemu.gameserver.questEngine.definition.QuestReward;
import com.aionemu.gameserver.questEngine.definition.QuestRewardAmountMode;
import com.aionemu.gameserver.questEngine.definition.QuestRewardKind;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
		return build(definition, snapshot, null, transition);
	}

	public static Optional<QuestMutationPlan> plan(CompiledQuestDefinition definition,
			QuestSnapshot snapshot, QuestEvent event, QuestTransition transition) {
		if (!QuestEvent.matches(transition.event(), event)) {
			return Optional.empty();
		}
		if (definition.id() != snapshot.questId()) {
			return Optional.empty();
		}
		return build(definition, snapshot, event, transition);
	}

	private static Optional<QuestMutationPlan> build(CompiledQuestDefinition definition,
			QuestSnapshot snapshot, QuestEvent event, QuestTransition transition) {
		ProgressLayout layout = definition.definition().progressLayout();
		if (!matchesSourceNode(definition, layout, snapshot, transition)) {
			return Optional.empty();
		}
		if (!metadataPrerequisitesSatisfied(definition, snapshot, transition)) {
			return Optional.empty();
		}
		if (!QuestConditionEvaluator.matches(layout, snapshot, event, transition.conditions())) {
			return Optional.empty();
		}
		QuestNode target = definition.definition().nodes().stream()
				.filter(node -> node.label().equals(transition.targetNode())).findFirst().orElseThrow();
		NodeProjection projection = target.projection();
		List<QuestAction> actions = new ArrayList<>(transition.actions());
		appendFinalRepeatRewards(definition, snapshot, actions);
		Map<String, Integer> variables = new LinkedHashMap<>(layout.unpack(snapshot.packedVariables()));
		for (QuestAction action : actions) {
			switch (action) {
				case QuestAction.RemoveItem remove -> {
					if (!removalFeasible(snapshot, remove)) {
						return Optional.empty();
					}
				}
				case QuestAction.GiveItem ignored -> {
				}
				case QuestAction.SetVariable set -> variables.put(set.field(), set.value());
				case QuestAction.IncrementVariable inc ->
					variables.merge(inc.field(), inc.delta(), Integer::sum);
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
				case QuestAction.BlockDefaultItemUse ignored -> {
				}
			}
		}
		variables.putAll(projection.variables());
		int packed = layout.pack(variables);
		var status = projection.status();
		for (QuestAction action : actions) {
			if (action instanceof QuestAction.SetStatus setStatus) {
				status = setStatus.status();
			}
		}
		return Optional.of(new QuestMutationPlan(definition.id(), status, packed, actions, transition.afterCommit()));
	}

	private static void appendFinalRepeatRewards(CompiledQuestDefinition definition, QuestSnapshot snapshot,
			List<QuestAction> actions) {
		if (definition.definition().metadata().extendedRewards().isEmpty()
				|| actions.stream().noneMatch(QuestAction.CompleteQuest.class::isInstance)) {
			return;
		}
		var repeat = definition.definition().metadata().repeatPolicy();
		if (repeat.maxRepeatCount() >= 255
				|| snapshot.completeCount() != repeat.maxRepeatCount() - 1) {
			return;
		}
		for (QuestReward reward : definition.definition().metadata().extendedRewards()) {
			QuestRewardKind kind = QuestRewardKind.fromWire(reward.kind());
			QuestRewardAmountMode mode = switch (kind) {
				case GOLD, KINAH, EXP, AP, GP -> QuestRewardAmountMode.QUEST_BASE;
				default -> QuestRewardAmountMode.EXACT;
			};
			actions.add(new QuestAction.GrantReward(reward.kind(), reward.id(), reward.amount(), mode));
		}
	}

	private static boolean matchesSourceNode(CompiledQuestDefinition definition, ProgressLayout layout,
			QuestSnapshot snapshot, QuestTransition transition) {
		if (transition.sourceNode() == null) {
			return true;
		}
		QuestNode source = definition.definition().nodes().stream()
			.filter(node -> node.label().equals(transition.sourceNode())).findFirst().orElseThrow();
		if (!matchesSourceStatus(snapshot, source, transition)) {
			return false;
		}
		Map<String, Integer> actual = layout.unpack(snapshot.packedVariables());
		return source.projection().variables().entrySet().stream()
			.allMatch(entry -> entry.getValue().equals(actual.get(entry.getKey())));
	}

	/** Metadata prerequisites gate only transitions that acquire an unaccepted quest. */
	private static boolean metadataPrerequisitesSatisfied(CompiledQuestDefinition definition,
		QuestSnapshot snapshot, QuestTransition transition) {
		if (definition.definition().metadata().prerequisites().isEmpty() || transition.sourceNode() == null) {
			return true;
		}
		QuestNode source = definition.definition().nodes().stream()
			.filter(node -> node.label().equals(transition.sourceNode())).findFirst().orElseThrow();
		QuestNode target = definition.definition().nodes().stream()
			.filter(node -> node.label().equals(transition.targetNode())).findFirst().orElseThrow();
		if (source.projection().status() != QuestStatus.NONE
			|| target.projection().status() == QuestStatus.NONE) {
			return true;
		}
		return QuestConditionEvaluator.matches(definition.definition().progressLayout(), snapshot,
			List.of(new QuestCondition.QuestsFinished(definition.definition().metadata().prerequisites())));
	}

	/**
	 * A repeatable quest is persisted as COMPLETE between runs, while its next
	 * start transition is declared from the NONE/unaccepted node. Only an
	 * explicitly start-eligible transition may cross that lifecycle boundary;
	 * ordinary unaccepted dialog routes must not fire after completion.
	 */
	private static boolean matchesSourceStatus(QuestSnapshot snapshot, QuestNode source,
		QuestTransition transition) {
		if (snapshot.status() == source.projection().status()) {
			return true;
		}
		return snapshot.status() == QuestStatus.COMPLETE
			&& source.projection().status() == QuestStatus.NONE
			&& transition.conditions().stream().anyMatch(QuestCondition.StartEligible.class::isInstance);
	}

	/**
	 * A removal is feasible only when the snapshot actually captured inventory
	 * facts. Unknown facts (player being logged out) never guess a zero balance:
	 * the removal is treated as infeasible instead of inventing a matching plan.
	 */
	private static boolean removalFeasible(QuestSnapshot snapshot, QuestAction.RemoveItem remove) {
		try {
			int available = snapshot.itemCount(remove.itemId());
			return remove.removeAll() || available >= remove.count();
		} catch (IllegalStateException unknownFacts) {
			return false;
		}
	}
}
