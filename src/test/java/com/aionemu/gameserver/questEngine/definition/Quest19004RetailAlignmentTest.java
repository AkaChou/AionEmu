package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Retail-anchored structural coverage for the Sanctum talk-chain owner 19004. */
class Quest19004RetailAlignmentTest {
	private static final Path XML = Path.of(
		"src/main/resources/aion/data/static_data/quest_definition/quests/19004.xml");

	@Test
	void preservesTalkChainStepsAndRewards() throws Exception {
		QuestDefinition definition;
		try (InputStream input = Files.newInputStream(XML)) {
			definition = QuestDefinitionXmlCompiler.compile(input).definition();
		}
		QuestMetadata metadata = definition.metadata();
		assertEquals("Perikles's Insight", metadata.name());
		assertEquals(1124504, metadata.displayNameId());
		assertEquals(29, metadata.minLevel());
		assertEquals("QUEST", metadata.category());
		assertTrue(metadata.cannotShare());
		assertEquals(List.of(new QuestReward("GOLD", 0, 9830), new QuestReward("EXP", 0, 37405)),
			metadata.rewards());

		List<QuestTransition> transitions = definition.transitions();
		assertTrue(transitions.stream().anyMatch(transition ->
			transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 203752 && Integer.valueOf(10000).equals(talk.dialogId())
				&& "started".equals(transition.sourceNode()) && "s1".equals(transition.targetNode())));
		assertTrue(transitions.stream().anyMatch(transition ->
			transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 203701 && Integer.valueOf(10001).equals(talk.dialogId())
				&& "s1".equals(transition.sourceNode()) && "s2".equals(transition.targetNode())));
		assertTrue(transitions.stream().anyMatch(transition ->
			transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 798500 && Integer.valueOf(1009).equals(talk.dialogId())
				&& "s2".equals(transition.sourceNode()) && "reward".equals(transition.targetNode())));
		assertTrue(transitions.stream().anyMatch(transition ->
			transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 203752 && Integer.valueOf(31).equals(talk.dialogId())
				&& transition.afterCommit().contains(new AfterCommitAction.ShowQuestDialog(1352))));
		assertTrue(transitions.stream().anyMatch(transition ->
			transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 203701 && Integer.valueOf(31).equals(talk.dialogId())
				&& transition.afterCommit().contains(new AfterCommitAction.ShowQuestDialog(1693))));
		assertTrue(transitions.stream().anyMatch(transition ->
			transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 798500 && Integer.valueOf(31).equals(talk.dialogId())
				&& transition.afterCommit().contains(new AfterCommitAction.ShowQuestDialog(2375))));
		assertTrue(transitions.stream().anyMatch(transition ->
			"reward".equals(transition.sourceNode()) && "complete".equals(transition.targetNode())
				&& transition.actions().contains(new QuestAction.GrantReward("GOLD", 0, 9830, QuestRewardAmountMode.QUEST_BASE))
				&& transition.actions().contains(new QuestAction.GrantReward("EXP", 0, 37405, QuestRewardAmountMode.QUEST_BASE))));
	}
}
