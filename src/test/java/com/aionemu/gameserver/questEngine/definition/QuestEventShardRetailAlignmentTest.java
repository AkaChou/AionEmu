package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Retail-anchored structural coverage for the six event shard owners 50031/50038/50040/50041/50073/50074. */
class QuestEventShardRetailAlignmentTest {

	private static final Path DIR = Path.of("src/main/resources/aion/data/static_data/quest_definition/quests");

	/** Expected metadata per quest: (name, display-name-id, min-level, daily, weekly, cannot-share, race, rewards). */
	private static final Map<Integer, Facts> EXPECTED = Map.of(
		50031, new Facts("[Event/Daily] Deal a critical love hit", 1150198, 21, true, false, true, "ELYOS",
			List.of(new QuestReward("EXP", 0, 77777), new QuestReward("ITEM", 188100117, 10))),
		50038, new Facts("[Event/Daily] Orders From Above", 1800100, 25, true, false, true, "ELYOS",
			List.of(new QuestReward("ITEM", 188100181, 1))),
		50040, new Facts("[Event/Weekly] Death to the Officers", 1800102, 25, false, true, true, "ELYOS",
			List.of(new QuestReward("ITEM", 188053253, 1))),
		50041, new Facts("[Event/Weekly] Death to the Generals", 1800103, 25, false, true, true, "ELYOS",
			List.of(new QuestReward("ITEM", 166030007, 1))),
		50073, new Facts("[Event] Attack on the Kumuki Hideout", 1803481, 46, false, false, true, "PC_ALL",
			List.of(new QuestReward("EXP", 0, 20000000), new QuestReward("ITEM", 162001063, 5))),
		50074, new Facts("[Event] Major attack on the Kumuki Hideout", 1803482, 51, false, false, true, "PC_ALL",
			List.of(new QuestReward("EXP", 0, 55000000), new QuestReward("ITEM", 162001063, 5))));

	/** Hunt steps per quest from retail quest.xml progress_info. */
	private static final Map<Integer, Integer> KILL_STEPS = Map.of(
		50031, 5, 50038, 5, 50040, 20, 50041, 6, 50073, 15, 50074, 15);

	/** Turn-in npcs per quest (report dialog 1009 from the last kill node). */
	private static final Map<Integer, Set<Integer>> REPORT_NPCS = Map.of(
		50031, Set.of(831783), 50038, Set.of(832815), 50040, Set.of(832815),
		50041, Set.of(832815), 50073, Set.of(835570, 835571), 50074, Set.of(835570, 835571));

	private static CompiledQuestDefinition load(int questId) throws Exception {
		try (InputStream input = Files.newInputStream(DIR.resolve(questId + ".xml"))) {
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}

	@Test
	void metadataMatchesRetailQuestData() throws Exception {
		for (Map.Entry<Integer, Facts> entry : EXPECTED.entrySet()) {
			int questId = entry.getKey();
			Facts expected = entry.getValue();
			CompiledQuestDefinition compiled = load(questId);
			QuestMetadata metadata = compiled.definition().metadata();
			assertEquals(questId, compiled.id(), "id of " + questId);
			assertEquals(expected.name(), metadata.name(), "name of " + questId);
			assertEquals(expected.displayNameId(), metadata.displayNameId(), "display-name-id of " + questId);
			assertEquals(expected.minLevel(), metadata.minLevel(), "min-level of " + questId);
			assertEquals("EVENT", metadata.category(), "category of " + questId);
			assertEquals(expected.cannotShare(), metadata.cannotShare(), "cannot-share of " + questId);
			assertEquals(Set.of(expected.race()), metadata.permittedRaces(), "race of " + questId);
			assertEquals(new RepeatPolicy(255, 0, expected.daily(), expected.weekly()),
				metadata.repeatPolicy(), "repeat policy of " + questId);
			assertEquals(expected.rewards(), metadata.rewards(), "rewards of " + questId);
		}
	}

	@Test
	void huntStepsUseAnyWorldWildcardOrTheRetailKumukiMob() throws Exception {
		for (int questId : new int[] {50031, 50038, 50040, 50041}) {
			List<QuestEvent> kills = load(questId).definition().transitions().stream()
				.map(QuestTransition::event)
				.filter(event -> event instanceof QuestEvent.KillInWorld)
				.toList();
			assertEquals(KILL_STEPS.get(questId), kills.size(), "kill-in-world steps of " + questId);
			for (QuestEvent kill : kills) {
				assertEquals(0, ((QuestEvent.KillInWorld) kill).worldId(),
					"quest " + questId + " kill step must use the any-world wildcard");
			}
		}
		for (int questId : new int[] {50073, 50074}) {
			List<QuestEvent> kills = load(questId).definition().transitions().stream()
				.map(QuestTransition::event)
				.filter(event -> event instanceof QuestEvent.KillNpc)
				.toList();
			assertEquals(KILL_STEPS.get(questId), kills.size(), "kill-npc steps of " + questId);
			for (QuestEvent kill : kills) {
				assertEquals(246293, ((QuestEvent.KillNpc) kill).npcId(),
					"quest " + questId + " must hunt IDEvent_Solo_Saam_65_N");
			}
		}
	}

	@Test
	void reportAndCompletionRoutesMatchRetailTurnIn() throws Exception {
		for (int questId : EXPECTED.keySet()) {
			CompiledQuestDefinition compiled = load(questId);
			List<QuestTransition> transitions = compiled.definition().transitions();

			Set<Integer> reportNpcs = new HashSet<>();
			for (QuestTransition transition : transitions) {
				if (transition.targetNode().equals("reward")
					&& transition.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.dialogId() != null && talk.dialogId() == 1009
					&& !transition.sourceNode().equals("reward")) {
					reportNpcs.add(talk.npcId());
				}
			}
			assertEquals(REPORT_NPCS.get(questId), reportNpcs, "report npcs of " + questId);

			// 50073/50074 双 NPC 各展开 8..23 的 16 条完成路线，其余单 NPC 16 条。
			List<List<QuestAction>> completions = transitions.stream()
				.filter(t -> t.sourceNode().equals("reward"))
				.filter(t -> t.targetNode().equals("complete"))
				.filter(t -> t.event() instanceof QuestEvent.TalkToNpc)
				.map(QuestTransition::actions).toList();
			assertEquals(16 * REPORT_NPCS.get(questId).size(), completions.size(),
				"completion route count of " + questId);
			for (List<QuestAction> path : completions) {
				for (QuestReward reward : EXPECTED.get(questId).rewards()) {
					if ("EXP".equals(reward.kind())) {
						assertTrue(path.contains(new QuestAction.GrantReward("EXP", 0, reward.amount(),
							QuestRewardAmountMode.QUEST_BASE)), "exp grant of " + questId);
					} else {
						assertTrue(path.contains(new QuestAction.GrantReward(reward.kind(), reward.id(),
							reward.amount())), "reward grant of " + questId);
					}
				}
				assertTrue(path.contains(new QuestAction.CompleteQuest(0)), "completion of " + questId);
			}
		}
	}

	private record Facts(String name, int displayNameId, int minLevel, boolean daily, boolean weekly,
		boolean cannotShare, String race, List<QuestReward> rewards) {
	}
}
