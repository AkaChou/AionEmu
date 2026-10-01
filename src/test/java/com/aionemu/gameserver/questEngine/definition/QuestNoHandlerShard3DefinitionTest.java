package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** Verdict for the no-handler shard-3 quests 29634 / 30208 / 30565 / 30760. */
class QuestNoHandlerShard3DefinitionTest {

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
		assertInstanceOf(QuestSpawnLocation.PlayerPosition.class, spawn.location());

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

	private CompiledQuestDefinition definition(String file) {
		// Shard3 行已由真端表驱动（退役），改从生产视图取定义；file 形如 "29634.xml"。
		// The shard-3 rows are retail-driven since retirement; load via the production view.
		return ProductionQuestDefinitions.definition(Integer.parseInt(file.substring(0, file.length() - 4)));
	}

	private InputStream resource(String path) {
		InputStream input = getClass().getResourceAsStream(path);
		if (input == null) {
			throw new IllegalStateException("missing resource " + path);
		}
		return input;
	}
}
