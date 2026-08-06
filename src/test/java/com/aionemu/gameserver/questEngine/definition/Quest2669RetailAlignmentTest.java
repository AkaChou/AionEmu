package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Retail-anchored structural coverage for the Asmodian Atla escort. */
class Quest2669RetailAlignmentTest {
	private static final Path XML = Path.of(
		"src/main/resources/aion/data/static_data/quest_definition/quests/2669.xml");

	@Test
	void preservesMetadataPrerequisiteAndRewards() throws Exception {
		QuestMetadata metadata = load().definition().metadata();
		assertEquals("[Group] Atla's Escape", metadata.name());
		assertEquals(1103969, metadata.displayNameId());
		assertEquals(35, metadata.minLevel());
		assertEquals(Set.of("ASMODIANS"), metadata.permittedRaces());
		assertTrue(metadata.cannotShare());
		assertEquals(Set.of(2668), metadata.prerequisites());
		assertEquals(6, metadata.rewards().size());
		assertEquals(new QuestReward("EXP", 0, 2954681), metadata.rewards().get(0));
		assertEquals(new QuestReward("ITEM", 188051194, 1), metadata.rewards().get(5));
		assertEquals(Set.of(114100805, 114300815, 114500777, 114600736), metadata.rewards().stream()
			.filter(reward -> reward.kind().equals("SELECTABLE_ITEM"))
			.map(QuestReward::id)
			.collect(java.util.stream.Collectors.toSet()));
	}

	@Test
	void replacesTheBlockedZoneApproximationWithTheRetailExitCoordinate() throws Exception {
		var transitions = load().definition().transitions();
		QuestTransition startFollow = transitions.stream()
			.filter(t -> t.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 204815 && Integer.valueOf(10000).equals(talk.dialogId()))
			.findFirst().orElseThrow();
		assertEquals("following", startFollow.targetNode());
		assertTrue(startFollow.conditions().contains(new QuestCondition.QuestVariableIs("var0", 0)));
		assertTrue(startFollow.actions().contains(new QuestAction.SetVariable("var0", 1)));
		AfterCommitAction.StartFollowCurrentTargetToPoint coordinate = startFollow.afterCommit().stream()
			.filter(AfterCommitAction.StartFollowCurrentTargetToPoint.class::isInstance)
			.map(AfterCommitAction.StartFollowCurrentTargetToPoint.class::cast)
			.findFirst().orElseThrow();
		assertEquals(609.212646f, coordinate.x());
		assertEquals(527.0f, coordinate.y());
		assertEquals(200.05792f, coordinate.z());

		QuestTransition reach = transitions.stream()
			.filter(t -> t.event() instanceof QuestEvent.NpcReachTarget)
			.findFirst().orElseThrow();
		assertEquals("reward", reach.targetNode());
		assertEquals(QuestStatus.START, reach.conditions().stream()
			.filter(QuestCondition.StatusIs.class::isInstance)
			.map(QuestCondition.StatusIs.class::cast)
			.map(QuestCondition.StatusIs::status)
			.findFirst().orElseThrow());
		assertTrue(reach.conditions().contains(new QuestCondition.QuestVariableIs("var0", 1)));

		for (QuestEvent event : new QuestEvent[] {new QuestEvent.NpcLostTarget(), new QuestEvent.LogOut(null)}) {
			assertTrue(transitions.stream().anyMatch(t -> t.event().equals(event)
				&& t.sourceNode().equals("following") && t.targetNode().equals("started")
				&& t.actions().contains(new QuestAction.SetVariable("var0", 0))));
		}
	}

	@Test
	void retainsFourDistinctSelectableCompletionBranches() throws Exception {
		var transitions = load().definition().transitions();
		var completions = transitions.stream()
			.filter(t -> t.sourceNode().equals("reward") && t.targetNode().equals("complete")
				&& t.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 204777 && talk.dialogId() != null
				&& talk.dialogId() >= 8 && talk.dialogId() <= 11)
			.toList();
		assertEquals(4, completions.size());
		assertEquals(Set.of(114100805, 114300815, 114500777, 114600736), completions.stream()
			.flatMap(t -> t.actions().stream())
			.filter(QuestAction.GrantReward.class::isInstance)
			.map(QuestAction.GrantReward.class::cast)
			.map(QuestAction.GrantReward::id)
			.filter(Set.of(114100805, 114300815, 114500777, 114600736)::contains)
			.collect(java.util.stream.Collectors.toSet()));
		assertTrue(completions.stream().allMatch(t -> t.actions().contains(new QuestAction.CompleteQuest(0))));
		assertTrue(completions.stream().allMatch(t -> t.afterCommit().contains(new AfterCommitAction.ShowQuestSelectionDialog(10))));
	}

	private static CompiledQuestDefinition load() throws Exception {
		try (InputStream input = Files.newInputStream(XML)) {
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
