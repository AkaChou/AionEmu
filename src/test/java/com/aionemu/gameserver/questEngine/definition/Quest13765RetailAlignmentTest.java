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
		// 客户端证据:quest.xml 13765 块的 8 个击杀序列,metadata 需与之对齐。
		assertEquals(java.util.stream.IntStream.rangeClosed(1, 8)
				.mapToObj(sequence -> new QuestKill(sequence, List.of(235357))).toList(),
			metadata.kills());

		List<QuestTransition> transitions = definition.transitions();
		assertEquals(8, transitions.stream()
			.filter(transition -> transition.event() instanceof QuestEvent.KillNpc kill
				&& kill.npcId() == 235357)
			.count());
		Set<Integer> reportNpcs = transitions.stream()
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.dialogId() != null && talk.dialogId() == 1009)
			.map(transition -> ((QuestEvent.TalkToNpc) transition.event()).npcId())
			.collect(Collectors.toSet());
		assertEquals(Set.of(805272, 805273, 805274), reportNpcs);
	}
}
