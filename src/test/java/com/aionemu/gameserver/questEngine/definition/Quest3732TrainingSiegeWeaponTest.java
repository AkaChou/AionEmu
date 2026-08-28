package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证任务 3732 在接取时发放训练用攻城兵器，并将其登记为任务工作物品。
 * Verifies that quest 3732 grants the Training Siege Weapon on accept and registers it as a quest work item.
 */
class Quest3732TrainingSiegeWeaponTest {
	private static final int TRAINING_SIEGE_WEAPON_ID = 182202179;

	@Test
	void grantsRegisteredWorkItemOnBothAcceptRoutes() throws Exception {
		QuestDefinition definition = load();

		assertEquals(List.of(new QuestItemRequirement(TRAINING_SIEGE_WEAPON_ID, 1)),
			definition.metadata().questWorkItems());
		assertTrue(hasAcceptRoute(definition, 1002));
		assertTrue(hasAcceptRoute(definition, 20000));
	}

	private static boolean hasAcceptRoute(QuestDefinition definition, int dialogId) {
		return definition.transitions().stream().anyMatch(transition ->
			"unaccepted".equals(transition.sourceNode())
				&& "started".equals(transition.targetNode())
				&& transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 800518
				&& Integer.valueOf(dialogId).equals(talk.dialogId())
				&& transition.conditions().contains(new QuestCondition.StartEligible())
				&& transition.actions().contains(new QuestAction.GiveItem(TRAINING_SIEGE_WEAPON_ID, 1)));
	}

	private static QuestDefinition load() throws Exception {
		try (InputStream input = Quest3732TrainingSiegeWeaponTest.class.getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/3732.xml")) {
			if (input == null) {
				throw new AssertionError("missing quest 3732 resource");
			}
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}
}
