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

/** Verdict for the no-handler shard-3 quests 29634 / 30208 / 30565 / 30760. */
class QuestNoHandlerShard3DefinitionTest {

	@Test
	void directoryCompilesAllFourRestoredOwners() throws Exception {
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader());
		assertTrue(catalog.find(29634).isPresent());
		assertTrue(catalog.find(30208).isPresent());
		assertTrue(catalog.find(30565).isPresent());
		assertTrue(catalog.find(30760).isPresent());
	}

	@Test
	void scaredSkurvsCarriesRetailHuntChainAndSharedTenKillCount() throws Exception {
		CompiledQuestDefinition compiled = definition("29634.xml");
		QuestMetadata meta = compiled.definition().metadata();
		assertEquals("Scared Skurvs", meta.name());
		assertEquals(1800422, meta.displayNameId());
		assertEquals(45, meta.minLevel());
		assertEquals(Set.of("ASMODIANS"), meta.permittedRaces());
		assertEquals("IMPORTANT", meta.category());
		assertEquals(List.of(new QuestStartCondition("finished", 29633, 0)), meta.startConditions());
		assertEquals(List.of(new QuestReward("EXP", 0, 6242224L),
			new QuestReward("SELECTABLE_ITEM", 110101862, 1L),
			new QuestReward("SELECTABLE_ITEM", 110301854, 1L),
			new QuestReward("SELECTABLE_ITEM", 110301855, 1L),
			new QuestReward("SELECTABLE_ITEM", 110551182, 1L),
			new QuestReward("SELECTABLE_ITEM", 110601652, 1L)), meta.rewards());

		// The four retail mobs (DF2A_KalnifSpotD_47/48_An, DF2A_ElementalWater4D_47/48_An)
		// share one total kill counter of 10, matching the deleted Java handler.
		Set<Integer> npcIds = compiled.definition().transitions().stream()
			.map(QuestTransition::event).filter(e -> e instanceof QuestEvent.KillNpcSet)
			.map(e -> ((QuestEvent.KillNpcSet) e).npcIds())
			.flatMap(Set::stream).collect(Collectors.toSet());
		assertEquals(Set.of(214371, 214372, 214440, 214441), npcIds);

		List<QuestTransition> killRoutes = compiled.definition().transitions().stream()
			.filter(t -> t.event() instanceof QuestEvent.KillNpcSet).toList();
		assertEquals(2, killRoutes.size());
		QuestTransition counting = killRoutes.stream().filter(t -> t.priority() != null && t.priority() == 1)
			.findFirst().orElseThrow();
		assertEquals(new QuestCondition.VariableBelow("var1", 10), counting.conditions().get(0));
		assertEquals(List.of(new QuestAction.IncrementVariable("var1", 1)), counting.actions());
		QuestTransition finishing = killRoutes.stream().filter(t -> t.priority() != null && t.priority() == 0)
			.findFirst().orElseThrow();
		assertEquals(new QuestCondition.VariableAtLeast("var1", 10), finishing.conditions().get(0));
		assertEquals("reward", finishing.targetNode());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 1)), finishing.actions());

		Map<String, Integer> rewardVars = varsOf(compiled, "reward");
		assertEquals(1, rewardVars.get("var0"));
		// Five selectable rewards -> five completion routes on npc 205164.
		List<List<QuestAction>> completions = completionActions(compiled);
		assertEquals(5, completions.size());
		for (List<QuestAction> path : completions) {
			assertEquals(3, path.size());
			assertEquals(new QuestAction.GrantReward("EXP", 0, 6242224, QuestRewardAmountMode.QUEST_BASE), path.get(0));
			assertEquals(new QuestAction.CompleteQuest(0), path.get(2));
		}
	}

	@Test
	void truthHurtsGivesWorkItemAtAcceptAndDeletesDrakanAfterSetReward() throws Exception {
		CompiledQuestDefinition compiled = definition("30208.xml");
		QuestMetadata meta = compiled.definition().metadata();
		assertEquals("[Group] The Truth Hurts", meta.name());
		assertEquals(1114308, meta.displayNameId());
		assertEquals(53, meta.minLevel());
		assertEquals(Set.of("ELYOS"), meta.permittedRaces());
		assertEquals("QUEST", meta.category());
		assertTrue(meta.cannotShare());
		assertEquals(List.of(new QuestStartCondition("finished", 30207, 0)), meta.startConditions());
		assertEquals(List.of(new QuestReward("EXP", 0, 6517414L),
			new QuestReward("ITEM", 186000098, 1L)), meta.rewards());

		// Accepting gives the summon ceremony work item quest_30208a (182209610).
		// started 节点内另有 1008/31 停留过渡，接取路径以 source="unaccepted" 区分。
		List<QuestTransition> acceptRoutes = compiled.definition().transitions().stream()
			.filter(t -> "unaccepted".equals(t.sourceNode())
				&& t.event() instanceof QuestEvent.TalkToNpc
				&& ((QuestEvent.TalkToNpc) t.event()).npcId() == 798941
				&& t.targetNode().equals("started")).toList();
		assertEquals(2, acceptRoutes.size());
		for (QuestTransition accept : acceptRoutes) {
			assertTrue(accept.actions().contains(new QuestAction.GiveItem(182209610, 1)));
		}

		// Faithful respondent Utra (799506) SET_REWARD deletes itself and moves to reward.
		QuestTransition ceremony = compiled.definition().transitions().stream()
			.filter(t -> t.event().equals(new QuestEvent.TalkToNpc(799506, 10255))).findFirst().orElseThrow();
		assertEquals("reward", ceremony.targetNode());
		assertTrue(ceremony.afterCommit().contains(new AfterCommitAction.DeleteInteractionNpc(true)));

		// Fixed reward completes on npc 798941 through the 8..23 dialog range.
		List<List<QuestAction>> completions = completionActions(compiled);
		assertEquals(16, completions.size());
		for (List<QuestAction> path : completions) {
			assertEquals(List.of(
				new QuestAction.GrantReward("EXP", 0, 6517414, QuestRewardAmountMode.QUEST_BASE),
				new QuestAction.GrantReward("ITEM", 186000098, 1),
				new QuestAction.CompleteQuest(0)), path);
		}
	}

	@Test
	void reunitingTheReiansIsAPureDialogTwoStepChain() throws Exception {
		CompiledQuestDefinition compiled = definition("30565.xml");
		QuestMetadata meta = compiled.definition().metadata();
		assertEquals("[Spy] Reuniting the Reians", meta.name());
		assertEquals(1801111, meta.displayNameId());
		assertEquals(65, meta.minLevel());
		assertEquals(Set.of("ASMODIANS"), meta.permittedRaces());
		assertEquals("IMPORTANT", meta.category());
		assertTrue(meta.prerequisites().isEmpty());
		assertEquals(List.of(new QuestReward("GOLD", 0, 299160L),
			new QuestReward("EXP", 0, 4432902L),
			new QuestReward("ITEM", 186000469, 210L)), meta.rewards());

		// Ekios (805156) starts; Garnon (804879) advances var0 0->1, then 1->2 reward.
		assertTrue(hasDialog(compiled, 805156, 1002, "unaccepted", "started"));
		assertTrue(hasDialog(compiled, 805156, 20000, "unaccepted", "started"));
		assertEquals(1, varsOf(compiled, "s1").get("var0"));
		assertEquals(2, varsOf(compiled, "reward").get("var0"));
		List<QuestTransition> stepRoutes = compiled.definition().transitions().stream()
			.filter(t -> t.event().equals(new QuestEvent.TalkToNpc(804879, 10000))).toList();
		assertEquals(1, stepRoutes.size());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 1)), stepRoutes.get(0).actions());
		// dialog-ids="-1 1009" 在 reward 节点另有一条同事件过渡，推进路由以 source="s1" 区分。
		List<QuestTransition> selectRoutes = compiled.definition().transitions().stream()
			.filter(t -> "s1".equals(t.sourceNode())
				&& t.event().equals(new QuestEvent.TalkToNpc(804879, 1009))).toList();
		assertEquals(1, selectRoutes.size());
		assertEquals("reward", selectRoutes.get(0).targetNode());

		// Fixed gold/exp/medal reward completes through the 8..23 dialog range.
		List<List<QuestAction>> completions = completionActions(compiled);
		assertEquals(16, completions.size());
		for (List<QuestAction> path : completions) {
			assertEquals(List.of(
				new QuestAction.GrantReward("GOLD", 0, 299160, QuestRewardAmountMode.QUEST_BASE),
				new QuestAction.GrantReward("EXP", 0, 4432902, QuestRewardAmountMode.QUEST_BASE),
				new QuestAction.GrantReward("ITEM", 186000469, 210),
				new QuestAction.CompleteQuest(0)), path);
		}
	}

	@Test
	void petrifiedHeroSpawnsHakelanAfterUsingTheStatue() throws Exception {
		CompiledQuestDefinition compiled = definition("30760.xml");
		QuestMetadata meta = compiled.definition().metadata();
		assertEquals("[Group] Petrified Hero of the Asmodians", meta.name());
		assertEquals(1137131, meta.displayNameId());
		assertEquals(57, meta.minLevel());
		assertEquals(Set.of("ASMODIANS"), meta.permittedRaces());
		assertEquals("QUEST", meta.category());
		assertTrue(meta.cannotShare());
		assertEquals(List.of(new QuestStartCondition("finished", 30759, 0)), meta.startConditions());
		assertEquals(List.of(new QuestReward("EXP", 0, 7086913L),
			new QuestReward("SELECTABLE_ITEM", 164000066, 26L),
			new QuestReward("SELECTABLE_ITEM", 164000121, 26L),
			new QuestReward("SELECTABLE_ITEM", 164000070, 26L)), meta.rewards());

		// Using the Asmodian Hero's Statue (701499, USE_OBJECT dialog -1) spawns
		// Hakelan (800458) at the player and moves to reward.
		QuestTransition statue = compiled.definition().transitions().stream()
			.filter(t -> t.event().equals(new QuestEvent.TalkToNpc(701499, -1))).findFirst().orElseThrow();
		assertEquals("reward", statue.targetNode());
		AfterCommitAction.SpawnNpc spawn = statue.afterCommit().stream()
			.filter(a -> a instanceof AfterCommitAction.SpawnNpc)
			.map(a -> (AfterCommitAction.SpawnNpc) a).findFirst().orElseThrow();
		assertEquals("hakelan", spawn.slot());
		assertEquals(800458, spawn.templateId());
		assertTrue(spawn.location() instanceof QuestSpawnLocation.PlayerPosition);

		// Three selectable scroll rewards complete on Hank (804871): dialog 8/9/10 各一条完成路线。
		List<List<QuestAction>> completions = completionActions(compiled);
		assertEquals(3, completions.size());
		assertTrue(completions.stream().anyMatch(p -> p.contains(new QuestAction.GrantReward("ITEM", 164000066, 26))));
		assertTrue(completions.stream().anyMatch(p -> p.contains(new QuestAction.GrantReward("ITEM", 164000121, 26))));
		assertTrue(completions.stream().anyMatch(p -> p.contains(new QuestAction.GrantReward("ITEM", 164000070, 26))));
	}

	private static boolean hasDialog(CompiledQuestDefinition compiled, int npcId, int dialogId,
		String source, String target) {
		return compiled.definition().transitions().stream()
			.anyMatch(t -> t.event().equals(new QuestEvent.TalkToNpc(npcId, dialogId))
				&& t.sourceNode().equals(source) && t.targetNode().equals(target));
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
		if (input == null) {
			throw new IllegalStateException("missing resource " + path);
		}
		return input;
	}
}
