package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestAction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Compares normalized candidate output with a typed legacy observation without executing either side. */
public final class QuestShadowComparator {
	private QuestShadowComparator() {
	}

	public static List<QuestShadowDifference> compare(QuestShadowRunner.QuestShadowResult candidate,
			QuestShadowObservation actual) {
		Objects.requireNonNull(candidate, "candidate");
		Objects.requireNonNull(actual, "actual");
		Map<Integer, List<QuestShadowRunner.QuestShadowOwner>> expected = candidate.owners().stream()
				.collect(Collectors.groupingBy(QuestShadowRunner.QuestShadowOwner::questId));
		List<QuestShadowDifference> differences = new ArrayList<>();
		Set<Integer> ownerIds = new HashSet<>(expected.keySet());
		ownerIds.addAll(actual.owners().keySet());
		for (int questId : ownerIds.stream().sorted().toList()) {
			List<QuestShadowRunner.QuestShadowOwner> shadowOwners = expected.get(questId);
			QuestShadowObservation.Owner actualOwner = actual.owners().get(questId);
			if (shadowOwners == null || actualOwner == null) {
				differences.add(new QuestShadowDifference(QuestShadowDifferenceKind.ROUTE, questId));
				continue;
			}
			QuestShadowRunner.QuestShadowOwner shadowOwner = shadowOwners.stream()
					.filter(owner -> owner.plan().isPresent()).findFirst().orElse(shadowOwners.get(0));
			if (shadowOwner.plan().isPresent() != actualOwner.conditionMatched()) {
				differences.add(new QuestShadowDifference(QuestShadowDifferenceKind.CONDITION, questId));
			}
			if (shadowOwner.plan().isPresent() && actualOwner.conditionMatched()) {
				QuestMutationPlan plan = shadowOwner.plan().orElseThrow();
				if (plan.nextStatus() != actualOwner.nextStatus()
						|| plan.nextPackedVariables() != actualOwner.nextPackedVariables()) {
					differences.add(new QuestShadowDifference(QuestShadowDifferenceKind.VARIABLES, questId));
				}
				List<QuestAction> expectedActions = externalActions(plan.requiredActions());
				List<QuestAction> actualActions = externalActions(actualOwner.requiredActions());
				List<QuestAction> expectedRewards = expectedActions.stream()
						.filter(action -> action instanceof QuestAction.GrantReward).toList();
				List<QuestAction> actualRewards = actualActions.stream()
						.filter(action -> action instanceof QuestAction.GrantReward).toList();
				if (!expectedRewards.equals(actualRewards)) {
					differences.add(new QuestShadowDifference(QuestShadowDifferenceKind.REWARD, questId));
				}
				List<QuestAction> expectedMutations = expectedActions.stream()
					.filter(QuestAction.RemoveItem.class::isInstance).toList();
				List<QuestAction> actualMutations = actualActions.stream()
					.filter(QuestAction.RemoveItem.class::isInstance).toList();
				if (!expectedMutations.equals(actualMutations)
						|| (expectedRewards.equals(actualRewards) && !expectedActions.equals(actualActions))) {
					differences.add(new QuestShadowDifference(QuestShadowDifferenceKind.ACTION, questId));
				}
				if (!plan.afterCommit().equals(actualOwner.afterCommit())) {
					differences.add(new QuestShadowDifference(QuestShadowDifferenceKind.PROTOCOL, questId));
				}
			}
			boolean candidateConsumed = shadowOwner.plan().isPresent();
			boolean actualConsumed = actualOwner.result() == QuestRouteResult.HANDLED;
			if (candidateConsumed != actualConsumed) {
				differences.add(new QuestShadowDifference(QuestShadowDifferenceKind.RESULT_CONSUMPTION, questId));
			}
		}
		boolean candidateConsumed = candidate.hasCandidatePlan();
		if (candidateConsumed != actual.consumed()) {
			differences.add(new QuestShadowDifference(QuestShadowDifferenceKind.RESULT_CONSUMPTION, 0));
		}
		return differences.stream().distinct()
				.sorted(Comparator.comparing(QuestShadowDifference::questId)
						.thenComparing(QuestShadowDifference::kind))
				.toList();
	}

	private static List<QuestAction> externalActions(List<QuestAction> actions) {
		return actions.stream()
			.filter(action -> !(action instanceof QuestAction.SetVariable)
				&& !(action instanceof QuestAction.SetStatus))
			.toList();
	}

	/** Compares a candidate result with one actual legacy invocation. */
	public static List<QuestShadowDifference> compare(QuestShadowRunner.QuestShadowResult candidate,
			QuestLegacyInvocation actual) {
		Objects.requireNonNull(candidate, "candidate");
		Objects.requireNonNull(actual, "actual");
		List<QuestShadowDifference> differences = new ArrayList<>();
		if (!candidate.event().type().equals(actual.eventType())) {
			differences.add(new QuestShadowDifference(QuestShadowDifferenceKind.ROUTE, actual.questId()));
		}
		differences.addAll(compare(candidate, actual.observation()));
		return differences.stream().distinct()
			.sorted(Comparator.comparing(QuestShadowDifference::questId)
				.thenComparing(QuestShadowDifference::kind))
			.toList();
	}
}
