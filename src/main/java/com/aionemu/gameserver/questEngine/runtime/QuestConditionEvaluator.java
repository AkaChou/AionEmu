package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.questEngine.definition.ProgressLayout;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestMembershipPermission;
import com.aionemu.gameserver.questEngine.definition.QuestNpcAttackFacts;

import java.util.List;
import java.util.Map;

/** Pure evaluator; it has no service or mutable-state access. */
public final class QuestConditionEvaluator {
	private QuestConditionEvaluator() {
	}

	public static boolean matches(ProgressLayout layout, QuestSnapshot snapshot,
			List<QuestCondition> conditions) {
		return matches(layout, snapshot, null, conditions);
	}

	public static boolean matches(ProgressLayout layout, QuestSnapshot snapshot, QuestEvent event,
			List<QuestCondition> conditions) {
		Map<String, Integer> variables = layout.unpack(snapshot.packedVariables());
		for (QuestCondition condition : conditions) {
			boolean matched = switch (condition) {
				case QuestCondition.StatusIs status -> snapshot.status() == status.status();
				case QuestCondition.HasItem item -> hasItem(snapshot, item);
				case QuestCondition.QuestVariableIs variable -> variables.getOrDefault(variable.field(), Integer.MIN_VALUE)
					== variable.value();
				case QuestCondition.VariableAtLeast variable ->
					variables.getOrDefault(variable.field(), Integer.MIN_VALUE) >= variable.value();
				case QuestCondition.VariableBelow variable ->
					variables.getOrDefault(variable.field(), Integer.MIN_VALUE) < variable.value();
				case QuestCondition.VariableSumIs variable -> variableSum(variables, variable.fields()) == variable.value();
				case QuestCondition.VariableSumBelow variable -> variableSum(variables, variable.fields()) < variable.value();
				case QuestCondition.RecipeKnown recipe -> recipeKnown(snapshot, recipe);
				case QuestCondition.CanGrantCraftSkill skill -> canGrantCraftSkill(snapshot, skill);
				case QuestCondition.PvpVictimLevelDelta level -> pvpVictimLevelDelta(snapshot, level);
				case QuestCondition.PvpRecipientInZone zone -> pvpRecipientInZone(snapshot, zone);
				case QuestCondition.StartEligible ignored -> startEligible(snapshot);
				case QuestCondition.PlayerClassIs playerClass -> playerClass(startingClass(snapshot),
					playerClass.startingClass());
				case QuestCondition.AdvancedClassIs playerClass ->
					advancedClass(snapshot, playerClass.playerClass());
				case QuestCondition.GenderIs gender -> gender(snapshot, gender);
				case QuestCondition.PlayerInGroup group -> playerInGroup(snapshot, group);
				case QuestCondition.WorldIs world -> worldIs(snapshot, world);
				case QuestCondition.WorldNpcIs npc -> worldNpcIs(snapshot, npc);
				case QuestCondition.ZoneIs zone -> zoneIs(snapshot, zone);
				case QuestCondition.NpcHpBelowPercent hp -> npcHpBelowPercent(event, hp);
				case QuestCondition.CurrencyAtLeast currency -> currencyAtLeast(snapshot, currency);
				case QuestCondition.CurrencyBelow currency -> currencyBelow(snapshot, currency);
				case QuestCondition.QuestsFinished quests -> questsFinished(snapshot, quests);
				case QuestCondition.EquipmentSetEquipped equipment -> equipmentSetEquipped(snapshot, equipment);
				case QuestCondition.EquippedItem equipped -> equippedItem(snapshot, equipped);
				case QuestCondition.MembershipPermission permission -> membershipPermission(snapshot, permission);
				case QuestCondition.DpAtMax ignored -> dpAtMax(snapshot);
				case QuestCondition.CompleteCountIs count -> completeCountIs(snapshot, count);
				case QuestCondition.EventActive active -> eventActive(snapshot, active);
			};
			if (!matched) {
				return false;
			}
		}
		return true;
	}

	private static boolean npcHpBelowPercent(QuestEvent event, QuestCondition.NpcHpBelowPercent condition) {
		if (!(event instanceof QuestEvent.AttackNpc attack)) {
			return false;
		}
		QuestNpcAttackFacts facts = attack.facts();
		return facts != null && facts.npcTemplateId() == condition.npcId()
			&& facts.belowPercent(condition.percent());
	}

