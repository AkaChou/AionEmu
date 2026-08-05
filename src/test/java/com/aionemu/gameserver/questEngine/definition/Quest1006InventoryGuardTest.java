package com.aionemu.gameserver.questEngine.definition;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.definition.QuestCondition.HasItem;
import com.aionemu.gameserver.questEngine.definition.QuestEvent.TalkToNpc;

class Quest1006InventoryGuardTest {
	@Test
	void preservesLegacyInventoryGuards() throws Exception {
		CompiledQuestDefinition definition;
		try (InputStream input = Quest1006InventoryGuardTest.class.getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/1006.xml")) {
			assertNotNull(input);
			definition = QuestDefinitionXmlCompiler.compile(input);
		}

		QuestTransition giveAscensionItem = definition.definition().transitions().stream()
			.filter(transition -> transition.event() instanceof TalkToNpc talk
				&& talk.npcId() == 790001 && talk.dialogId() == 10000)
			.findFirst().orElseThrow();
		assertTrue(giveAscensionItem.conditions().contains(new HasItem(182200007, 1, false)));

		QuestTransition daminuReport = definition.definition().transitions().stream()
			.filter(transition -> transition.event() instanceof TalkToNpc talk
				&& talk.npcId() == 730008 && talk.dialogId() == 31)
			.findFirst().orElseThrow();
		assertTrue(daminuReport.conditions().contains(new HasItem(182200008, 1, true)));
	}
}
