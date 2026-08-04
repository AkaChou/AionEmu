package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Full vertical proof for the migrated Poeta mission-chain owners 1000 / 1002 / 1003 / 1100. */
class MissionFamilyDefinitionTest {
	private static final String MANIFEST =
		"/aion/data/static_data/quest_definition/quest_definition_catalog.xml";

	@Test
	void packagedProductionCatalogCompilesTheMigratedMissionOwners() throws Exception {
		try (InputStream input = resource(MANIFEST)) {
			QuestCatalog catalog = QuestDefinitionCatalogManifest.compile(input, getClass().getClassLoader());
			// Full catalog <-> quests/*.xml agreement is proven by QuestDefinitionCatalogManifestTest.
			assertTrue(catalog.find(1000).isPresent());
			assertTrue(catalog.find(1002).isPresent());
			assertTrue(catalog.find(1003).isPresent());
			assertTrue(catalog.find(1005).isPresent());
			assertTrue(catalog.find(1100).isPresent());
		}
	}

	@Test
	void prologueStartsOnEnteringThePlainsAndCompletesAfterTheMovie() throws Exception {
		CompiledQuestDefinition compiled = definition("1000.xml");
		List<QuestTransition> transitions = compiled.definition().transitions();

		QuestTransition start = transitions.stream()
			.filter(t -> t.sourceNode().equals("unaccepted") && t.targetNode().equals("started"))
			.findFirst().orElseThrow();
		assertEquals(new QuestEvent.EnterZone("AKARIOS_PLAINS_210010000"), start.event());
		assertEquals(List.of(new AfterCommitAction.PlayMovie(1),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH)), start.afterCommit());

