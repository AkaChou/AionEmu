package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Retail-anchored structural coverage for the A03 shard quests (21065, 23920, 25533, 25640, 25698).
 * Metadata and hunt targets are pinned to the retail data_driven_quest.xml / quest.xml tables and
 * the AionEmu quest_data.xml entries.
 */
class QuestA03ShardRetailAlignmentTest {

	private static final Path DIR = Path.of("src/main/resources/aion/data/static_data/quest_definition/quests");

	/** Metadata facts per quest: name, display-name-id, min-level, category, reward list (kind,id,amount). */
	private static final Map<Integer, List<Object>> METADATA = Map.ofEntries(
		Map.entry(21065, List.of("Swept Away", 1127065, 50, "SEEN_MARKER",
			List.of(new long[]{1, 0, 25910}, new long[]{2, 0, 3244812}))),
		Map.entry(23920, List.of("[Alliance] Battle for Siel's Western Fortress", 1803121, 45, "SEEN_MARKER",
			List.of(new long[]{2, 0, 2603450}, new long[]{3, 188056966, 1}))),
		Map.entry(25533, List.of("[Daily] Protect Saphora Forest", 1802185, 68, "SEEN_MARKER",
			List.of(new long[]{2, 0, 77328000}, new long[]{3, 188054912, 1}))),
		Map.entry(25640, List.of("[Daily] Mysterious Organisms in Norsvold", 1802383, 68, "SEEN_MARKER",
			List.of(new long[]{2, 0, 25060275}, new long[]{3, 186000237, 5}))),
		Map.entry(25698, List.of("[Weekly] Shadows after the Territory of Spiritus Base", 1803881, 70, "QUEST",
			List.of(new long[]{1, 0, 1500000}, new long[]{2, 0, 53023500}, new long[]{3, 186000500, 3}))));

	/** Hunt target npc-ids per quest from retail data_driven_quest.xml progress_info. */
	private static final Map<Integer, Set<Integer>> HUNT_NPCS = Map.of(
		23920, Set.of(263026, 263027, 263028, 263029, 263030, 263041, 263042, 263043, 263044, 263045),
		25533, Set.of(240467, 240469, 237613, 237618, 237623, 238840, 238845, 238850, 238855, 238860,
			238865, 238870, 238875, 238880, 238885, 238890, 238895, 238900, 238905, 238910, 238915, 238920, 238925),
		25640, Set.of(237455, 237460, 237450, 237494, 237499, 237489, 237560, 237555, 237550, 237618, 237623, 237613),
		25698, Set.of(885487, 885488, 885489, 885490));

	/**
	 * 零售 progress_info 只登记了部分等级变体，客户端 quest_monster 合同声明的是整族变体（含世界中真正刷新的 T_ 变体）。
	 * 这些任务的击杀目标必须覆盖零售名单，并以评审后的客户端变体合同快照为准。
	 * Retail progress_info enumerates only part of the level variants while the client quest_monster contract lists the
	 * whole family, so these quests must cover the retail list and match the reviewed client variant contract snapshot.
	 */
	private static final Set<Integer> RETAIL_PROGRESS_LIST_IS_PARTIAL = Set.of(25533, 25640);

	/** 客户端变体合同快照（生成脚本见 .agents/summary/quest-15546-kill-progress/）。 */
	private static final Path CONTRACT_TSV =
		Path.of("src/test/resources/quest/iluma-norsvold-kill-target-contract.tsv");

	/** Hunt step counts (kills required) per quest from retail data_driven_quest.xml. */
	private static final Map<Integer, Integer> HUNT_STEPS = Map.of(
		23920, 10, 25533, 30, 25640, 30, 25698, 5);

