package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证任务 16803 的进入世界自动接取、30 杀收口进入领奖与上线自愈合同。
 * Verifies quest 16803 area acquisition, the 30th-kill reward entry, and the legacy k1 stage
 * login self-heal.
 */
class Quest16803ClientDialogAlignmentTest {
	private static final int QUEST_ID = 16803;
	private static final int DIALOG_NPC_ID = 806148;
	private static final int ARCHIVES_WORLD_ID = 301540000;
	// 击杀集合必须覆盖客户端 9 个变体：第 9 个是 Cube Artifact(220411)，与 16807/26803/26807 同族一致。
	// The kill set must cover all nine client variants; the ninth is the Cube Artifact (220411).
	private static final Set<Integer> RELIQUARIANS = Set.of(
		220307, 220310, 220313, 220316, 220319, 220325, 220328, 220331, 220411);

	@Test
	void keepsAreaAcquisitionSeparateFromTheRewardDialog() throws Exception {
		QuestDefinition definition = definition().definition();
		assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of("var0", 0, "var1", 0));
		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 1, "var1", 30));
		assertNode(definition, "complete", QuestStatus.COMPLETE, Map.of("var0", 0, "var1", 0));

		QuestTransition areaEntry = transition(definition, "unaccepted", "started", new QuestEvent.EnterWorld());
		assertEquals(List.of(
			new QuestCondition.WorldIs(ARCHIVES_WORLD_ID, true),
			new QuestCondition.StartEligible(),
			new QuestCondition.QuestsFinished(Set.of(16802))), areaEntry.conditions());
		assertEquals(List.of(), areaEntry.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH)),
			areaEntry.afterCommit());

		// 汇报页只在领奖状态下发；进行中状态不得提前下发报告页或奖励窗口。
		// The report page is reserved for the reward status.
		assertFalse(hasTalk(definition, "started", QuestDialogAction.QUEST_SELECT.id()));
		assertFalse(hasTalk(definition, "started", QuestDialogAction.SELECT_QUEST_REWARD.id()));

		QuestTransition report = talk(definition, "reward", "reward", QuestDialogAction.QUEST_SELECT.id());
		assertEquals(List.of(), report.conditions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			report.afterCommit());

		QuestTransition rewardPreview = talk(definition, "reward", "reward",
			QuestDialogAction.SELECT_QUEST_REWARD.id());
		assertEquals(List.of(), rewardPreview.conditions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), rewardPreview.afterCommit());
	}

	@Test
	void finalKillEntersRewardBeforeTheNpcReport() throws Exception {
		QuestDefinition definition = definition().definition();
		QuestEvent reliquarians = new QuestEvent.KillNpcSet(RELIQUARIANS);

		QuestTransition continuing = transition(definition, "started", "started", reliquarians);
		assertEquals(Integer.valueOf(1), continuing.priority());
		assertEquals(List.of(
			new QuestCondition.QuestVariableIs("var0", 0),
			new QuestCondition.VariableBelow("var1", 29)), continuing.conditions());
		assertEquals(List.of(new QuestAction.IncrementVariable("var1", 1)), continuing.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			continuing.afterCommit());

		QuestTransition completion = transition(definition, "started", "reward", reliquarians);
		assertEquals(Integer.valueOf(0), completion.priority());
		assertEquals(List.of(
			new QuestCondition.QuestVariableIs("var0", 0),
			new QuestCondition.VariableAtLeast("var1", 29)), completion.conditions());
		assertEquals(List.of(
			new QuestAction.SetVariable("var1", 30),
			new QuestAction.SetVariable("var0", 1)), completion.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), completion.afterCommit());
	}

	@Test
	void legacyK1StageFlagIsRepairedOnEnterWorld() throws Exception {
		QuestDefinition definition = definition().definition();

		QuestTransition promote = selfHeal(definition, "reward");
		assertEquals(Integer.valueOf(0), promote.priority());
		assertEquals(List.of(
			new QuestCondition.StatusIs(QuestStatus.START),
			new QuestCondition.VariableAtLeast("var0", 1),
			new QuestCondition.VariableAtLeast("var1", 30)), promote.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 1)), promote.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), promote.afterCommit());

		QuestTransition reset = selfHeal(definition, "started");
		assertEquals(Integer.valueOf(1), reset.priority());
		assertEquals(List.of(
			new QuestCondition.StatusIs(QuestStatus.START),
			new QuestCondition.VariableAtLeast("var0", 1)), reset.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 0)), reset.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			reset.afterCommit());

		assertTrue(QuestMutationPlanner.plan(definition(), snapshot(definition, Map.of("var0", 1, "var1", 15)),
			new QuestEvent.EnterWorld(), promote).isEmpty(),
			"an incomplete save must not be promoted to reward");
		assertEquals(QuestStatus.START, QuestMutationPlanner.plan(definition(),
			snapshot(definition, Map.of("var0", 1, "var1", 15)), new QuestEvent.EnterWorld(), reset)
			.orElseThrow().nextStatus());
	}

	@Test
	void incompleteProgressCannotReachRewardOrTheRewardWindow() throws Exception {
		CompiledQuestDefinition compiled = definition();
		QuestDefinition definition = compiled.definition();
		for (Map<String, Integer> variables : List.of(
			Map.of("var0", 0, "var1", 0),
			Map.of("var0", 0, "var1", 29),
			Map.of("var0", 1, "var1", 15))) {
			QuestSnapshot snapshot = snapshot(definition, variables);
			for (QuestTransition route : definition.transitions()) {
				if (!(route.event() instanceof QuestEvent.TalkToNpc)) {
					continue;
				}
				QuestMutationPlanner.plan(compiled, snapshot, route.event(), route).ifPresent(plan -> {
					boolean rewards = plan.nextStatus() == QuestStatus.REWARD
						|| plan.nextStatus() == QuestStatus.COMPLETE
						|| plan.requiredActions().stream().anyMatch(action ->
							action instanceof QuestAction.GrantReward
								|| action instanceof QuestAction.GrantSelectedReward
								|| action instanceof QuestAction.CompleteQuest)
						|| plan.afterCommit().contains(new AfterCommitAction.ShowQuestDialog(
							QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id()));
					assertFalse(rewards, () -> "quest " + QUEST_ID + " premature reward at " + variables
						+ " via " + route);
				});
			}
		}
	}

	private static QuestSnapshot snapshot(QuestDefinition definition, Map<String, Integer> variables) {
		return new QuestSnapshot(7, definition.id(), QuestStatus.START,
			definition.progressLayout().pack(variables), Map.of());
	}

	private static boolean hasTalk(QuestDefinition definition, String source, int action) {
		return definition.transitions().stream()
			.anyMatch(candidate -> source.equals(candidate.sourceNode())
				&& candidate.event().equals(new QuestEvent.TalkToNpc(DIALOG_NPC_ID, action)));
	}

	private static QuestTransition talk(QuestDefinition definition, String source, String target, int action) {
		return transition(definition, source, target, new QuestEvent.TalkToNpc(DIALOG_NPC_ID, action));
	}

	private static QuestTransition selfHeal(QuestDefinition definition, String target) {
		return definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode() == null)
			.filter(candidate -> candidate.targetNode().equals(target))
			.filter(candidate -> candidate.event().equals(new QuestEvent.EnterWorld()))
			.findFirst().orElseThrow();
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event) {
		return definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> target.equals(candidate.targetNode()))
			.filter(candidate -> candidate.event().equals(event))
			.findFirst().orElseThrow();
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
			Map<String, Integer> variables) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	private CompiledQuestDefinition definition() throws Exception {
		try (InputStream input = getClass().getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + QUEST_ID + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + QUEST_ID + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
