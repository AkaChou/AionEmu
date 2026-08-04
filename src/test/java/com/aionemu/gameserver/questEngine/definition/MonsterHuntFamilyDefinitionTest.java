package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Full vertical proof for the current MonsterHunt owners 1112 / 1113 / 1120. */
class MonsterHuntFamilyDefinitionTest {
	@Test
	void packagedProductionDirectoryCompilesTheThreeHuntOwners() throws Exception {
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader());
		assertTrue(catalog.find(1112).isPresent());
		assertTrue(catalog.find(1113).isPresent());
		assertTrue(catalog.find(1120).isPresent());
	}

	@Test
	void mushroomThievesKillChainAdvancesVar0OneStepPerKill() throws Exception {
		CompiledQuestDefinition compiled = definition("1113.xml");
		assertEquals(Set.of(210262, 210675), killNpcIds(compiled));
		List<QuestTransition> kills = kills(compiled);
		assertEquals(16, kills.size());
		// started -> k1 -> ... -> k8: each step advances var0 by exactly one.
		for (QuestTransition kill : kills) {
			int source = varsOf(compiled, kill.sourceNode()).get("var0");
			int target = varsOf(compiled, kill.targetNode()).get("var0");
			assertEquals(source + 1, target, kill.sourceNode() + " must advance one kill");
		}
		// The grid is a single serial chain (only one count dimension), so every
		// non-final node has exactly two outgoing kill routes (one per npc id).
		Map<String, Long> outgoing = kills.stream()
			.collect(Collectors.groupingBy(QuestTransition::sourceNode, Collectors.counting()));
		assertEquals(Set.of(2L), outgoing.values().stream().collect(Collectors.toSet()));
		assertTrue(varsOf(compiled, "started").get("var0") == 0);
		assertTrue(varsOf(compiled, "k8").get("var0") == 8);
	}

	@Test
	void thinningWorgsKillChainAdvancesVar0OneStepPerKill() throws Exception {
		CompiledQuestDefinition compiled = definition("1120.xml");
		assertEquals(Set.of(210142, 210143), killNpcIds(compiled));
		assertEquals(18, kills(compiled).size());
		for (QuestTransition kill : kills(compiled)) {
			int source = varsOf(compiled, kill.sourceNode()).get("var0");
			int target = varsOf(compiled, kill.targetNode()).get("var0");
			assertEquals(source + 1, target, kill.sourceNode() + " must advance one kill");
		}
		assertTrue(varsOf(compiled, "k9").get("var0") == 9);
	}

	@Test
	void toFishInPeaceUsesTwoIndependentSixBitKillCounts() throws Exception {
		CompiledQuestDefinition compiled = definition("1112.xml");
		assertEquals(Set.of(210259, 210260, 210065, 210066), killNpcIds(compiled));
		List<QuestTransition> kills = kills(compiled);
		assertEquals(120, kills.size());

		// var0 advances only when a water-target npc (210259/210260) dies.
		List<QuestTransition> aKills = kills.stream()
			.filter(k -> Set.of(210259, 210260).contains(((QuestEvent.KillNpc) k.event()).npcId())).toList();
		assertEquals(60, aKills.size());
		for (QuestTransition kill : aKills) {
			Map<String, Integer> source = varsOf(compiled, kill.sourceNode());
			Map<String, Integer> target = varsOf(compiled, kill.targetNode());
			assertEquals(source.get("var0") + 1, target.get("var0"), "var0 must advance on water kill");
			assertEquals(source.get("var1"), target.get("var1"), "var1 must not move on water kill");
		}
		// var1 advances only when a different target (210065/210066) dies.
		List<QuestTransition> bKills = kills.stream()
			.filter(k -> Set.of(210065, 210066).contains(((QuestEvent.KillNpc) k.event()).npcId())).toList();
		assertEquals(60, bKills.size());
		for (QuestTransition kill : bKills) {
			Map<String, Integer> source = varsOf(compiled, kill.sourceNode());
			Map<String, Integer> target = varsOf(compiled, kill.targetNode());
			assertEquals(source.get("var1") + 1, target.get("var1"), "var1 must advance on fishing kill");
			assertEquals(source.get("var0"), target.get("var0"), "var0 must not move on fishing kill");
		}
		// The acceptance gate requires BOTH counts saturated.
		Map<String, Integer> rewardVars = varsOf(compiled, "reward");
		assertEquals(5, rewardVars.get("var0"));
		assertEquals(5, rewardVars.get("var1"));
	}

	@Test
	void completionRewardsCarrySelectableItemsAndFixedRewards() throws Exception {
		CompiledQuestDefinition mushroom = definition("1113.xml");
		List<List<QuestAction>> mushroomOptions = completionActions(mushroom);
		assertEquals(2, mushroomOptions.size());
		assertTrue(mushroomOptions.contains(List.of(new QuestAction.GrantReward("EXP", 0, 1738, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("ITEM", 160003001, 3),
			new QuestAction.GrantReward("ITEM", 162000048, 1),
			new QuestAction.CompleteQuest(0))));
		assertTrue(mushroomOptions.contains(List.of(new QuestAction.GrantReward("EXP", 0, 1738, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("ITEM", 160003001, 3),
			new QuestAction.GrantReward("ITEM", 169000003, 150),
			new QuestAction.CompleteQuest(0))));

		CompiledQuestDefinition worgs = definition("1120.xml");
		List<List<QuestAction>> worgsOptions = completionActions(worgs);
		assertEquals(2, worgsOptions.size());
		assertTrue(worgsOptions.contains(List.of(new QuestAction.GrantReward("GOLD", 0, 3040, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("EXP", 0, 4455, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("ITEM", 162000048, 1),
			new QuestAction.CompleteQuest(0))));
		assertTrue(worgsOptions.contains(List.of(new QuestAction.GrantReward("GOLD", 0, 3040, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("EXP", 0, 4455, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("ITEM", 169000003, 250),
			new QuestAction.CompleteQuest(0))));

		CompiledQuestDefinition fishing = definition("1112.xml");
		// dialog-ids="8..23" expands to sixteen identical completion routes.
		List<List<QuestAction>> fishingCompletions = completionActions(fishing);
		assertEquals(16, fishingCompletions.size());
		List<QuestAction> expectedFishing = List.of(
			new QuestAction.GrantReward("GOLD", 0, 1810, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("EXP", 0, 1375, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("ITEM", 169300002, 30),
			new QuestAction.CompleteQuest(0));
		for (List<QuestAction> path : fishingCompletions) {
			assertEquals(expectedFishing, path);
		}

		assertEquals(List.of(new QuestReward("SELECTABLE_ITEM", 162000048, 1L),
			new QuestReward("SELECTABLE_ITEM", 169000003, 150L),
			new QuestReward("ITEM", 160003001, 3L)),
			mushroom.definition().metadata().rewards().stream()
				.filter(r -> !r.kind().equals("EXP")).toList());
	}

	@Test
	void productionCatalogDoesNotRetainTheLegacyMonsterHuntOwners() throws Exception {
		String legacy;
		try (InputStream input = resource("/aion/data/static_data/quest_script_data/poeta.xml")) {
			legacy = new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
		}
		assertFalse(legacy.contains("id=\"1112\""));
		assertFalse(legacy.contains("id=\"1113\""));
		assertFalse(legacy.contains("id=\"1120\""));
	}

	private static List<QuestTransition> kills(CompiledQuestDefinition compiled) {
		return compiled.definition().transitions().stream()
			.filter(t -> t.event() instanceof QuestEvent.KillNpc).toList();
	}

	private static Set<Integer> killNpcIds(CompiledQuestDefinition compiled) {
		return compiled.definition().transitions().stream()
			.map(QuestTransition::event).filter(e -> e instanceof QuestEvent.KillNpc)
			.map(e -> ((QuestEvent.KillNpc) e).npcId()).collect(Collectors.toSet());
	}

	private static Map<String, Integer> varsOf(CompiledQuestDefinition compiled, String label) {
		return compiled.definition().nodes().stream().filter(n -> n.label().equals(label))
			.findFirst().orElseThrow().projection().variables();
	}

	private static List<List<QuestAction>> completionActions(CompiledQuestDefinition compiled) {
		return compiled.definition().transitions().stream()
			.filter(t -> t.targetNode().equals("complete"))
			.map(QuestTransition::actions).toList();
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
