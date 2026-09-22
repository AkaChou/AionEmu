package com.aionemu.gameserver.questEngine.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import java.io.InputStream;
import java.util.Objects;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionXmlCompiler;

import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;

/**
 * {@link QuestFactRequirements} 的推导契约。
 * Derivation contract for {@link QuestFactRequirements}.
 * <p>背景：快照里最贵的事实族只在少数条件/动作里被读取，推导必须做到「读什么就采什么」；漏采会让读取方
 * fail-closed（条件不匹配或抛异常），多采只损失性能。这里的用例锁定每类读取者的映射，并锁定 sealed
 * switch 带来的“新增类型必须显式分类”约束（编译期保证，无法用运行时断言表达）。
 * Background: the expensive fact families are read by only a few conditions/actions, so the derivation must
 * capture exactly what is read. Missing a family makes the reader fail closed (no match or an exception),
 * while capturing extra only costs performance. These cases pin the mapping per reader; the "a new sealed
 * type must be classified" rule is enforced by the compiler instead.</p>
 */
class QuestFactRequirementsTest {

	private static final int QUEST_ID = 14010;

	/** 无任何读取者时不得采集任何可选事实族。 / No reader means no optional family. */
	@Test
	void plainTransitionRequiresNoOptionalFacts() {
		QuestFactRequirements requirements = requirements(List.of(), List.of());

		assertFalse(requirements.startEligibility());
		assertFalse(requirements.worldFacts());
		assertFalse(requirements.questIdSets());
		assertFalse(requirements.inventory());
		assertFalse(requirements.equipment());
		assertFalse(requirements.craft());
		assertTrue(requirements.eventActivityQuestIds().isEmpty());
	}

	/** 任务 ID 集合只由四个完成/进行中条件触发。 / Quest-id sets are triggered only by the four completion/progress conditions. */
	@Test
	void questIdSetConditionsRequireQuestIdFacts() {
		assertTrue(requirements(List.of(new QuestCondition.QuestsFinished(Set.of(14010))), List.of()).questIdSets());
		assertTrue(requirements(List.of(new QuestCondition.UnfinishedQuest(Set.of(14010))), List.of()).questIdSets());
		assertTrue(requirements(List.of(new QuestCondition.AcquiredQuest(Set.of(14010))), List.of()).questIdSets());
		assertTrue(requirements(List.of(new QuestCondition.NoAcquiredQuest(Set.of(14010))), List.of()).questIdSets());
	}

	/** 背包事实由物品条件与增删物品动作触发；完成任务/放弃任务也要采（工作物品清理会读数量）。 */
	@Test
	void inventoryFactsFollowItemReadersIncludingWorkItemCleanup() {
		assertTrue(requirements(List.of(new QuestCondition.HasItem(182200201, 1, true)), List.of()).inventory());
		assertTrue(requirements(List.of(), List.of(new QuestAction.RemoveItem(182200201, 1))).inventory());
		assertTrue(requirements(List.of(), List.of(new QuestAction.GiveItem(182200201, 1))).inventory());
		assertTrue(requirements(List.of(), List.of(new QuestAction.CompleteQuest(0))).inventory());
		assertTrue(requirements(List.of(), List.of(new QuestAction.AbandonQuest())).inventory());
	}

	/** 装备事实由套装/装备条件与卸下动作触发。 / Equipment facts follow set/item conditions and unequip actions. */
	@Test
	void equipmentFactsFollowEquipmentReaders() {
		assertTrue(requirements(List.of(new QuestCondition.EquipmentSetEquipped(Set.of(6), 2, true)), List.of())
			.equipment());
		assertTrue(requirements(List.of(new QuestCondition.EquippedItem(100000001, 1, true)), List.of()).equipment());
		assertTrue(requirements(List.of(), List.of(new QuestAction.UnequipItem(100000001, 0))).equipment());
	}

