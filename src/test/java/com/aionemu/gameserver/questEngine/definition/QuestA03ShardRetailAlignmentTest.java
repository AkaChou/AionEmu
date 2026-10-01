package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Retail-anchored structural coverage for the A03 shard quests (21065, 23920, 25533, 25640, 25698).
 * Metadata and hunt targets are pinned to the retail data_driven_quest.xml / quest.xml tables and
 * the AionEmu quest_data.xml entries. 已退役网格行（23920/25533/25640/25698）统一取生产驱动定义；
 * 21065 仍属独立通道，维持原 XML 装载路径。
 * Retired grid rows (23920/25533/25640/25698) load through the production driver; 21065 stays on
 * its own lane and keeps the original XML load path.
 */
class QuestA03ShardRetailAlignmentTest {

	private static final Path DIR = Path.of("src/main/resources/aion/data/static_data/quest/definitions/quests");

	/** Metadata facts per quest: name, display-name-id, min-level, category, reward list (kind,id,amount). */
	private static final Map<Integer, List<Object>> METADATA = Map.ofEntries(
		Map.entry(23920, List.of("Q23920", 1803121, 45, "SEEN_MARKER",
			List.of(new long[]{2, 0, 2603450}, new long[]{3, 188056966, 1}))),
		Map.entry(25533, List.of("Q25533", 1802185, 68, "SEEN_MARKER",
			List.of(new long[]{2, 0, 77328000}, new long[]{3, 188054912, 1}))),
		Map.entry(25640, List.of("Q25640", 1802383, 68, "SEEN_MARKER",
			List.of(new long[]{2, 0, 25060275}, new long[]{3, 186000237, 5}))),
		Map.entry(25698, List.of("Q25698", 1803881, 70, "QUEST",
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

	/** 生产驱动定义（已退役的 XML 只在 git 历史）。 / The production-driver definition (the retired XML lives only in git history). */
	private static CompiledQuestDefinition production(int questId) {
		return ProductionQuestDefinitions.definition(questId);
	}

	private static long kindOrdinal(String kind) {
		return switch (kind) {
			case "GOLD" -> 1;
			case "EXP" -> 2;
			case "ITEM" -> 3;
			default -> throw new IllegalArgumentException("unexpected reward kind " + kind);
		};
	}

	/** 领奖投影携带的击杀网格规模。 / The kill-grid size carried by the reward projection. */
	private static int rewardProjection(QuestDefinition definition) {
		return definition.nodes().stream()
			.filter(node -> "reward".equals(node.label()))
			.findFirst().orElseThrow().projection().variables().get("var0");
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