	private static boolean currencyAtLeast(QuestSnapshot snapshot, QuestCondition.CurrencyAtLeast condition) {
		try {
			return snapshot.balance(condition.kind()) >= condition.amount();
		} catch (IllegalStateException unknownFacts) {
			return false;
		}
	}

	private static boolean currencyBelow(QuestSnapshot snapshot, QuestCondition.CurrencyBelow condition) {
		try {
			return snapshot.balance(condition.kind()) < condition.amount();
		} catch (IllegalStateException unknownFacts) {
			return false;
		}
	}

	private static long variableSum(Map<String, Integer> variables, List<String> fields) {
		long sum = 0;
		for (String field : fields) {
			Integer value = variables.get(field);
			if (value == null) {
				return Long.MIN_VALUE;
			}
			sum += value;
		}
		return sum;
	}

	/**
	 * 物品条件仅在快照已捕获库存事实时匹配；未知事实失败关闭，不猜测为零。
	 * Item conditions match only captured inventory facts; unknown facts fail closed instead of being guessed as zero.
	 */
	private static boolean hasItem(QuestSnapshot snapshot, QuestCondition.HasItem item) {
		try {
			return (snapshot.itemCount(item.itemId()) >= item.count()) == item.expected();
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

	private static boolean advancedClass(QuestSnapshot snapshot, PlayerClass expected) {
		PlayerClass actual = snapshot.playerClass();
		return actual != null && actual == expected;
	}

	private static boolean gender(QuestSnapshot snapshot, QuestCondition.GenderIs condition) {
		return snapshot.gender() == condition.gender();
	}

	private static boolean playerInGroup(QuestSnapshot snapshot, QuestCondition.PlayerInGroup condition) {
		QuestTeamFacts facts = snapshot.teamFacts();
		return facts != null && facts.inGroup() == condition.expected();
	}

	private static boolean worldIs(QuestSnapshot snapshot, QuestCondition.WorldIs condition) {
		// worldId == 0 means the player's position was not captured; do not turn
		// that unknown fact into a successful "not in world" condition.
		return snapshot.worldId() > 0
			&& (snapshot.worldId() == condition.worldId()) == condition.expected();
	}

	private static boolean worldNpcIs(QuestSnapshot snapshot, QuestCondition.WorldNpcIs condition) {
		QuestWorldFacts facts = snapshot.worldFacts();
		return facts != null && facts.containsNpc(condition.npcId()) == condition.expected();
	}

	private static boolean zoneIs(QuestSnapshot snapshot, QuestCondition.ZoneIs condition) {
		QuestWorldFacts facts = snapshot.worldFacts();
		return facts != null && facts.containsZone(condition.zone()) == condition.expected();
	}

	private static boolean questsFinished(QuestSnapshot snapshot, QuestCondition.QuestsFinished condition) {
		if (!snapshot.completedQuestsCaptured()) {
			return false;
		}
		return condition.questIds().stream().allMatch(snapshot::hasCompletedQuest);
	}

	private static boolean equipmentSetEquipped(QuestSnapshot snapshot,
		QuestCondition.EquipmentSetEquipped condition) {
		QuestEquipmentFacts facts = snapshot.equipmentFacts();
		return facts != null && facts.anySetHasExactly(condition.setIds(), condition.count()) == condition.expected();
	}

	private static boolean equippedItem(QuestSnapshot snapshot, QuestCondition.EquippedItem condition) {
		QuestEquipmentFacts facts = snapshot.equipmentFacts();
		return facts != null && (facts.equippedItemCount(condition.itemId()) >= condition.count())
			== condition.expected();
	}

	private static boolean membershipPermission(QuestSnapshot snapshot,
		QuestCondition.MembershipPermission condition) {
		QuestMembershipFacts facts = snapshot.membershipFacts();
		return facts != null && facts.has(condition.permission()) == condition.expected();
	}

	private static boolean dpAtMax(QuestSnapshot snapshot) {
		return snapshot.dpAtMax();
	}

	/** The completion count is always captured (zero when the quest was never completed). */
	private static boolean completeCountIs(QuestSnapshot snapshot, QuestCondition.CompleteCountIs condition) {
		return (snapshot.completeCount() == condition.value()) == condition.expected();
	}

	/**
	 * 事件激活条件仅在快照已捕获活动事实时匹配;未知事实失败关闭。
	 * Event-activity conditions match only captured facts; unknown facts fail closed.
	 */
	private static boolean eventActive(QuestSnapshot snapshot, QuestCondition.EventActive condition) {
		Boolean actual = snapshot.eventActive();
		return actual != null && actual == condition.expected();
	}
}