	private static CompiledQuestDefinition load(int questId) throws Exception {
		try (InputStream input = Files.newInputStream(DIR.resolve(questId + ".xml"))) {
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}

	@Test
	void metadataMatchesRetailQuestData() throws Exception {
		for (Map.Entry<Integer, List<Object>> entry : METADATA.entrySet()) {
			int questId = entry.getKey();
			List<Object> facts = entry.getValue();
			QuestMetadata metadata = load(questId).definition().metadata();
			assertEquals(facts.get(0), metadata.name(), "name of " + questId);
			assertEquals(facts.get(1), metadata.displayNameId(), "display-name-id of " + questId);
			assertEquals(facts.get(2), metadata.minLevel(), "min-level of " + questId);
			assertEquals(facts.get(3), metadata.category(), "category of " + questId);
			assertTrue(metadata.permittedRaces().contains("ASMODIANS"), "race of " + questId);
			@SuppressWarnings("unchecked")
			List<long[]> expectedRewards = (List<long[]>) facts.get(4);
			assertEquals(expectedRewards.size(), metadata.rewards().size(), "reward count of " + questId);
			for (int i = 0; i < expectedRewards.size(); i++) {
				long[] expected = expectedRewards.get(i);
				QuestReward reward = metadata.rewards().get(i);
				assertEquals(expected[0], kindOrdinal(reward.kind()), "reward kind of " + questId + "#" + i);
				assertEquals(expected[1], reward.id(), "reward id of " + questId + "#" + i);
				assertEquals(expected[2], reward.amount(), "reward amount of " + questId + "#" + i);
			}
		}
	}

	private static long kindOrdinal(String kind) {
        switch (kind) {
            case "GOLD":
                return 1;
            case "EXP":
                return 2;
            case "ITEM":
                return 3;
            default:
                throw new IllegalArgumentException("unexpected reward kind " + kind);
        }
	}

	@Test
	void huntTargetsAndStepsMatchRetailProgressInfo() throws Exception {
		Map<Integer, Set<Integer>> clientContract = clientVariantContract();
		for (Map.Entry<Integer, Set<Integer>> entry : HUNT_NPCS.entrySet()) {
			int questId = entry.getKey();
			Set<Integer> expectedNpcs = entry.getValue();
			List<QuestTransition> transitions = load(questId).definition().transitions();
			Set<Integer> killNpcs = new HashSet<>();
			for (QuestTransition transition : transitions) {
				if (transition.event() instanceof QuestEvent.KillNpc(int npcId)) {
					killNpcs.add(npcId);
				} else if (transition.event() instanceof QuestEvent.KillNpcSet(Set<Integer> npcIds)) {
					killNpcs.addAll(npcIds);
				}
			}
			if (RETAIL_PROGRESS_LIST_IS_PARTIAL.contains(questId)) {
				assertTrue(killNpcs.containsAll(expectedNpcs),
					"retail progress list of " + questId + " must stay covered");
				assertEquals(clientContract.get(questId), killNpcs, "client variant contract of " + questId);
			} else {
				assertEquals(expectedNpcs, killNpcs, "kill-npc targets of " + questId);
			}

			// 计数器合同：击杀路线把 var1 累加到零售要求次数，完成后进入 reward。
			int required = HUNT_STEPS.get(questId);
			assertEquals(required, load(questId).definition().progressLayout().field("var1").maxValue(),
				"kill counter ceiling of " + questId);
			List<QuestTransition> killRoutes = transitions.stream()
				.filter(transition -> transition.event() instanceof QuestEvent.KillNpc
					|| transition.event() instanceof QuestEvent.KillNpcSet)
				.toList();
			assertFalse(killRoutes.isEmpty(), "quest " + questId + " must have kill transitions");
			for (QuestTransition killRoute : killRoutes) {
				assertEquals("started", killRoute.sourceNode(), "kill source of " + questId);
			}
			QuestTransition completion = killRoutes.stream()
				.filter(killRoute -> "reward".equals(killRoute.targetNode()))
				.findFirst().orElseThrow();
			assertEquals(List.of(new QuestCondition.VariableAtLeast("var1", required - 1)),
				completion.conditions(), "completion gate of " + questId);
			assertEquals(List.of(new QuestAction.SetVariable("var0", 1),
				new QuestAction.SetVariable("var1", required)), completion.actions(),
				"completion counters of " + questId);
		}
	}

	/** 读取客户端变体合同快照：任务 -> 全部击杀目标 NPC ID。 */
	private static Map<Integer, Set<Integer>> clientVariantContract() throws IOException {
		Map<Integer, Set<Integer>> rows = new HashMap<>();
		for (String line : Files.readAllLines(CONTRACT_TSV)) {
			String trimmed = line.trim();
			if (!trimmed.contains("\t") || trimmed.startsWith("#") || trimmed.startsWith("quest_id")) {
				continue;
			}
			String[] columns = trimmed.split("\t");
			Set<Integer> targets = new HashSet<>();
			for (String token : columns[1].trim().split(" ")) {
				if (!token.isEmpty()) {
					targets.add(Integer.parseInt(token));
				}
			}
			rows.put(Integer.parseInt(columns[0].trim()), targets);
		}
		return rows;
	}

	@Test
	void pureTalkQuest21065HasNoKillTransitions() throws Exception {
		CompiledQuestDefinition compiled = load(21065);
		boolean hasKill = compiled.definition().transitions().stream()
			.anyMatch(t -> t.event() instanceof QuestEvent.KillNpc || t.event() instanceof QuestEvent.KillNpcSet);
		assertFalse(hasKill, "21065 is a pure talk quest");
		Set<Integer> talkNpcs = new HashSet<>();
		for (QuestTransition transition : compiled.definition().transitions()) {
			if (transition.event() instanceof QuestEvent.TalkToNpc talk) {
				talkNpcs.add(talk.npcId());
			}
		}
		assertEquals(Set.of(799231, 799322), talkNpcs, "21065 talk npcs (Niamela + Herka)");
	}
}