		QuestTransition done = transitions.stream()
			.filter(t -> t.sourceNode().equals("started") && t.targetNode().equals("complete"))
			.findFirst().orElseThrow();
		assertEquals(new QuestEvent.MovieEnd(1), done.event());
		assertTrue(done.actions().contains(new QuestAction.GrantReward("EXP", 0, 1, QuestRewardAmountMode.QUEST_BASE)));
		assertTrue(done.actions().contains(new QuestAction.CompleteQuest(0)));
	}

	@Test
	void kerubThreatKillsFiveKerubsThenTurnsInTheKerubimCorpses() throws Exception {
		CompiledQuestDefinition compiled = definition("1001.xml");
		List<QuestTransition> transitions = compiled.definition().transitions();
		assertTrue(transitions.stream().anyMatch(t -> t.sourceNode().equals("unaccepted")
			&& t.event().equals(new QuestEvent.LevelUp())
			&& t.conditions().contains(new QuestCondition.StartEligible())));

		// Five serial kerub kills advance v1 -> v6.
		assertEquals(5, transitions.stream()
			.filter(t -> t.event().equals(new QuestEvent.KillNpc(210670))).count());

		// The turn-in consumes three corpse items and enters reward only when held.
		QuestTransition turnIn = transitions.stream()
			.filter(t -> t.sourceNode().equals("v7") && t.targetNode().equals("reward")
				&& t.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 203071 && talk.dialogId() == 10002)
			.findFirst().orElseThrow();
		assertTrue(turnIn.conditions().contains(new QuestCondition.HasItem(182200001, 3)));
		assertTrue(turnIn.actions().contains(new QuestAction.RemoveItem(182200001, QuestAction.RemoveItem.ALL)));

		List<QuestAction> completions = transitions.stream()
			.filter(t -> t.sourceNode().equals("reward") && t.targetNode().equals("complete"))
			.flatMap(t -> t.actions().stream()).toList();
		assertTrue(completions.contains(new QuestAction.GrantReward("EXP", 0, 2100, QuestRewardAmountMode.QUEST_BASE)));
		assertTrue(completions.contains(new QuestAction.GrantReward("ITEM", 114100806, 1)));
		assertTrue(completions.contains(new QuestAction.GrantReward("ITEM", 114300816, 1)));
		assertTrue(completions.contains(new QuestAction.GrantReward("ITEM", 114500778, 1)));
	}

	@Test
	void kaliosCallStartsByEnteringTheVillageAndPaysFourSelectableRewards() throws Exception {
		CompiledQuestDefinition compiled = definition("1100.xml");
		List<QuestTransition> transitions = compiled.definition().transitions();
		assertTrue(transitions.stream().anyMatch(t -> t.sourceNode().equals("unaccepted")
			&& t.event().equals(new QuestEvent.EnterZone("AKARIOS_VILLAGE_210010000"))));

		QuestTransition reward = transitions.stream()
			.filter(t -> t.sourceNode().equals("started") && t.targetNode().equals("reward")
				&& t.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 203067 && talk.dialogId() == 1009)
			.findFirst().orElseThrow();
		assertTrue(reward.afterCommit().contains(new AfterCommitAction.ShowQuestDialog(5)));

		List<QuestAction> completions = transitions.stream()
			.filter(t -> t.sourceNode().equals("reward") && t.targetNode().equals("complete"))
			.flatMap(t -> t.actions().stream()).toList();
		assertEquals(7, completions.stream().filter(a -> a instanceof QuestAction.GrantReward granted
			&& granted.rewardKind() == QuestRewardKind.ITEM).count());
		assertTrue(completions.contains(new QuestAction.GrantReward("EXP", 0, 510, QuestRewardAmountMode.QUEST_BASE)));
		assertTrue(completions.contains(new QuestAction.GrantReward("ITEM", 100000095, 1)));
		assertTrue(completions.contains(new QuestAction.GrantReward("ITEM", 100200113, 1)));
		assertTrue(completions.contains(new QuestAction.GrantReward("ITEM", 100100012, 1)));
		assertTrue(completions.contains(new QuestAction.GrantReward("ITEM", 100600035, 1)));
	}

	@Test
	void barringTheGateAdvancesThroughEightLinearStopsThenTheSentinel() throws Exception {
		CompiledQuestDefinition compiled = definition("1005.xml");
		List<QuestTransition> transitions = compiled.definition().transitions();
		assertTrue(transitions.stream().anyMatch(t -> t.sourceNode().equals("unaccepted")
			&& t.event().equals(new QuestEvent.LevelUp())
			&& t.conditions().contains(new QuestCondition.StartEligible())));

		// Each npc stop advances exactly one step; the final object plays the gate movie.
		QuestTransition gate = transitions.stream()
			.filter(t -> t.sourceNode().equals("v8") && t.targetNode().equals("reward")
				&& t.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == 700080)
			.findFirst().orElseThrow();
		assertEquals(List.of(new AfterCommitAction.PlayMovie(21),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)), gate.afterCommit());

		List<QuestAction> completions = transitions.stream()
			.filter(t -> t.sourceNode().equals("reward") && t.targetNode().equals("complete"))
			.flatMap(t -> t.actions().stream()).toList();
		assertTrue(completions.contains(new QuestAction.GrantReward("EXP", 0, 28750, QuestRewardAmountMode.QUEST_BASE)));
		assertTrue(completions.contains(new QuestAction.GrantReward("TITLE", 1, 1)));
		assertTrue(completions.contains(new QuestAction.GrantReward("ITEM", 110101249, 1)));
		assertTrue(completions.contains(new QuestAction.GrantReward("ITEM", 110301181, 1)));
		assertTrue(completions.contains(new QuestAction.GrantReward("ITEM", 110501155, 1)));
	}

	@Test
	void illegalLoggingAdvancesAcrossNineLumberjackMobsThenTheSentinel() throws Exception {
		CompiledQuestDefinition compiled = definition("1003.xml");
		List<QuestTransition> transitions = compiled.definition().transitions();
		assertTrue(transitions.stream().anyMatch(t -> t.sourceNode().equals("unaccepted")
			&& t.event().equals(new QuestEvent.LevelUp())
			&& t.conditions().contains(new QuestCondition.StartEligible())));

		Set<Integer> lumberjacks = Set.of(210096, 210149, 210145, 210146, 210150, 210151, 210092, 210154, 210685);
		List<QuestTransition> lumberKills = transitions.stream()
			.filter(t -> t.sourceNode().equals("v1") && t.targetNode().equals("v2")
				&& t.event() instanceof QuestEvent.KillNpc kill && lumberjacks.contains(kill.npcId())).toList();
		assertEquals(9, lumberKills.size());

		// The sentinel kill chain v8 -> v9 -> v10 -> reward.
		assertEquals(3, transitions.stream()
			.filter(t -> t.event().equals(new QuestEvent.KillNpc(210160))).count());
		assertTrue(transitions.stream().anyMatch(t -> t.sourceNode().equals("v10")
			&& t.targetNode().equals("reward")
			&& t.event().equals(new QuestEvent.KillNpc(210160))));

		List<QuestAction> completions = transitions.stream()
			.filter(t -> t.sourceNode().equals("reward") && t.targetNode().equals("complete"))
			.flatMap(t -> t.actions().stream()).toList();
		assertTrue(completions.contains(new QuestAction.GrantReward("EXP", 0, 17056, QuestRewardAmountMode.QUEST_BASE)));
		assertTrue(completions.contains(new QuestAction.GrantReward("ITEM", 114100807, 1)));
		assertTrue(completions.contains(new QuestAction.GrantReward("ITEM", 114300817, 1)));
		assertTrue(completions.contains(new QuestAction.GrantReward("ITEM", 114500779, 1)));
	}

	@Test
	void requestOfTheElimCoversTheFullWorldEffectStateMachine() throws Exception {
		CompiledQuestDefinition compiled = definition("1002.xml");
		List<QuestTransition> transitions = compiled.definition().transitions();
		assertTrue(compiled.definition().metadata().prerequisites().contains(1100));

		// Both accept entrances require start eligibility.
		assertTrue(transitions.stream().anyMatch(t -> t.sourceNode().equals("unaccepted")
			&& t.event().equals(new QuestEvent.LevelUp())
			&& t.conditions().contains(new QuestCondition.StartEligible())));
		assertTrue(transitions.stream().anyMatch(t -> t.sourceNode().equals("unaccepted")
			&& t.event().equals(new QuestEvent.ZoneMissionEnd())
			&& t.conditions().contains(new QuestCondition.StartEligible())));

		// Ampeis advances to s1.
		assertTrue(talk(transitions, "s0", 203076, 10000, "s1") != null);

		// Noah's collect check requires the three samples and removes them.
		QuestTransition collect = talk(transitions, "s6", 730007, 39, "s12");
		assertTrue(collect.conditions().contains(new QuestCondition.HasItem(182200003, 3)));
		assertTrue(collect.actions().contains(new QuestAction.RemoveItem(182200003, 3)));

		// Sleeping Elder use deletes the world npc and advances.
		QuestTransition elder = talk(transitions, "s2", 730010, -1, "s4");
		assertTrue(elder.conditions().contains(new QuestCondition.HasItem(182200002, 1)));
		assertTrue(elder.afterCommit().contains(new AfterCommitAction.DeleteInteractionNpc(true)));

		// Daminu allocates a fresh 310010000 instance.
		QuestTransition daminu = talk(transitions, "s13", 730008, 10004, "s20");
		assertTrue(daminu.afterCommit().contains(new AfterCommitAction.TeleportPlayer(
			QuestInstanceTarget.nextAvailable(310010000), 310010000, 52f, 174f, 229f, (byte) 0)));

		// Belpartan starts the flight teleport and the 43s return timer.
		QuestTransition flight = talk(transitions, "s20", 205000, 31, "s20");
		assertTrue(flight.afterCommit().contains(new AfterCommitAction.FlightTeleport(1001)));
		assertTrue(flight.afterCommit().stream().anyMatch(a -> a instanceof AfterCommitAction.StartInvisibleTimer timer
			&& timer.seconds() == 43));

		// The timer end returns the player to Poeta and rewinds var0 to 14.
		QuestTransition timerEnd = transitions.stream()
			.filter(t -> t.sourceNode().equals("s20") && t.targetNode().equals("s14")
				&& t.event().equals(new QuestEvent.InvisibleTimerEnd()))
			.findFirst().orElseThrow();
		assertTrue(timerEnd.afterCommit().contains(new AfterCommitAction.TeleportPlayer(210010000, 603f, 1537f, 116f, (byte) 20)));

		// Entering the ascension world morphs the player; the non-instance revert exists only at var0=20.
		QuestTransition morph = transitions.stream()
			.filter(t -> t.sourceNode().equals("s13") && t.targetNode().equals("s13")
				&& t.event().equals(new QuestEvent.EnterWorld())
				&& t.conditions().contains(new QuestCondition.WorldIs(310010000, true)))
			.findFirst().orElseThrow();
		assertTrue(morph.afterCommit().contains(new AfterCommitAction.Morph(1)));
		assertEquals(10, transitions.stream()
			.filter(t -> t.event().equals(new QuestEvent.EnterWorld())
				&& t.conditions().contains(new QuestCondition.WorldIs(310010000, true))).count());
		QuestTransition revert = transitions.stream()
			.filter(t -> t.sourceNode().equals("s20") && t.targetNode().equals("s13")
				&& t.event().equals(new QuestEvent.EnterWorld())
				&& t.conditions().contains(new QuestCondition.WorldIs(310010000, false)))
			.findFirst().orElseThrow();
		assertTrue(revert.afterCommit().stream().anyMatch(a -> a instanceof AfterCommitAction.SyncQuestState));

		// The Sleeping Elder is interactive only at steps 2 and 4.
		assertEquals(2, transitions.stream()
			.filter(t -> t.event() instanceof QuestEvent.CanAct canAct && canAct.templateId() == 730010).count());

		// Six selectable reward routes, each carrying the shared typed reward set.
		List<List<QuestAction>> completions = transitions.stream()
			.filter(t -> t.sourceNode().equals("reward") && t.targetNode().equals("complete"))
			.map(QuestTransition::actions).toList();
		assertEquals(6, completions.size());
		assertTrue(completions.stream().allMatch(actions -> actions.contains(
			new QuestAction.GrantReward("EXP", 0, 5943, QuestRewardAmountMode.QUEST_BASE))));
		assertTrue(completions.stream().allMatch(actions -> actions.contains(
			new QuestAction.GrantReward("TITLE", 4, 1))));
		assertEquals(6, completions.stream().flatMap(List::stream)
			.filter(a -> a instanceof QuestAction.GrantReward reward && reward.rewardKind() == QuestRewardKind.ITEM).count());
	}

	private static QuestTransition talk(List<QuestTransition> transitions, String source, int npcId,
			int dialogId, String target) {
		return transitions.stream().filter(t -> t.sourceNode().equals(source) && t.targetNode().equals(target))
			.filter(t -> t.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == npcId
				&& Integer.valueOf(dialogId).equals(talk.dialogId()))
			.findFirst().orElse(null);
	}

	private CompiledQuestDefinition definition(String file) throws Exception {
		try (InputStream input = resource("/aion/data/static_data/quest_definition/quests/" + file)) {
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}

	private InputStream resource(String path) {
		InputStream input = getClass().getResourceAsStream(path);
		if (input == null) throw new IllegalStateException("missing resource " + path);
		return input;
	}
}