	/** 制作事实由两个制作条件触发（制作动作只写技能/配方，不读快照）。 / Craft facts follow the two craft conditions. */
	@Test
	void craftFactsFollowCraftConditionsOnly() {
		assertTrue(requirements(List.of(new QuestCondition.RecipeKnown(155001001, true)), List.of()).craft());
		assertTrue(requirements(List.of(new QuestCondition.CanGrantCraftSkill(40001, 400)), List.of()).craft());
		assertFalse(requirements(List.of(), List.of(new QuestAction.GrantCraftSkill(40001, 400, true))).craft());
	}

	/** 开始资格与事件服务事实沿用旧门控语义，自引用 questId==0 解析为当前任务。 */
	@Test
	void startEligibilityAndEventActivityFollowLegacyGates() {
		QuestFactRequirements requirements = requirements(
			List.of(new QuestCondition.StartEligible(), new QuestCondition.EventActive(0, true),
				new QuestCondition.EventActive(80029, true)),
			List.of());

		assertTrue(requirements.startEligibility());
		assertEquals(Set.of(QUEST_ID, 80029), requirements.eventActivityQuestIds());
	}

	/** 兼容入口（旧三参数重载）必须保守全采，否则旧调用方会读到未捕获事实。 */
	@Test
	void conservativeEntryPointCapturesEveryOptionalFamily() {
		QuestFactRequirements requirements = QuestFactRequirements.conservative(true, Set.of(80029), true);

		assertTrue(requirements.startEligibility());
		assertTrue(requirements.questIdSets());
		assertTrue(requirements.inventory());
		assertTrue(requirements.equipment());
		assertTrue(requirements.craft());
		assertTrue(requirements.worldFacts());
		assertEquals(Set.of(80029), requirements.eventActivityQuestIds());
	}

	/** 事实需求不可变且拒绝 null。 / Requirements are immutable and reject nulls. */
	@Test
	void requirementsAreImmutableAndNonNull() {
		QuestFactRequirements requirements = QuestFactRequirements.conservative(false, Set.of(80029), false);

		assertThrows(UnsupportedOperationException.class, () -> requirements.eventActivityQuestIds().add(1));
		assertThrows(NullPointerException.class, () -> QuestFactRequirements.conservative(false, null, false));
	}

	/**
	 * 接取转换（从 NONE 到 START）必须继承元数据声明的前置条件所需事实族（如前置任务完成集合）。
	 * 任务 19638 要求前置 19637 finished，接取时必须采集 questIdSets；任务 19637 无前置，不额外采集。
	 */
	@Test
	void acquiringTransitionInheritsMetadataPrerequisites() throws Exception {
		CompiledQuestDefinition quest19638 = loadQuest(19638);
		QuestTransition accept19638 = findTransition(quest19638, "unaccepted", "started");
		QuestFactRequirements req19638 = QuestFactRequirements.of(quest19638, accept19638.event(), accept19638);
		assertTrue(req19638.questIdSets(), "19638 接取时必须采集前置任务 ID 集合以校验 19637 完成状态");

		CompiledQuestDefinition quest19637 = loadQuest(19637);
		QuestTransition accept19637 = findTransition(quest19637, "unaccepted", "started");
		QuestFactRequirements req19637 = QuestFactRequirements.of(quest19637, accept19637.event(), accept19637);
		assertFalse(req19637.questIdSets(), "19637 无前置条件，接取转换不应多采 questIdSets");
	}

	private static CompiledQuestDefinition loadQuest(int questId) throws Exception {
		try (InputStream in = QuestFactRequirementsTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			return QuestDefinitionXmlCompiler.compile(Objects.requireNonNull(in, "missing quest " + questId));
		}
	}

	private static QuestTransition findTransition(CompiledQuestDefinition compiled, String source, String target) {
		return compiled.definition().transitions().stream()
			.filter(t -> Objects.equals(t.sourceNode(), source) && Objects.equals(t.targetNode(), target))
			.findFirst().orElseThrow();
	}

	private static QuestFactRequirements requirements(List<QuestCondition> conditions, List<QuestAction> actions) {
		QuestEvent event = new QuestEvent.TalkToNpc(203700);
		QuestTransition transition = new QuestTransition(event, conditions, actions, "started", List.of(), null);
		return QuestFactRequirements.of(QUEST_ID, event, transition);
	}
}
