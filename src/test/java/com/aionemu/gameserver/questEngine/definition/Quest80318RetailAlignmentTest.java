package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Quest80318RetailAlignmentTest {
	@Test
	void preservesRetailMetadataWorkItemAndNpcRoute() throws Exception {
		QuestDefinition definition = load();
		QuestMetadata metadata = definition.metadata();

		assertEquals(80318, definition.id());
		assertEquals(1199917, metadata.displayNameId());
		assertEquals(25, metadata.minLevel());
		assertEquals(35, metadata.maxLevel());
		assertEquals("EVENT", metadata.category());
		assertEquals(Set.of("ASMODIANS"), metadata.permittedRaces());
		assertTrue(metadata.cannotShare());
		assertEquals(RepeatPolicy.once(), metadata.repeatPolicy());
		assertEquals(List.of(new QuestItemRequirement(182215301, 1)), metadata.questWorkItems());
		assertEquals(List.of(new QuestReward("EXP", 0, 540340),
			new QuestReward("ITEM", 162000035, 10)), metadata.rewards());

		assertTrue(hasStartRoute(definition, 1002));
		assertTrue(hasStartRoute(definition, 20000));
		assertTrue(definition.transitions().stream().anyMatch(transition ->
			"started".equals(transition.sourceNode())
				&& "reward".equals(transition.targetNode())
				&& transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 831426
				&& Integer.valueOf(1353).equals(talk.dialogId())
				&& transition.conditions().contains(new QuestCondition.HasItem(182215301, 1))
				&& transition.actions().contains(new QuestAction.RemoveItem(182215301, 1))));
		assertTrue(definition.transitions().stream().anyMatch(transition ->
			"reward".equals(transition.sourceNode())
				&& "complete".equals(transition.targetNode())
				&& transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 831426
				&& transition.actions().contains(new QuestAction.GrantReward("EXP", 0, 540340,
					QuestRewardAmountMode.QUEST_BASE))
				&& transition.actions().contains(new QuestAction.GrantReward("ITEM", 162000035, 10))
				&& transition.actions().contains(new QuestAction.CompleteQuest(0))));
	}

	@Test
	void hasOneProductionOwnerAndNoLegacyOwner() throws Exception {
		String catalog = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/quest_definition/quest_definition_catalog.xml"));
		assertFalse(legacyScriptDataExists(), "quest_script_data directory must be fully removed");

		assertEquals(1, occurrences(catalog, "id=\"80318\""));
	}

	private static boolean hasStartRoute(QuestDefinition definition, int dialogId) {
		return definition.transitions().stream().anyMatch(transition ->
			"unaccepted".equals(transition.sourceNode())
				&& "started".equals(transition.targetNode())
				&& transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 831425
				&& Integer.valueOf(dialogId).equals(talk.dialogId())
				&& transition.conditions().contains(new QuestCondition.StartEligible())
				&& transition.actions().contains(new QuestAction.GiveItem(182215301, 1)));
	}

	private static QuestDefinition load() throws Exception {
		try (InputStream input = Quest80318RetailAlignmentTest.class.getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/80318.xml")) {
			if (input == null) {
				throw new AssertionError("missing quest 80318 resource");
			}
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}

	private static int occurrences(String text, String token) {
		return text.split(java.util.regex.Pattern.quote(token), -1).length - 1;
	}
	private static boolean legacyScriptDataExists() {
		return java.nio.file.Files.exists(
			java.nio.file.Path.of("src/main/resources/aion/data/static_data/quest_script_data"));
	}

}
