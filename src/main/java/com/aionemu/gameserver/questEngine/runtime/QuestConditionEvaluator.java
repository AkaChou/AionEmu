package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.questEngine.definition.ProgressLayout;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;

import java.util.List;
import java.util.Map;

/** Pure evaluator; it has no service or mutable-state access. */
public final class QuestConditionEvaluator {
	private QuestConditionEvaluator() {
	}

	public static boolean matches(ProgressLayout layout, QuestSnapshot snapshot,
			List<QuestCondition> conditions) {
		Map<String, Integer> variables = layout.unpack(snapshot.packedVariables());
		for (QuestCondition condition : conditions) {
				boolean matched = switch (condition) {
					case QuestCondition.StatusIs status -> snapshot.status() == status.status();
					case QuestCondition.HasItem item -> hasItem(snapshot, item);
					case QuestCondition.QuestVariableIs variable -> variables.getOrDefault(variable.field(), Integer.MIN_VALUE)
							== variable.value();
					case QuestCondition.RecipeKnown recipe -> recipeKnown(snapshot, recipe);
					case QuestCondition.CanGrantCraftSkill skill -> canGrantCraftSkill(snapshot, skill);
					case QuestCondition.PvpVictimLevelDelta level -> pvpVictimLevelDelta(snapshot, level);
					case QuestCondition.PvpRecipientInZone zone -> pvpRecipientInZone(snapshot, zone);
					case QuestCondition.StartEligible ignored -> startEligible(snapshot);
					case QuestCondition.PlayerClassIs playerClass -> playerClass(startingClass(snapshot),
						playerClass.startingClass());
					case QuestCondition.WorldIs world -> worldIs(snapshot, world);
				};
			if (!matched) {
				return false;
			}
		}
		return true;
	}

	/**
	 * An item condition matches only when the snapshot actually captured
	 * inventory facts. Unknown facts are treated as unmatched — never guessed
	 * as zero — so the shadow exposes the difference instead of a fake plan.
	 */
	private static boolean hasItem(QuestSnapshot snapshot, QuestCondition.HasItem item) {
		try {
			return snapshot.itemCount(item.itemId()) >= item.count();
		} catch (IllegalStateException unknownFacts) {
			return false;
		}
	}

	private static boolean recipeKnown(QuestSnapshot snapshot, QuestCondition.RecipeKnown condition) {
		try {
			return snapshot.recipeKnown(condition.recipeId()) == condition.expected();
		} catch (IllegalStateException unknownFacts) {
			return false;
		}
	}

	private static boolean canGrantCraftSkill(QuestSnapshot snapshot,
			QuestCondition.CanGrantCraftSkill condition) {
		try {
			return snapshot.canGrantCraftSkill(condition.skillId(), condition.targetLevel());
		} catch (IllegalStateException unknownFacts) {
			return false;
		}
	}

	private static boolean pvpVictimLevelDelta(QuestSnapshot snapshot,
			QuestCondition.PvpVictimLevelDelta condition) {
		try {
			return snapshot.pvpFacts().victimLevelDeltaBetween(condition.minimumRecipientDelta(),
				condition.maximumRecipientDelta());
		} catch (IllegalStateException unknownFacts) {
			return false;
		}
	}

	private static boolean pvpRecipientInZone(QuestSnapshot snapshot,
			QuestCondition.PvpRecipientInZone condition) {
		try {
			return snapshot.pvpFacts().recipientInZone(condition.zone());
		} catch (IllegalStateException unknownFacts) {
			return false;
		}
	}

	private static boolean startEligible(QuestSnapshot snapshot) {
		try {
			return snapshot.startEligibility().eligible();
		} catch (IllegalStateException unknownFacts) {
			return false;
		}
	}

	private static boolean playerClass(PlayerClass snapshotClass, PlayerClass expected) {
		return expected == snapshotClass;
	}

	private static PlayerClass startingClass(QuestSnapshot snapshot) {
		PlayerClass playerClass = snapshot.startingClass();
		if (playerClass == null) {
			throw new IllegalStateException("player class facts are not captured in this snapshot");
		}
		return playerClass;
	}

	private static boolean worldIs(QuestSnapshot snapshot, QuestCondition.WorldIs condition) {
		return (snapshot.worldId() == condition.worldId()) == condition.expected();
	}
}
