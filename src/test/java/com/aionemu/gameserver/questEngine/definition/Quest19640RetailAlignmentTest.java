package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Retail-anchored structural coverage for the special mission hunt owner 19640. */
class Quest19640RetailAlignmentTest {
	private static final Path XML = Path.of(
		"src/main/resources/aion/data/static_data/quest_definition/quests/19640.xml");

	@Test
	void preservesMetadataTenKillsAndSixSelectableBranches() throws Exception {
		QuestDefinition definition;
		try (InputStream input = Files.newInputStream(XML)) {
			definition = QuestDefinitionXmlCompiler.compile(input).definition();
		}
		QuestMetadata metadata = definition.metadata();
		assertEquals("Flying through Four", metadata.name());
		assertEquals(1800385, metadata.displayNameId());
		assertEquals(50, metadata.minLevel());
		assertEquals("IMPORTANT", metadata.category());
		assertEquals(Set.of("ELYOS"), metadata.permittedRaces());
		assertEquals(Set.of(19639), metadata.startConditions().stream()
			.map(QuestStartCondition::questId).collect(java.util.stream.Collectors.toSet()));
		assertEquals(new QuestReward("EXP", 0, 6937236), metadata.rewards().get(0));
		assertEquals(6, metadata.rewards().stream()
			.filter(reward -> "SELECTABLE_ITEM".equals(reward.kind())).count());
		assertEquals(List.of(new QuestKill(1, List.of(215525, 215527))), metadata.kills());

		List<QuestTransition> transitions = definition.transitions();
		// 计数器合同：一条 npc-set 击杀路线累加，第二条在满计数后进入 reward 收口 10 杀。
		List<QuestTransition> killRoutes = transitions.stream()
			.filter(transition -> transition.event() instanceof QuestEvent.KillNpcSet(Set<Integer> npcIds)
				&& npcIds.equals(Set.of(215525, 215527)))
			.toList();
		assertEquals(2, killRoutes.size(), "19640 must accumulate and close one npc-set counter");
		assertEquals(10, definition.progressLayout().field("var1").maxValue(),
			"19640 kill counter ceiling");
		QuestTransition finish = killRoutes.stream()
			.filter(route -> "reward".equals(route.targetNode()))
			.findFirst().orElseThrow();
		assertEquals(List.of(new QuestCondition.VariableAtLeast("var1", 9)), finish.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 1),
			new QuestAction.SetVariable("var1", 10)), finish.actions());
		assertEquals(6, transitions.stream()
			.filter(transition -> "reward".equals(transition.sourceNode())
				&& "complete".equals(transition.targetNode()))
			.count());
		assertEquals(Set.of(798991), transitions.stream()
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.dialogId() != null && talk.dialogId() == 1009)
			.map(transition -> ((QuestEvent.TalkToNpc) transition.event()).npcId())
			.collect(java.util.stream.Collectors.toSet()));
	}
}
