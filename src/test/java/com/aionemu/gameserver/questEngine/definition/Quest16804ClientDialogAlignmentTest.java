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

/**
 * 验证任务 16804 的进入世界自动接取、首领击杀进入领奖与上线自愈合同。
 * Verifies quest 16804 area acquisition, boss-kill reward entry, and the legacy k1 stage
 * login self-heal.
 */
class Quest16804ClientDialogAlignmentTest {
	private static final int QUEST_ID = 16804;
	private static final int DIALOG_NPC_ID = 806148;
	private static final int ARCHIVES_WORLD_ID = 301540000;
	private static final Set<Integer> ZONE_BOSSES = Set.of(857460, 857462, 857464);

	@Test
	void keepsAreaAcquisitionSeparateFromTheRewardDialog() throws Exception {
		QuestDefinition definition = definition().definition();
		assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of("var0", 0));
		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 1));
		assertNode(definition, "complete", QuestStatus.COMPLETE, Map.of("var0", 0));

		QuestTransition areaEntry = transition(definition, "unaccepted", "started", new QuestEvent.EnterWorld());
		assertEquals(List.of(
			new QuestCondition.WorldIs(ARCHIVES_WORLD_ID, true),
			new QuestCondition.StartEligible(),
			new QuestCondition.QuestsFinished(Set.of(16803))), areaEntry.conditions());
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
	void bossKillEntersRewardBeforeTheNpcReport() throws Exception {
		QuestTransition completion = transition(definition().definition(), "started", "reward",
			new QuestEvent.KillNpcSet(ZONE_BOSSES));

		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 0)), completion.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 1)), completion.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), completion.afterCommit());
	}

	@Test
	void legacyK1StageFlagIsRepairedOnEnterWorld() throws Exception {
		QuestDefinition definition = definition().definition();

		QuestTransition promote = selfHeal(definition);
		assertEquals(Integer.valueOf(0), promote.priority());
		assertEquals(List.of(
			new QuestCondition.StatusIs(QuestStatus.START),
			new QuestCondition.VariableAtLeast("var0", 1)), promote.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 1)), promote.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), promote.afterCommit());

		QuestSnapshot restart = new QuestSnapshot(7, QUEST_ID, QuestStatus.START,
			definition.progressLayout().pack(Map.of("var0", 1)), Map.of());
		assertEquals(QuestStatus.REWARD, QuestMutationPlanner.plan(definition(), restart,
			new QuestEvent.EnterWorld(), promote).orElseThrow().nextStatus());

		QuestSnapshot counting = new QuestSnapshot(7, QUEST_ID, QuestStatus.START,
			definition.progressLayout().pack(Map.of("var0", 0)), Map.of());
		assertFalse(QuestMutationPlanner.plan(definition(), counting,
			new QuestEvent.EnterWorld(), promote).isPresent(),
			"a counting save must not be promoted to reward");
	}

	@Test
	void incompleteProgressCannotReachRewardOrTheRewardWindow() throws Exception {
		CompiledQuestDefinition compiled = definition();
		QuestDefinition definition = compiled.definition();
		QuestSnapshot snapshot = new QuestSnapshot(7, QUEST_ID, QuestStatus.START,
			definition.progressLayout().pack(Map.of("var0", 0)), Map.of());
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
				assertFalse(rewards, () -> "quest " + QUEST_ID + " premature reward via " + route);
			});
		}
	}

	private static boolean hasTalk(QuestDefinition definition, String source, int action) {
		return definition.transitions().stream()
			.anyMatch(candidate -> source.equals(candidate.sourceNode())
				&& candidate.event().equals(new QuestEvent.TalkToNpc(DIALOG_NPC_ID, action)));
	}

	private static QuestTransition talk(QuestDefinition definition, String source, String target, int action) {
		return transition(definition, source, target, new QuestEvent.TalkToNpc(DIALOG_NPC_ID, action));
	}

	private static QuestTransition selfHeal(QuestDefinition definition) {
		return definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode() == null)
			.filter(candidate -> candidate.targetNode().equals("reward"))
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
