package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import com.aionemu.gameserver.questEngine.runtime.QuestStartEligibility;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Full vertical proof for the migrated Poeta Java-handler owners 1122 / 1123 / 1230 / 1231 / 1205. */
class JavaHandlerFamilyDefinitionTest {
	@Test
	void packagedProductionDirectoryCompilesTheMigratedHandlerOwners() throws Exception {
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader());
		assertTrue(catalog.find(1107).isPresent());
		assertTrue(catalog.find(1111).isPresent());
		assertTrue(catalog.find(1122).isPresent());
		assertTrue(catalog.find(1230).isPresent());
		assertTrue(catalog.find(1231).isPresent());
		assertTrue(catalog.find(1205).isPresent());
	}

	@Test
	void lostAxeUsesTheAxeThenAcceptsThroughTheTargetlessDialog() throws Exception {
		CompiledQuestDefinition compiled = definition("1107.xml");
		List<QuestTransition> transitions = compiled.definition().transitions();
		QuestTransition use = transitions.stream()
			.filter(t -> t.sourceNode().equals("unaccepted") && t.targetNode().equals("unaccepted")
				&& t.event().equals(new QuestEvent.UseItem(182200501)))
			.findFirst().orElseThrow();
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(4)), use.afterCommit());
		QuestTransition start = transitions.stream()
			.filter(t -> t.sourceNode().equals("unaccepted") && t.targetNode().equals("started")
				&& t.event().equals(new QuestEvent.QuestDialog(1002)))
			.findFirst().orElseThrow();
		assertEquals(new QuestEvent.QuestDialog(1002), start.event());

		QuestTransition reward = transitions.stream()
			.filter(t -> t.sourceNode().equals("started") && t.targetNode().equals("reward")
				&& t.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 203075 && talk.dialogId() == 1009)
			.findFirst().orElseThrow();
		assertEquals(1, varsOf(compiled, "reward").get("var0"));

		List<QuestAction> completion = completions(transitions, "reward");
		assertTrue(completion.contains(new QuestAction.GrantReward("GOLD", 0, 1560, QuestRewardAmountMode.QUEST_BASE)));
		assertTrue(completion.contains(new QuestAction.GrantReward("EXP", 0, 462, QuestRewardAmountMode.QUEST_BASE)));
	}

	@Test
	void nymphsGownStartsByUsingTheDiaryAndBranchesIntoTwoRewardPaths() throws Exception {
		CompiledQuestDefinition compiled = definition("1114.xml");
		List<QuestTransition> transitions = compiled.definition().transitions();

		QuestTransition use = transitions.stream()
			.filter(t -> t.sourceNode().equals("unaccepted") && t.targetNode().equals("unaccepted")
				&& t.event().equals(new QuestEvent.UseItem(182200214)))
			.findFirst().orElseThrow();
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(4)), use.afterCommit());

		QuestTransition start = transitions.stream()
			.filter(t -> t.sourceNode().equals("unaccepted") && t.targetNode().equals("v0")
				&& t.event().equals(new QuestEvent.QuestDialog(1002)))
			.findFirst().orElseThrow();
		assertEquals(new QuestEvent.QuestDialog(1002), start.event());
		assertTrue(start.actions().contains(new QuestAction.GiveItem(182200226, 1)));
		assertTrue(start.actions().contains(new QuestAction.RemoveItem(182200214, 1)));

		QuestTransition dress = transitions.stream()
			.filter(t -> t.sourceNode().equals("v1") && t.targetNode().equals("v2")
				&& t.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == 700008)
			.findFirst().orElseThrow();
		assertTrue(dress.actions().contains(new QuestAction.GiveItem(182200217, 1)));
		assertTrue(dress.afterCommit().contains(new AfterCommitAction.AddNpcAggro(203175, 50)));

		QuestEvent directHandoff = new QuestEvent.TalkToNpc(203075, 2375);
		QuestTransition handoffPage = transitions.stream()
			.filter(t -> t.sourceNode().equals("v2") && t.targetNode().equals("v3")
				&& t.event().equals(directHandoff))
			.findFirst().orElseThrow();
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.ShowQuestDialog(2375)), handoffPage.afterCommit());
		QuestMutationPlan handoffPlan = QuestMutationPlanner.plan(compiled,
			new QuestSnapshot(7, 1114, QuestStatus.START, 2, Map.of(182200217, 1)),
			directHandoff, handoffPage).orElseThrow();
		assertEquals(QuestStatus.START, handoffPlan.nextStatus());
		assertEquals(3, handoffPlan.nextPackedVariables());

		QuestEvent selectReward = new QuestEvent.TalkToNpc(203075, 1009);
		QuestTransition namusReward = transitions.stream()
			.filter(t -> t.sourceNode().equals("v3") && t.targetNode().equals("reward4")
				&& t.event().equals(selectReward))
			.findFirst().orElseThrow();
		QuestMutationPlan rewardPlan = QuestMutationPlanner.plan(compiled,
			new QuestSnapshot(7, 1114, QuestStatus.START, handoffPlan.nextPackedVariables(),
				Map.of(182200217, 1)), selectReward, namusReward).orElseThrow();
		assertEquals(QuestStatus.REWARD, rewardPlan.nextStatus());
		assertEquals(4, rewardPlan.nextPackedVariables());
		assertTrue(rewardPlan.requiredActions().contains(new QuestAction.RemoveItem(182200217, 1)));
		assertTrue(rewardPlan.afterCommit().contains(new AfterCommitAction.ShowQuestDialog(6)));

		List<QuestAction> reward4 = transitions.stream()
			.filter(t -> t.sourceNode().equals("reward4") && t.targetNode().equals("complete"))
			.flatMap(t -> t.actions().stream()).toList();
		assertTrue(reward4.contains(new QuestAction.GrantReward("GOLD", 0, 1920, QuestRewardAmountMode.QUEST_BASE)));
		assertTrue(reward4.contains(new QuestAction.GrantReward("EXP", 0, 4367, QuestRewardAmountMode.QUEST_BASE)));

		List<QuestAction> reward3 = transitions.stream()
			.filter(t -> t.sourceNode().equals("reward3") && t.targetNode().equals("complete"))
			.flatMap(t -> t.actions().stream()).toList();
		assertTrue(reward3.contains(new QuestAction.GrantReward("GOLD", 0, 960, QuestRewardAmountMode.QUEST_BASE)));
		assertTrue(reward3.contains(new QuestAction.GrantReward("EXP", 0, 3120, QuestRewardAmountMode.QUEST_BASE)));
	}

	@Test
	void tuttySearchAdvancesViaZoneEntryAndMovie() throws Exception {
		CompiledQuestDefinition compiled = definition("1123.xml");
		List<QuestTransition> transitions = compiled.definition().transitions();
		QuestTransition zone = transitions.stream()
			.filter(t -> t.sourceNode().equals("started") && t.targetNode().equals("reward"))
			.findFirst().orElseThrow();
		assertEquals(QuestEvent.EnterZone.class, zone.event().getClass());
		assertEquals("LF1_SENSORY_AREA_Q1123_210010000", ((QuestEvent.EnterZone) zone.event()).zone());
		assertEquals(List.of(new AfterCommitAction.PlayMovie(11),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), zone.afterCommit());

		List<QuestAction> completion = completions(transitions, "reward");
		assertTrue(completion.contains(new QuestAction.GrantReward("EXP", 0, 4565, QuestRewardAmountMode.QUEST_BASE)));
		assertTrue(completion.contains(new QuestAction.GrantReward("ITEM", 162000048, 1)));
	}

	@Test
	void robeDeliverySelectsAmongThreeItemGatedRewardBranches() throws Exception {
		CompiledQuestDefinition compiled = definition("1122.xml");
		Set<Integer> prerequisites = compiled.definition().metadata().prerequisites();
		assertTrue(prerequisites.contains(1116));

		List<QuestTransition> transitions = compiled.definition().transitions();
		// Each step dialog is gated by the matching collect item and removes it.
		assertTrue(gatedReward(transitions, 10000, 182200218, "reward1") != null);
		assertTrue(gatedReward(transitions, 10001, 182200219, "reward2") != null);
		assertTrue(gatedReward(transitions, 10002, 182200220, "reward3") != null);
		// Every step dialog also has a fallback that reports the missing item.
		for (int dialog : new int[] {10000, 10001, 10002}) {
			QuestTransition fallback = transitions.stream()
				.filter(t -> t.sourceNode().equals("started") && t.targetNode().equals("started")
					&& t.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.npcId() == 790001 && talk.dialogId() == dialog
					&& t.conditions().isEmpty())
				.findFirst().orElseThrow();
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(1608)), fallback.afterCommit());
		}
		// Each reward node resolves its own completion reward group.
		assertTrue(completions(transitions, "reward1").contains(new QuestAction.GrantReward("ITEM", 123000878, 1)));
		assertTrue(completions(transitions, "reward2").contains(new QuestAction.GrantReward("ITEM", 162000048, 1)));
		assertTrue(completions(transitions, "reward3").contains(new QuestAction.GrantReward("ITEM", 182000352, 1)));
	}

	@Test
	void insomniaMedicineBranchesByCollectedHerbsThenChoosesARewardPath() throws Exception {
		CompiledQuestDefinition compiled = definition("1111.xml");
		List<QuestTransition> transitions = compiled.definition().transitions();

		// Collect-check consumes three herbs and advances to v1; missing herbs show 1693.
		QuestTransition collect = transitions.stream()
			.filter(t -> t.sourceNode().equals("started") && t.targetNode().equals("v1")
				&& t.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 203061 && talk.dialogId() == 39)
			.findFirst().orElseThrow();
		assertTrue(collect.conditions().contains(new QuestCondition.HasItem(182200223, 3)));
		assertTrue(collect.actions().contains(new QuestAction.RemoveItem(182200223, 3)));

		// Each reward path hands a distinct work item and resolves the same typed reward set.
		QuestTransition pathA = transitions.stream()
			.filter(t -> t.sourceNode().equals("v1") && t.targetNode().equals("reward2")
				&& t.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 203061 && talk.dialogId() == 10000)
			.findFirst().orElseThrow();
		assertTrue(pathA.actions().contains(new QuestAction.GiveItem(182200222, 1)));
		QuestTransition pathB = transitions.stream()
			.filter(t -> t.sourceNode().equals("v1") && t.targetNode().equals("reward3")
				&& t.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 203061 && talk.dialogId() == 10001)
			.findFirst().orElseThrow();
		assertTrue(pathB.actions().contains(new QuestAction.GiveItem(182200221, 1)));

		List<QuestAction> completionA = completions(transitions, "reward2");
		assertTrue(completionA.contains(new QuestAction.GrantReward("GOLD", 0, 960, QuestRewardAmountMode.QUEST_BASE)));
		assertTrue(completionA.contains(new QuestAction.GrantReward("EXP", 0, 1595, QuestRewardAmountMode.QUEST_BASE)));
		assertTrue(completionA.contains(new QuestAction.CompleteQuest(0)));
	}

	@Test
	void messageForMadelinAndIrreconcilableLoversArePlainReports() throws Exception {
		CompiledQuestDefinition madelin = definition("1230.xml");
		List<QuestAction> madelinCompletions = completions(madelin.definition().transitions(), "reward");
		assertEquals(2, madelin.definition().transitions().stream()
			.filter(t -> t.sourceNode().equals("reward") && t.targetNode().equals("complete")).count());
		assertEquals(2, madelinCompletions.stream().filter(a -> a.equals(
			new QuestAction.GrantReward("GOLD", 0, 6800, QuestRewardAmountMode.QUEST_BASE))).count());
		assertTrue(madelinCompletions.contains(new QuestAction.GrantReward("ITEM", 164000076, 1)));
		assertTrue(madelinCompletions.contains(new QuestAction.GrantReward("ITEM", 164000073, 1)));
		assertTrue(madelinCompletions.contains(new QuestAction.GrantReward("ITEM", 164000134, 1)));

		CompiledQuestDefinition lovers = definition("1231.xml");
		List<QuestAction> loversCompletions = completions(lovers.definition().transitions(), "reward");
		assertEquals(16, lovers.definition().transitions().stream()
			.filter(t -> t.sourceNode().equals("reward") && t.targetNode().equals("complete")).count());
		assertTrue(loversCompletions.contains(new QuestAction.GrantReward("GOLD", 0, 6800, QuestRewardAmountMode.QUEST_BASE)));
		assertTrue(loversCompletions.contains(new QuestAction.CompleteQuest(0)));
	}

	@Test
	void newSkillRoutesEachStartingClassToItsOwnRewardNode() throws Exception {
		CompiledQuestDefinition compiled = definition("1205.xml");
		List<QuestTransition> transitions = compiled.definition().transitions();

		// Six level-up start routes, one per starting class, all landing in REWARD directly.
		List<QuestTransition> starts = transitions.stream()
			.filter(t -> t.sourceNode().equals("unaccepted")
				&& t.event().equals(new QuestEvent.LevelUp()))
			.toList();
		assertEquals(6, starts.size());
		for (QuestTransition start : starts) {
			assertTrue(start.conditions().contains(new QuestCondition.StartEligible()));
			assertTrue(start.conditions().stream()
				.anyMatch(c -> c instanceof QuestCondition.PlayerClassIs));
		}

		// The planner picks the class-specific reward node by its packed var0 (1..6).
		QuestEvent levelUp = new QuestEvent.LevelUp();
		for (var entry : Map.of(PlayerClass.WARRIOR, 1, PlayerClass.SCOUT, 2, PlayerClass.MAGE, 3,
			PlayerClass.PRIEST, 4, PlayerClass.TECHNIST, 5, PlayerClass.MUSE, 6).entrySet()) {
			QuestSnapshot snapshot = new QuestSnapshot(7, 1205, QuestStatus.NONE, 0, Map.of())
				.withStartEligibility(QuestStartEligibility.allowed())
				.withStartingClass(entry.getKey());
			QuestTransition own = startRoute(transitions, entry.getValue());
			QuestMutationPlan plan = QuestMutationPlanner.plan(compiled, snapshot, levelUp, own)
				.orElseThrow();
			assertEquals(QuestStatus.REWARD, plan.nextStatus());
			assertEquals(entry.getValue(), plan.nextPackedVariables(),
				entry.getKey() + " must route to its own reward node");
			// The same snapshot is rejected by every other class route.
			for (int other = 1; other <= 6; other++) {
				if (other == entry.getValue()) {
					continue;
				}
				assertTrue(QuestMutationPlanner.plan(compiled, snapshot, levelUp,
					startRoute(transitions, other)).isEmpty(),
					entry.getKey() + " must not route to reward" + other);
			}
		}

		// The class npc routes resolve to the same typed reward set.
		QuestTransition warriorDialog = transitions.stream()
			.filter(t -> t.sourceNode().equals("reward1") && t.targetNode().equals("reward1")
				&& t.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == 203087
				&& talk.dialogId() == -1)
			.findFirst().orElseThrow();
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(1011)), warriorDialog.afterCommit());

		List<QuestAction> warriorComplete = transitions.stream()
			.filter(t -> t.sourceNode().equals("reward1") && t.targetNode().equals("complete"))
			.flatMap(t -> t.actions().stream()).toList();
		assertTrue(warriorComplete.contains(new QuestAction.GrantReward("EXP", 0, 275, QuestRewardAmountMode.QUEST_BASE)));
		assertTrue(warriorComplete.contains(new QuestAction.CompleteQuest(0)));

		Set<String> completionSources = transitions.stream()
			.filter(t -> t.targetNode().equals("complete"))
			.peek(t -> assertTrue(t.afterCommit().contains(new AfterCommitAction.ShowQuestSelectionDialog(10)),
				t.sourceNode() + " must leave the reward confirmation dialog after completion"))
			.map(QuestTransition::sourceNode)
			.collect(Collectors.toSet());
		assertEquals(Set.of("reward1", "reward2", "reward3", "reward4", "reward5", "reward6"), completionSources);
	}

	private static QuestTransition startRoute(List<QuestTransition> transitions, int rewardNode) {
		return transitions.stream().filter(t -> t.sourceNode().equals("unaccepted")
				&& t.event().equals(new QuestEvent.LevelUp())
				&& t.targetNode().equals("reward" + rewardNode))
			.findFirst().orElseThrow();
	}

	private static QuestTransition gatedReward(List<QuestTransition> transitions, int dialog, int itemId,
			String target) {
		return transitions.stream().filter(t -> t.sourceNode().equals("started") && t.targetNode().equals(target))
			.filter(t -> t.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 790001 && talk.dialogId() == dialog)
			.filter(t -> t.conditions().contains(new QuestCondition.HasItem(itemId, 1)))
			.findFirst().orElse(null);
	}

	private static List<QuestAction> completions(List<QuestTransition> transitions, String source) {
		return transitions.stream().filter(t -> t.sourceNode().equals(source) && t.targetNode().equals("complete"))
			.flatMap(t -> t.actions().stream()).toList();
	}

	private static Map<String, Integer> varsOf(CompiledQuestDefinition compiled, String label) {
		return compiled.definition().nodes().stream().filter(n -> n.label().equals(label))
			.findFirst().orElseThrow().projection().variables();
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
