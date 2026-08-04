package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Full vertical proof for the current XmlQuest owners 1115 / 1127. */
class XmlQuestFamilyDefinitionTest {
	@Test
	void packagedProductionDirectoryCompilesTheTwoXmlQuestOwners() throws Exception {
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader());
		assertTrue(catalog.find(1115).isPresent());
		assertTrue(catalog.find(1127).isPresent());
	}

	@Test
	void elimMessageAdvancesThroughTheBearerThenReportsToTheEndNpc() throws Exception {
		CompiledQuestDefinition compiled = definition("1115.xml");
		List<QuestTransition> transitions = compiled.definition().transitions();

		// Accept at 203075, then step to v1 at 203072, then reward at 203058.
		assertTrue(talk(transitions, "started", 203072, 10000, "v1") != null);
		assertTrue(talk(transitions, "v1", 203058, 1009, "reward") != null);
		assertTrue(talk(transitions, "unaccepted", 203072, 31, "unaccepted") != null);
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(1352)),
			talk(transitions, "started", 203072, 31, "started").afterCommit());

		assertEquals(1, varsOf(compiled, "reward").get("var0"));
		List<QuestAction> rewardActions = completions(transitions, "reward");
		assertTrue(rewardActions.contains(new QuestAction.GrantReward("GOLD", 0, 680, QuestRewardAmountMode.QUEST_BASE)));
		assertTrue(rewardActions.contains(new QuestAction.GrantReward("EXP", 0, 2673, QuestRewardAmountMode.QUEST_BASE)));
		assertTrue(rewardActions.contains(new QuestAction.CompleteQuest(0)));
	}

	@Test
	void ancientCubeGivesTheWorkItemThenRewardsOnlyWithTheCollectedCube() throws Exception {
		CompiledQuestDefinition compiled = definition("1127.xml");
		List<QuestTransition> transitions = compiled.definition().transitions();

		// Use-object on 700001 grants the cube and advances to v1.
		QuestTransition give = talk(transitions, "started", 700001, -1, "v1");
		assertTrue(give.actions().contains(new QuestAction.GiveItem(182200215, 1)));

		// The collect-check splits on whether the cube is held.
		QuestTransition enough = talk(transitions, "v1", 798008, 39, "reward");
		assertTrue(enough.conditions().contains(new QuestCondition.HasItem(182200215, 1)));
		assertTrue(enough.actions().contains(new QuestAction.RemoveItem(182200215, 1)));
		QuestTransition missing = talk(transitions, "v1", 798008, 39, "v1");
		assertTrue(missing.conditions().isEmpty());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(2716)), missing.afterCommit());

		// The reward path carries the full typed reward set.
		List<QuestAction> complete = completions(transitions, "reward");
		assertTrue(complete.contains(new QuestAction.GrantReward("GOLD", 0, 2400, QuestRewardAmountMode.QUEST_BASE)));
		assertTrue(complete.contains(new QuestAction.GrantReward("EXP", 0, 4015, QuestRewardAmountMode.QUEST_BASE)));
		assertTrue(complete.contains(new QuestAction.CompleteQuest(0)));
	}

	@Test
	void productionCatalogDoesNotRetainTheLegacyXmlQuestOwners() throws Exception {
		String legacy;
		try (InputStream input = resource("/aion/data/static_data/quest_script_data/poeta.xml")) {
			legacy = new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
		}
		assertFalse(legacy.contains("1115"));
		assertFalse(legacy.contains("1127"));
		assertFalse(legacy.contains("<monster_hunt"));
	}

	private static QuestTransition talk(List<QuestTransition> transitions, String source, int npcId,
			Integer dialogId, String target) {
		return transitions.stream().filter(t -> t.sourceNode().equals(source) && t.targetNode().equals(target))
			.filter(t -> t.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == npcId
				&& java.util.Objects.equals(talk.dialogId(), dialogId))
			.findFirst().orElse(null);
	}

	/** Flattened action set shared by every reward->complete route (dialog-ids="8..23"). */
	private static List<QuestAction> completions(List<QuestTransition> transitions, String source) {
		return transitions.stream().filter(t -> t.sourceNode().equals(source) && t.targetNode().equals("complete"))
			.flatMap(t -> t.actions().stream()).toList();
	}

	private static java.util.Map<String, Integer> varsOf(CompiledQuestDefinition compiled, String label) {
		return compiled.definition().nodes().stream().filter(n -> n.label().equals(label))
			.findFirst().orElseThrow().projection().variables();
	}

	private CompiledQuestDefinition definition(String file) throws Exception {
		try (InputStream input = resource("/aion/data/static_data/quest_definition/quests/" + file)) {
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}

	private InputStream resource(String path) {
		InputStream input = getClass().getResourceAsStream(path);
		if (input == null) throw new IllegalStateException("missing resource " + path);
		return input;
	}
}
