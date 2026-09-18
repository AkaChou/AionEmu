package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Retail-anchored structural coverage for the weekly Levinshor hunt owner 13765. */
class Quest13765RetailAlignmentTest {
	private static final Path XML = Path.of(
		"src/main/resources/aion/data/static_data/quest_definition/quests/13765.xml");

	@Test
	void preservesWeeklyMetadataKillTargetAndThreeGuardNpcs() throws Exception {
		QuestDefinition definition;
		try (InputStream input = Files.newInputStream(XML)) {
			definition = QuestDefinitionXmlCompiler.compile(input).definition();
		}
		QuestMetadata metadata = definition.metadata();
		assertEquals("[Weekly] Push from the East", metadata.name());
		assertEquals(1801283, metadata.displayNameId());
		assertEquals(65, metadata.minLevel());
		assertEquals("SEEN_MARKER", metadata.category());
		assertEquals(Set.of("ELYOS"), metadata.permittedRaces());
		assertEquals(255, metadata.repeatPolicy().maxRepeatCount());
		assertEquals(List.of(new QuestReward("EXP", 0, 3618881), new QuestReward("ITEM", 186000236, 5)),
			metadata.rewards());
		// 客户端 quest_monster.csv SECTION_1<5、data_driven value0_progress_=5 与
		// 911440146 legacy handler(var1<5) 三份证据一致：单条狩猎步骤击杀 235357 共 5 次。
		assertEquals(List.of(new QuestKill(1, List.of(235357))), metadata.kills(),
			"13765 hunts one npc family in a single hunt step");
		assertEquals(5, definition.progressLayout().field("var1").maxValue(),
			"13765 kill counter ceiling");

		List<QuestTransition> transitions = definition.transitions();
		List<QuestTransition> killRoutes = transitions.stream()
			.filter(transition -> transition.event() instanceof QuestEvent.KillNpcSet(Set<Integer> npcIds)
				&& npcIds.equals(Set.of(235357)))
			.toList();
		assertEquals(2, killRoutes.size(),
			"13765 accumulates and then closes its counter in npc-set kill routes");
		QuestTransition accumulate = killRoutes.stream()
			.filter(route -> "started".equals(route.sourceNode()) && "started".equals(route.targetNode()))
			.findFirst().orElseThrow();
		assertEquals(List.of(new QuestCondition.VariableBelow("var1", 4)), accumulate.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 0),
			new QuestAction.IncrementVariable("var1", 1)), accumulate.actions());
		QuestTransition finish = killRoutes.stream()
			.filter(route -> "reward".equals(route.targetNode()))
			.findFirst().orElseThrow();
		assertEquals(List.of(new QuestCondition.VariableAtLeast("var1", 4)), finish.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 1),
			new QuestAction.SetVariable("var1", 5)), finish.actions());
		Set<Integer> reportNpcs = transitions.stream()
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.dialogId() != null && talk.dialogId() == 1009)
			.map(transition -> ((QuestEvent.TalkToNpc) transition.event()).npcId())
			.collect(Collectors.toSet());
		assertEquals(Set.of(805272, 805273, 805274), reportNpcs);
	}
}
