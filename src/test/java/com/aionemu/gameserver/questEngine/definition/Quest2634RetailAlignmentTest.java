package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Retail-anchored structural coverage for the Draupnir Rescue escort. */
class Quest2634RetailAlignmentTest {
	private static final Path XML = Path.of(
		"src/main/resources/aion/data/static_data/quest_definition/quests/2634.xml");

	@Test
	void preservesRetailMetadataAndRewards() throws Exception {
		QuestMetadata metadata = load().definition().metadata();
		assertEquals("[Instance/Group] The Draupnir Rescue", metadata.name());
		assertEquals(1103934, metadata.displayNameId());
		assertEquals(45, metadata.minLevel());
		assertEquals(Integer.MAX_VALUE, metadata.maxLevel());
		assertEquals(Set.of("ASMODIANS"), metadata.permittedRaces());
		assertTrue(metadata.cannotShare());
		assertEquals(Set.of(new QuestReward("GOLD", 0, 30690), new QuestReward("EXP", 0, 3360640)),
			Set.copyOf(metadata.rewards()));
	}

	@Test
	void usesTheTrueDraupnirExitCoordinateForTheSpawnedSurvivor() throws Exception {
		var transitions = load().definition().transitions();
		QuestTransition escort = transitions.stream()
			.filter(t -> t.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 700350 && Integer.valueOf(10000).equals(talk.dialogId()))
			.findFirst().orElseThrow();
		assertEquals("following", escort.targetNode());
		assertTrue(escort.actions().contains(new QuestAction.SetVariable("var0", 1)));
		assertTrue(escort.afterCommit().contains(new AfterCommitAction.DeleteInteractionNpc(false)));

		AfterCommitAction.DeleteInteractionNpc delete = escort.afterCommit().stream()
			.filter(AfterCommitAction.DeleteInteractionNpc.class::isInstance)
			.map(AfterCommitAction.DeleteInteractionNpc.class::cast)
			.findFirst().orElseThrow();
		assertTrue(!delete.scheduleRespawn());
		AfterCommitAction.SpawnNpc spawn = escort.afterCommit().stream()
			.filter(AfterCommitAction.SpawnNpc.class::isInstance)
			.map(AfterCommitAction.SpawnNpc.class::cast)
			.findFirst().orElseThrow();
		assertEquals("survivor", spawn.slot());
		assertEquals(204830, spawn.templateId());
		assertInstanceOf(QuestSpawnLocation.PlayerPosition.class, spawn.location());
		assertTrue(escort.afterCommit().contains(new AfterCommitAction.StartFollow("survivor")));

		AfterCommitAction.WatchFollowCoordinate coordinate = escort.afterCommit().stream()
			.filter(AfterCommitAction.WatchFollowCoordinate.class::isInstance)
			.map(AfterCommitAction.WatchFollowCoordinate.class::cast)
			.findFirst().orElseThrow();
		assertEquals("survivor", coordinate.slot());
		assertEquals(213.177994f, coordinate.x());
		assertEquals(370.8797f, coordinate.y());
		assertEquals(503.3588f, coordinate.z());
	}

	@Test
	void preservesEscortCompletionRecoveryAndGoldExpSettlement() throws Exception {
		var transitions = load().definition().transitions();
		QuestTransition reach = transitions.stream()
			.filter(t -> t.event() instanceof QuestEvent.NpcReachTarget)
			.findFirst().orElseThrow();
		assertEquals("following", reach.sourceNode());
		assertEquals("reward", reach.targetNode());
		assertEquals(QuestStatus.START, reach.conditions().stream()
			.filter(QuestCondition.StatusIs.class::isInstance)
			.map(QuestCondition.StatusIs.class::cast)
			.map(QuestCondition.StatusIs::status)
			.findFirst().orElseThrow());
		assertTrue(reach.conditions().contains(new QuestCondition.QuestVariableIs("var0", 1)));

		for (QuestEvent event : new QuestEvent[] {new QuestEvent.NpcLostTarget(), new QuestEvent.LogOut(null)}) {
			QuestTransition recovery = transitions.stream()
				.filter(t -> t.event().equals(event) && t.sourceNode().equals("following")
					&& t.targetNode().equals("started"))
				.findFirst().orElseThrow();
			assertTrue(recovery.actions().contains(new QuestAction.SetVariable("var0", 0)));
			assertTrue(recovery.afterCommit().contains(new AfterCommitAction.DespawnNpc("survivor")));
		}

		QuestTransition completion = transitions.stream()
			.filter(t -> t.sourceNode().equals("reward") && t.targetNode().equals("complete")
				&& t.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == 204828
				&& Integer.valueOf(8).equals(talk.dialogId()))
			.findFirst().orElseThrow();
		assertTrue(completion.actions().contains(new QuestAction.GrantReward("GOLD", 0, 30690,
			QuestRewardAmountMode.QUEST_BASE)));
		assertTrue(completion.actions().contains(new QuestAction.GrantReward("EXP", 0, 3360640,
			QuestRewardAmountMode.QUEST_BASE)));
		assertTrue(completion.actions().contains(new QuestAction.CompleteQuest(0)));
		assertTrue(completion.afterCommit().contains(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION)));
	}

	private static CompiledQuestDefinition load() throws Exception {
		try (InputStream input = Files.newInputStream(XML)) {
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
