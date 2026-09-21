package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.questEngine.definition.ProgressLayout;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestNpcAttackFacts;

import java.util.List;
import java.util.Map;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

/** 纯求值器：不访问任何服务或可变状态。 / Pure evaluator; it has no service or mutable-state access. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class QuestConditionEvaluator {
	public static boolean matches(ProgressLayout layout, QuestSnapshot snapshot,
			List<QuestCondition> conditions) {
		return matches(layout, snapshot, null, conditions);
	}

	public static boolean matches(ProgressLayout layout, QuestSnapshot snapshot, QuestEvent event,
			List<QuestCondition> conditions) {
		Map<String, Integer> variables = layout.unpack(snapshot.packedVariables());
		for (QuestCondition condition : conditions) {
			boolean matched;
			switch (condition) {
				case QuestCondition.StatusIs status:
					matched = snapshot.status() == status.status();
					break;
				case QuestCondition.HasItem item:
					matched = hasItem(snapshot, item);
					break;
				case QuestCondition.QuestVariableIs variable:
					matched = variables.getOrDefault(variable.field(), Integer.MIN_VALUE)
						== variable.value();
					break;
				case QuestCondition.VariableAtLeast variable:
					matched = variables.getOrDefault(variable.field(), Integer.MIN_VALUE) >= variable.value();
					break;
				case QuestCondition.VariableBelow variable:
					matched = variables.getOrDefault(variable.field(), Integer.MIN_VALUE) < variable.value();
					break;
				case QuestCondition.VariableSumIs variable:
					matched = variableSum(variables, variable.fields()) == variable.value();
					break;
				case QuestCondition.VariableSumBelow variable:
					matched = variableSum(variables, variable.fields()) < variable.value();
					break;
				case QuestCondition.RecipeKnown recipe:
					matched = recipeKnown(snapshot, recipe);
					break;
				case QuestCondition.CanGrantCraftSkill skill:
					matched = canGrantCraftSkill(snapshot, skill);
					break;
				case QuestCondition.PvpVictimLevelDelta level:
					matched = pvpVictimLevelDelta(snapshot, level);
					break;
				case QuestCondition.PvpRecipientInZone zone:
					matched = pvpRecipientInZone(snapshot, zone);
					break;
				case QuestCondition.StartEligible ignored:
					matched = startEligible(snapshot);
					break;
				case QuestCondition.PlayerClassIs playerClass:
					matched = playerClass(startingClass(snapshot),
						playerClass.startingClass());
					break;
				case QuestCondition.AdvancedClassIs playerClass:
					matched = advancedClass(snapshot, playerClass.playerClass());
					break;
				case QuestCondition.GenderIs gender:
					matched = gender(snapshot, gender);
					break;
				case QuestCondition.PlayerRaceIs race:
					matched = snapshot.race() != null && snapshot.race() == race.race();
					break;
				case QuestCondition.PlayerInGroup group:
					matched = playerInGroup(snapshot, group);
					break;
				case QuestCondition.WorldIs world:
					matched = worldIs(snapshot, world);
					break;
				case QuestCondition.WorldNpcIs npc:
					matched = worldNpcIs(snapshot, npc);
					break;
				case QuestCondition.ZoneIs zone:
					matched = zoneIs(snapshot, zone);
					break;
				case QuestCondition.NpcHpBelowPercent hp:
					matched = npcHpBelowPercent(event, hp);
					break;
				case QuestCondition.CurrencyAtLeast currency:
					matched = currencyAtLeast(snapshot, currency);
					break;
				case QuestCondition.CurrencyBelow currency:
					matched = currencyBelow(snapshot, currency);
					break;
				case QuestCondition.QuestsFinished quests:
					matched = questsFinished(snapshot, quests);
					break;
				case QuestCondition.UnfinishedQuest quests:
					matched = unfinishedQuest(snapshot, quests);
					break;
				case QuestCondition.NoAcquiredQuest quests:
					matched = noAcquiredQuest(snapshot, quests);
					break;
				case QuestCondition.AcquiredQuest quests:
					matched = acquiredQuest(snapshot, quests);
					break;
				case QuestCondition.EquipmentSetEquipped equipment:
					matched = equipmentSetEquipped(snapshot, equipment);
					break;
				case QuestCondition.EquippedItem equipped:
					matched = equippedItem(snapshot, equipped);
					break;
				case QuestCondition.MembershipPermission permission:
					matched = membershipPermission(snapshot, permission);
					break;
				case QuestCondition.DpAtMax ignored:
					matched = dpAtMax(snapshot);
					break;
				case QuestCondition.CompleteCountIs count:
					matched = completeCountIs(snapshot, count);
					break;
				case QuestCondition.EventActive active:
					matched = eventActive(snapshot, active);
					break;
				default:
					throw new IllegalArgumentException();
			}
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
		// worldId == 0 表示玩家位置未被捕获；不要把未知事实变成成功的「不在该世界」条件。
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

	/**
	 * 未完成条件仅在快照已采集完成事实时匹配;未知事实失败关闭。
	 * Unfinished-quest conditions match only captured facts; unknown facts fail closed.
	 */
	private static boolean unfinishedQuest(QuestSnapshot snapshot, QuestCondition.UnfinishedQuest condition) {
		if (!snapshot.completedQuestsCaptured()) {
			return false;
		}
		return condition.questIds().stream().noneMatch(snapshot::hasCompletedQuest);
	}

	/**
	 * 未接取条件要求完成与进行中事实均已采集;未知事实失败关闭。
	 * No-acquired conditions require both completed and active facts; unknown facts fail closed.
	 */
	private static boolean noAcquiredQuest(QuestSnapshot snapshot, QuestCondition.NoAcquiredQuest condition) {
		if (!snapshot.completedQuestsCaptured() || !snapshot.activeQuestsCaptured()) {
			return false;
		}
		return condition.questIds().stream()
			.noneMatch(id -> snapshot.hasCompletedQuest(id) || snapshot.hasActiveQuest(id));
	}

	/**
	 * 已接取条件(legacy {@code acquired})要求完成与进行中事实均已采集;
	 * 匹配每个列出的任务已完成或进行中。
	 * Acquired conditions require both completed and active facts; each listed quest
	 * must be completed or in progress.
	 */
	private static boolean acquiredQuest(QuestSnapshot snapshot, QuestCondition.AcquiredQuest condition) {
		if (!snapshot.completedQuestsCaptured() || !snapshot.activeQuestsCaptured()) {
			return false;
		}
		return condition.questIds().stream()
			.allMatch(id -> snapshot.hasCompletedQuest(id) || snapshot.hasActiveQuest(id));
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
		Boolean actual = condition.questId() == 0
			? snapshot.eventActive() : snapshot.eventActivity(condition.questId());
		return actual != null && actual == condition.expected();
	}
}
