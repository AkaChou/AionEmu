package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
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
 * 验证任务 16802 的进入世界自动接取、双计数击杀完成、客户端实时领奖入口与上线自愈合同。
 * Verifies quest 16802 area acquisition, both kill counters, the client real-time reward entry,
 * and the legacy k1/k2 stage login self-heal.
 */
class Quest16802ClientDialogAlignmentTest {
	private static final int QUEST_ID = 16802;
	private static final int DIALOG_NPC_ID = 806148;
	private static final int ARCHIVES_WORLD_ID = 301540000;
	private static final Set<Integer> LIBRARIANS = Set.of(
		220306, 220309, 220312, 220315, 220318, 220324, 220327, 220330);
	private static final Set<Integer> SUB_BOSSES = Set.of(
		857450, 857452, 857454, 857456, 857458, 857459);

	@Test
	void keepsAreaAcquisitionSeparateFromTheRewardDialog() throws Exception {
		QuestDefinition definition = definition().definition();
		assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of("var0", 0, "var1", 0, "var2", 0));
		assertNode(definition, "started", QuestStatus.START, Map.of());
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 1, "var1", 30, "var2", 2));
		assertNode(definition, "complete", QuestStatus.COMPLETE, Map.of("var0", 0, "var1", 0, "var2", 0));

		QuestTransition areaEntry = transition(definition, "unaccepted", "started", new QuestEvent.EnterWorld());
		assertEquals(List.of(
			new QuestCondition.WorldIs(ARCHIVES_WORLD_ID, true),
			new QuestCondition.StartEligible(),
			new QuestCondition.QuestsFinished(Set.of(16801))), areaEntry.conditions());
		assertEquals(List.of(), areaEntry.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH)),
			areaEntry.afterCommit());

		// 汇报页只在领奖状态下发；进行中状态不得提前下发报告页。
		// The report page is reserved for the reward status; START must never emit it.
		assertFalse(hasTalk(definition, "started", QuestDialogAction.QUEST_SELECT.id()));

		// START 只保留“满计数旧存档”的带门禁恢复路线（形状见 finalKillInEitherCounterEntersReward 与
		// incompleteProgressCannotReachRewardOrTheRewardWindow）；无门禁的提前领奖旁路必须保持删除。
		// START keeps only the guarded full-count recovery route; the unguarded early-reward bypass stays removed.
		List<QuestTransition> startRecoveries = definition.transitions().stream()
			.filter(route -> "started".equals(route.sourceNode()))
			.filter(route -> route.event().equals(new QuestEvent.TalkToNpc(DIALOG_NPC_ID,
				QuestDialogAction.SELECT_QUEST_REWARD.id())))
			.toList();
		assertEquals(1, startRecoveries.size());
		assertFalse(startRecoveries.getFirst().conditions().isEmpty(),
			"the START reward-window route must stay guarded by the full kill counters");

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
	void finalKillInEitherCounterEntersRewardBeforeReporting() throws Exception {
		QuestDefinition definition = definition().definition();

		assertCounter(definition, LIBRARIANS, "var1", 30);
		assertCounter(definition, SUB_BOSSES, "var2", 2);

		QuestTransition librarianCompletion = killRoute(definition, "started", "reward", LIBRARIANS, 0);
		assertEquals(List.of(
			new QuestCondition.QuestVariableIs("var0", 0),
			new QuestCondition.VariableAtLeast("var1", 29),
			new QuestCondition.VariableAtLeast("var2", 2)), librarianCompletion.conditions());
		assertEquals(List.of(
			new QuestAction.SetVariable("var1", 30),
			new QuestAction.SetVariable("var0", 1)), librarianCompletion.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), librarianCompletion.afterCommit());

		QuestTransition bossCompletion = killRoute(definition, "started", "reward", SUB_BOSSES, 0);
		assertEquals(List.of(
			new QuestCondition.QuestVariableIs("var0", 0),
			new QuestCondition.VariableAtLeast("var1", 30),
			new QuestCondition.VariableAtLeast("var2", 1)), bossCompletion.conditions());
		assertEquals(List.of(
			new QuestAction.SetVariable("var2", 2),
			new QuestAction.SetVariable("var0", 1)), bossCompletion.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), bossCompletion.afterCommit());

		// 满计数旧存档仍可在报告 NPC 处补进领奖并打开奖励窗。
		// Persisted full-count saves may still report at the reward NPC and open the reward window.
		QuestTransition recoveredReport = talk(definition, "started", "reward",
			QuestDialogAction.SELECT_QUEST_REWARD.id());
		assertEquals(List.of(
			new QuestCondition.VariableAtLeast("var1", 30),
			new QuestCondition.VariableAtLeast("var2", 2)), recoveredReport.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 1)), recoveredReport.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			recoveredReport.afterCommit());
	}

	@Test
	void legacyK1K2StageFlagsAreRepairedOnEnterWorld() throws Exception {
		QuestDefinition definition = definition().definition();

		QuestTransition promote = selfHeal(definition, "reward");
		assertEquals(Integer.valueOf(0), promote.priority());
		assertEquals(List.of(
			new QuestCondition.StatusIs(QuestStatus.START),
			new QuestCondition.VariableAtLeast("var0", 1),
			new QuestCondition.VariableAtLeast("var1", 30),
			new QuestCondition.VariableAtLeast("var2", 2)), promote.conditions());
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

		// 旧 k2(var0=2、两组计数已满)登录时直接补进领奖；未满的旧 k1 回到计数阶段。
		// A legacy k2 save (var0=2, both counters full) enters reward on login, while an
		// incomplete legacy k1 save returns to the counting stage.
		assertSelfHealPlan(definition(), "reward", Map.of("var0", 2, "var1", 30, "var2", 2),
			QuestStatus.REWARD, Map.of("var0", 1, "var1", 30, "var2", 2));
		assertSelfHealPlan(definition(), "started", Map.of("var0", 1, "var1", 12, "var2", 0),
			QuestStatus.START, Map.of("var0", 0, "var1", 12, "var2", 0));
		assertTrue(QuestMutationPlanner.plan(definition(), snapshot(definition, Map.of("var0", 1, "var1", 12, "var2", 0)),
			new QuestEvent.EnterWorld(), promote).isEmpty(),
			"an incomplete save must not be promoted to reward");
	}

	@Test
	void incompleteProgressCannotReachRewardOrTheRewardWindow() throws Exception {
		CompiledQuestDefinition compiled = definition();
		QuestDefinition definition = compiled.definition();
		for (Map<String, Integer> variables : List.of(
			Map.of("var0", 0, "var1", 0, "var2", 0),
			Map.of("var0", 0, "var1", 29, "var2", 2),
			Map.of("var0", 0, "var1", 30, "var2", 1),
			Map.of("var0", 0, "var1", 30, "var2", 0),
			Map.of("var0", 1, "var1", 30, "var2", 0),
			Map.of("var0", 2, "var1", 29, "var2", 1))) {
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

	private static void assertCounter(QuestDefinition definition, Set<Integer> npcIds, String field,
			int required) {
		QuestTransition continuing = killRoute(definition, "started", "started", npcIds, 2);
		assertEquals(List.of(
			new QuestCondition.QuestVariableIs("var0", 0),
			new QuestCondition.VariableBelow(field, required)), continuing.conditions());
		assertEquals(List.of(new QuestAction.IncrementVariable(field, 1)), continuing.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			continuing.afterCommit());

		QuestTransition cap = killRoute(definition, "started", "started", npcIds, 1);
		assertEquals(List.of(
			new QuestCondition.QuestVariableIs("var0", 0),
			new QuestCondition.VariableAtLeast(field, required - 1)), cap.conditions());
		assertEquals(List.of(new QuestAction.SetVariable(field, required)), cap.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			cap.afterCommit());
	}

	private static void assertSelfHealPlan(CompiledQuestDefinition compiled, String target,
			Map<String, Integer> variables, QuestStatus expectedStatus, Map<String, Integer> expected) {
		QuestDefinition definition = compiled.definition();
		QuestMutationPlan plan = QuestMutationPlanner.plan(compiled, snapshot(definition, variables),
			new QuestEvent.EnterWorld(), selfHeal(definition, target)).orElseThrow(() ->
				new IllegalStateException("quest " + compiled.id() + " self-heal " + target
					+ " did not plan at " + variables));
		assertEquals(expectedStatus, plan.nextStatus());
		assertEquals(expected, definition.progressLayout().unpack(plan.nextPackedVariables()));
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

	private static QuestTransition killRoute(QuestDefinition definition, String source, String target,
			Set<Integer> npcIds, int priority) {
		return definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> target.equals(candidate.targetNode()))
			.filter(candidate -> candidate.event().equals(new QuestEvent.KillNpcSet(npcIds)))
			.filter(candidate -> Integer.valueOf(priority).equals(candidate.priority()))
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
