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

	private static final Path DIR = Path.of("src/main/resources/aion/data/static_data/quest_definition/quests");

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

	@Test
	void metadataMatchesRetailQuestData() throws Exception {
		for (Map.Entry<Integer, List<Object>> entry : METADATA.entrySet()) {
			int questId = entry.getKey();
			List<Object> facts = entry.getValue();
			QuestMetadata metadata = production(questId).definition().metadata();
			// 真端驱动合成行名 Q<id>，客户端锚点保留在 displayNameId。
			// The retail driver synthesizes the row name Q<id>; the client anchor stays in displayNameId.
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
	void huntTargetsAndStepsMatchRetailProgressInfo() {
		for (Map.Entry<Integer, Set<Integer>> entry : HUNT_NPCS.entrySet()) {
			int questId = entry.getKey();
			Set<Integer> expectedNpcs = entry.getValue();
			CompiledQuestDefinition compiled = production(questId);
			QuestDefinition definition = compiled.definition();
			List<QuestTransition> transitions = definition.transitions();
			int required = HUNT_STEPS.get(questId);

			// 计数器合同：真端把击杀计数投影为 a0..aN 网格，领奖投影 var0 = 零售要求次数。
			// Counter contract: the retail shape projects the kills as the a0..aN grid and the
			// reward projection carries var0 = the retail kill requirement.
			assertEquals(required, rewardProjection(definition), "kill counter ceiling of " + questId);

			// 击杀网格：每个未满格状态覆盖零售名单（驱动显示名扩展是合法超集）并推进到下一状态。
			// Kill grid: every unfinished state covers the retail list (driver display-name
			// expansion is a legal superset) and advances to the next state.
			Set<Integer> killNpcs = null;
			for (int kills = 0; kills < required; kills++) {
				final int state = kills;
				List<QuestTransition> killRoutes = transitions.stream()
					.filter(transition -> Objects.equals(transition.sourceNode(), "a" + state)
						&& transition.event() instanceof QuestEvent.KillNpc)
					.toList();
				assertFalse(killRoutes.isEmpty(),
					() -> "quest " + questId + " state a" + state + " must have kill transitions");
				Set<Integer> stateTargets = new TreeSet<>();
				for (QuestTransition killRoute : killRoutes) {
					assertEquals("a" + (state + 1), killRoute.targetNode(),
						"kill target of " + questId);
					assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
						killRoute.afterCommit(), "kill afterCommit of " + questId);
					stateTargets.add(((QuestEvent.KillNpc) killRoute.event()).npcId());
				}
				if (killNpcs == null) {
					killNpcs = stateTargets;
				}
				assertEquals(killNpcs, stateTargets, () -> "a" + state + " target set of " + questId);
			}
			assertTrue(killNpcs.containsAll(expectedNpcs),
				() -> "retail progress list of " + questId + " must stay covered");

			// 交付收口（P0-2 规范形）：满格 a{N} 的 QUEST_SELECT 无门禁进入 reward；
			// 未满格无 QUEST_SELECT/1009 报告通道。
			// Delivery close (canonical since P0-2): the saturated a{N} QUEST_SELECT enters reward
			// ungated; unfinished nodes keep no QUEST_SELECT/1009 report channel.
			QuestTransition completion = transitions.stream()
				.filter(transition -> Objects.equals(transition.sourceNode(), "a" + required))
				.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
					&& Integer.valueOf(QuestDialogAction.QUEST_SELECT.id()).equals(talk.dialogId()))
				.findFirst().orElseThrow();
			assertEquals("reward", completion.targetNode(), "delivery route of " + questId);
			assertEquals(List.of(), completion.conditions(), "delivery gate of " + questId);
			for (int kills = 0; kills < required; kills++) {
				final int state = kills;
				assertTrue(transitions.stream().noneMatch(transition ->
					Objects.equals(transition.sourceNode(), "a" + state)
						&& transition.event() instanceof QuestEvent.TalkToNpc talk
						&& talk.dialogId() != null
						&& (Integer.valueOf(QuestDialogAction.QUEST_SELECT.id()).equals(talk.dialogId())
							|| Integer.valueOf(QuestDialogAction.SELECT_QUEST_REWARD.id())
								.equals(talk.dialogId()))),
					() -> "a" + state + " keeps no report channel of " + questId);
			}
		}
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
