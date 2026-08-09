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

class Quest50019RetailAlignmentTest {
	@Test
	void preservesRetailMetadataItemsPrerequisiteAndNpcRoute() throws Exception {
		QuestDefinition definition = load();
		QuestMetadata metadata = definition.metadata();

		assertEquals(50019, definition.id());
		assertEquals(1150062, metadata.displayNameId());
		assertEquals(9, metadata.minLevel());
		assertEquals(Integer.MAX_VALUE, metadata.maxLevel());
		assertEquals("EVENT", metadata.category());
		assertEquals(Set.of("ELYOS"), metadata.permittedRaces());
		assertTrue(metadata.cannotShare());
		assertEquals(new RepeatPolicy(255, 0, true, false), metadata.repeatPolicy());
		assertEquals(Set.of("ALL"), metadata.repeatCycles());
		assertEquals(Set.of(50010), metadata.prerequisites());
		assertEquals(List.of(new QuestItemRequirement(182215172, 3)), metadata.itemRequirements());
		assertEquals(List.of(new QuestItemRequirement(182215174, 1)), metadata.questWorkItems());
		assertEquals(List.of(new QuestReward("ITEM", 188051769, 1),
			new QuestReward("ITEM", 160010208, 1),
			new QuestReward("ITEM", 160010209, 1)), metadata.rewards());

		assertTrue(hasStartRoute(definition, 1002));
		assertTrue(hasStartRoute(definition, 20000));
		assertTrue(hasStartRoute(definition, 10000));
		assertTrue(definition.transitions().stream().anyMatch(transition ->
			"started".equals(transition.sourceNode())
				&& "reward".equals(transition.targetNode())
				&& transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 202549
				&& Integer.valueOf(39).equals(talk.dialogId())
				&& transition.conditions().contains(new QuestCondition.HasItem(182215172, 3))
				&& transition.actions().contains(new QuestAction.RemoveItem(182215172, 3))));
		assertTrue(definition.transitions().stream().anyMatch(transition ->
			"reward".equals(transition.sourceNode())
				&& "complete".equals(transition.targetNode())
				&& transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 202549
				&& transition.actions().contains(new QuestAction.GrantReward("ITEM", 188051769, 1))
				&& transition.actions().contains(new QuestAction.GrantReward("ITEM", 160010208, 1))
				&& transition.actions().contains(new QuestAction.GrantReward("ITEM", 160010209, 1))
				&& transition.actions().contains(new QuestAction.CompleteQuest(0))));
	}

	@Test
	void hasOneProductionOwnerAndNoLegacyOwner() throws Exception {
		String catalog = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/quest_definition/quest_definition_catalog.xml"));
		assertFalse(legacyScriptDataExists(), "quest_script_data directory must be fully removed");

		assertEquals(1, occurrences(catalog, "id=\"50019\""));
	}

	private static boolean hasStartRoute(QuestDefinition definition, int dialogId) {
		return definition.transitions().stream().anyMatch(transition ->
			"unaccepted".equals(transition.sourceNode())
				&& "started".equals(transition.targetNode())
				&& transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 202549
				&& Integer.valueOf(dialogId).equals(talk.dialogId())
				&& transition.conditions().contains(new QuestCondition.StartEligible()));
	}

	private static QuestDefinition load() throws Exception {
		try (InputStream input = Quest50019RetailAlignmentTest.class.getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/50019.xml")) {
			if (input == null) {
				throw new AssertionError("missing quest 50019 resource");
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
