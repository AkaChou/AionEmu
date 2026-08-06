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
		return switch (kind) {
			case "GOLD" -> 1;
			case "EXP" -> 2;
			case "ITEM" -> 3;
			default -> throw new IllegalArgumentException("unexpected reward kind " + kind);
		};
	}

	@Test
	void huntTargetsAndStepsMatchRetailProgressInfo() throws Exception {
		for (Map.Entry<Integer, Set<Integer>> entry : HUNT_NPCS.entrySet()) {
			int questId = entry.getKey();
			Set<Integer> expectedNpcs = entry.getValue();
			List<QuestTransition> transitions = load(questId).definition().transitions();
			Set<Integer> killNpcs = new HashSet<>();
			for (QuestTransition transition : transitions) {
				if (transition.event() instanceof QuestEvent.KillNpc kill) {
					killNpcs.add(kill.npcId());
				} else if (transition.event() instanceof QuestEvent.KillNpcSet set) {
					killNpcs.addAll(set.npcIds());
				}
			}
			assertEquals(expectedNpcs, killNpcs, "kill-npc targets of " + questId);

			// each hunt step is a source->target kill transition; count distinct source nodes of kill transitions
			Set<String> killSources = new HashSet<>();
			boolean killSeen = false;
			for (QuestTransition transition : transitions) {
				if (transition.event() instanceof QuestEvent.KillNpc
					|| transition.event() instanceof QuestEvent.KillNpcSet) {
					killSeen = true;
					killSources.add(transition.sourceNode());
				}
			}
			assertTrue(killSeen, "quest " + questId + " must have kill transitions");
			assertEquals(HUNT_STEPS.get(questId), killSources.size(),
				"hunt step count of " + questId);
		}
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
