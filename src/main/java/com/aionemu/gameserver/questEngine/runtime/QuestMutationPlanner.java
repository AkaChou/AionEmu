package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
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
import java.util.Set;

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
		return planMatched(definition, snapshot, event, transition);
	}

	static Optional<QuestMutationPlan> planSharedQuestAccept(CompiledQuestDefinition definition,
			QuestSnapshot snapshot, QuestEvent.QuestDialog event, QuestTransition transition) {
		if (!(transition.event() instanceof QuestEvent.TalkToNpc talk)
				|| talk.dialogId() == null || talk.dialogId() != event.dialogId()) {
			return Optional.empty();
		}
		return planMatched(definition, snapshot, event, transition);
	}

	private static Optional<QuestMutationPlan> planMatched(CompiledQuestDefinition definition,
			QuestSnapshot snapshot, QuestEvent event, QuestTransition transition) {
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
		List<QuestAction> actions = expandSelectedRewards(definition, transition.actions());
		appendFinalRepeatRewards(definition, snapshot, actions);
		appendAbandonWorkItemCleanup(definition, event, transition.event(), actions);
		Map<String, Integer> variables = new LinkedHashMap<>(layout.unpack(snapshot.packedVariables()));
		Map<QuestRewardKind, Long> plannedDebits = new LinkedHashMap<>();
		Map<Integer, Integer> unequippedItems = new LinkedHashMap<>();
		Map<Integer, Integer> returnedItemRemovals = new LinkedHashMap<>();
		Map<Integer, Integer> plannedRemovals = new LinkedHashMap<>();
		for (QuestAction action : actions) {
			if (action instanceof QuestAction.UnequipItem unequip && snapshot.equipmentFacts() != null) {
				int count = snapshot.equipmentFacts().equippedItemCount(unequip.itemId());
				unequippedItems.put(unequip.itemId(), count);
				try {
					returnedItemRemovals.merge(unequip.itemId(), unequip.removeReturnedCount(), Math::addExact);
				} catch (ArithmeticException overflow) {
					return Optional.empty();
				}
			}
		}
		for (QuestAction action : actions) {
			switch (action) {
				case QuestAction.RemoveItem remove -> {
					if (!removalFeasible(snapshot, remove, unequippedItems, returnedItemRemovals,
						plannedRemovals)) {
						return Optional.empty();
					}
					if (remove.removeAll()) {
						plannedRemovals.put(remove.itemId(), Integer.MAX_VALUE);
					} else {
						try {
							plannedRemovals.merge(remove.itemId(), remove.count(), Math::addExact);
						} catch (ArithmeticException overflow) {
							return Optional.empty();
						}
					}
				}
				case QuestAction.UnequipItem unequip -> {
					if (snapshot.equipmentFacts() == null) {
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
				case QuestAction.GrantSelectedReward ignored -> {
					// Lowered to GrantReward before this validation loop.
				}
				case QuestAction.DecreaseCurrency debit -> {
					QuestRewardKind balanceKind = canonicalCurrencyKind(debit.kind());
					long total;
					try {
						total = plannedDebits.merge(balanceKind, debit.amount(), Math::addExact);
					} catch (ArithmeticException overflow) {
						return Optional.empty();
					}
					if (!debitFeasible(snapshot, balanceKind, total)) {
						return Optional.empty();
					}
				}
				case QuestAction.SetCurrency set -> {
					if (!setCurrencyFeasible(snapshot, set)) {
						return Optional.empty();
					}
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
				case QuestAction.AbandonQuest ignored -> {
					// The NONE projection is persisted by QuestStatePort; terminal cleanup
					// is registered by QuestExecutionCoordinator after commit.
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
		return Optional.of(new QuestMutationPlan(definition.id(), status, packed, actions,
			npcFactionLifecycleActions(definition, snapshot, event, transition, target)));
	}

	/**
	 * NPC-faction state is a player-side lifecycle resource rather than a quest
	 * variable. Keep it outside the SQL mutation, but schedule the same start,
	 * complete, and explicit-abandon hooks that the legacy QuestService invokes.
	 * The daily/weekly gate was dropped: retail faction rotation is daily and the
	 * daily flag is unreliable, so StartNpcFactionQuest fires on every NONE-to-START
	 * acquire; min-level 999 already guards the rotated window.
	 */
	private static List<AfterCommitAction> npcFactionLifecycleActions(CompiledQuestDefinition definition,
		QuestSnapshot snapshot, QuestEvent event, QuestTransition transition, QuestNode target) {
		int npcFactionId = definition.definition().metadata().npcFactionId();
		if (npcFactionId == 0) {
			return transition.afterCommit();
		}
		QuestStatus sourceStatus = transition.sourceNode() == null ? snapshot.status() : QuestStatus.NONE;
		if (transition.sourceNode() != null) {
			sourceStatus = definition.definition().nodes().stream()
				.filter(node -> node.label().equals(transition.sourceNode()))
				.map(node -> node.projection().status()).findFirst().orElse(QuestStatus.NONE);
		}
		List<AfterCommitAction> lifecycle = new ArrayList<>();
		if (sourceStatus == QuestStatus.NONE && target.projection().status() == QuestStatus.START) {
			lifecycle.add(new AfterCommitAction.StartNpcFactionQuest(npcFactionId));
		}
		if (target.projection().status() == QuestStatus.COMPLETE) {
			lifecycle.add(new AfterCommitAction.CompleteNpcFactionQuest(npcFactionId));
		}
		if (event instanceof QuestEvent.Abandon && sourceStatus != QuestStatus.NONE
			&& target.projection().status() == QuestStatus.NONE) {
			lifecycle.add(new AfterCommitAction.AbortNpcFactionQuest(npcFactionId));
		}
		lifecycle.addAll(transition.afterCommit());
		return lifecycle;
	}

	private static List<QuestAction> expandSelectedRewards(CompiledQuestDefinition definition,
			List<QuestAction> declaredActions) {
		List<QuestReward> metadataRewards = definition.definition().metadata().rewards();
		List<QuestAction> expanded = new ArrayList<>(declaredActions.size());
		for (QuestAction action : declaredActions) {
			if (!(action instanceof QuestAction.GrantSelectedReward selected)) {
				expanded.add(action);
				continue;
			}
			if (selected.rewardIndex() >= metadataRewards.size()) {
				throw new IllegalStateException("selected reward index " + selected.rewardIndex()
					+ " is not present in quest metadata " + definition.id());
			}
			QuestReward reward = metadataRewards.get(selected.rewardIndex());
			QuestRewardKind kind = QuestRewardKind.fromWire(reward.kind());
			QuestRewardAmountMode mode = switch (kind) {
				case GOLD, KINAH, AP, GP, EXP -> QuestRewardAmountMode.QUEST_BASE;
				default -> QuestRewardAmountMode.EXACT;
			};
			expanded.add(new QuestAction.GrantReward(reward.kind(), reward.id(), reward.amount(), mode));
		}
		return expanded;
	}

	/**
	 * Abandonment must remove legacy quest work items in the same transaction as
	 * the NONE projection. Ordinary collected items remain untouched.
	 */
	private static void appendAbandonWorkItemCleanup(CompiledQuestDefinition definition, QuestEvent event,
			QuestEvent declaredEvent, List<QuestAction> actions) {
		boolean abandons = event instanceof QuestEvent.Abandon
			|| declaredEvent instanceof QuestEvent.Abandon
			|| actions.stream().anyMatch(QuestAction.AbandonQuest.class::isInstance);
		if (!abandons) {
			return;
		}
		for (var item : definition.definition().metadata().questWorkItems()) {
			boolean alreadyRemovesAll = actions.stream().anyMatch(action -> action instanceof QuestAction.RemoveItem removal
				&& removal.itemId() == item.itemId() && removal.removeAll());
			if (!alreadyRemovesAll) {
				actions.add(new QuestAction.RemoveItem(item.itemId(), QuestAction.RemoveItem.ALL));
			}
		}
	}

	private static void appendFinalRepeatRewards(CompiledQuestDefinition definition, QuestSnapshot snapshot,
			List<QuestAction> actions) {
		if (definition.definition().metadata().extendedRewards().isEmpty()
				|| actions.stream().noneMatch(QuestAction.CompleteQuest.class::isInstance)) {
			return;
		}
		var repeat = definition.definition().metadata().repeatPolicy();
		if (repeat.rewardRepeatCount() == 0
				|| snapshot.completeCount() != repeat.rewardRepeatCount() - 1) {
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

	/** Metadata prerequisites and start conditions gate only transitions that acquire an unaccepted quest. */
	private static boolean metadataPrerequisitesSatisfied(CompiledQuestDefinition definition,
		QuestSnapshot snapshot, QuestTransition transition) {
		if (transition.sourceNode() == null) {
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
		if (!definition.definition().metadata().prerequisites().isEmpty()) {
			QuestCondition prerequisite = new QuestCondition.QuestsFinished(
				definition.definition().metadata().prerequisites());
			if (!QuestConditionEvaluator.matches(definition.definition().progressLayout(), snapshot,
					List.of(prerequisite))) {
				return false;
			}
		}
		var groups = definition.definition().metadata().startConditionGroups();
		if (groups.isEmpty()) {
			return true;
		}
		return groups.stream().anyMatch(group -> QuestConditionEvaluator.matches(
			definition.definition().progressLayout(), snapshot, group.conditions().stream().map(startCondition ->
				(QuestCondition) switch (startCondition.type()) {
					case "finished" -> new QuestCondition.QuestsFinished(Set.of(startCondition.questId()));
					case "unfinished" -> new QuestCondition.UnfinishedQuest(Set.of(startCondition.questId()));
					case "noacquired" -> new QuestCondition.NoAcquiredQuest(Set.of(startCondition.questId()));
					case "acquired" -> new QuestCondition.AcquiredQuest(Set.of(startCondition.questId()));
					default -> throw new IllegalArgumentException(
						"unsupported start condition type: " + startCondition.type());
				}).toList()));
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
	private static boolean removalFeasible(QuestSnapshot snapshot, QuestAction.RemoveItem remove,
		Map<Integer, Integer> unequippedItems, Map<Integer, Integer> returnedItemRemovals,
		Map<Integer, Integer> plannedRemovals) {
		try {
			long available = Math.addExact(snapshot.itemCount(remove.itemId()),
				unequippedItems.getOrDefault(remove.itemId(), 0));
			long returnedRemoval = Math.min(available,
				returnedItemRemovals.getOrDefault(remove.itemId(), 0));
			long alreadyRemoved = plannedRemovals.getOrDefault(remove.itemId(), 0);
			long remaining = available - returnedRemoval - alreadyRemoved;
			return remove.removeAll() || remaining >= remove.count();
		} catch (IllegalStateException | ArithmeticException unknownFacts) {
			return false;
		}
	}

	/**
	 * A debit is feasible only when the balance was captured and the cumulative
	 * amount for this transition fits. Unknown balances fail closed.
	 */
	private static boolean debitFeasible(QuestSnapshot snapshot, QuestRewardKind kind, long amount) {
		try {
			return snapshot.balance(kind) >= amount;
		} catch (IllegalStateException unknownFacts) {
			return false;
		}
	}

	/** Exact currency writes require captured balances and currently support DP as a reset/set resource. */
	private static boolean setCurrencyFeasible(QuestSnapshot snapshot, QuestAction.SetCurrency set) {
		if (set.kind() != QuestRewardKind.DP || set.amount() > Integer.MAX_VALUE) {
			return false;
		}
		try {
			snapshot.balance(set.kind());
			return true;
		} catch (IllegalStateException unknownFacts) {
			return false;
		}
	}

	private static QuestRewardKind canonicalCurrencyKind(QuestRewardKind kind) {
		return kind == QuestRewardKind.KINAH ? QuestRewardKind.GOLD : kind;
	}
}
