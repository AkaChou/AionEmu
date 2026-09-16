package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证任务 19637 的阶段变量与击杀计数字段隔离、第 10 击直接进入 REWARD，以及客户端对话报告合同。
 * Verifies that quest 19637 isolates its step variable from the kill counter, enters REWARD on the 10th kill,
 * and maintains the Aion 5.8 client dialog/report contract.
 */
class Quest19637ClientDialogAlignmentTest {
	private static final int QUEST_ID = 19637;
	private static final int CAINUS_NPC_ID = 798926;
	private static final Set<Integer> TARGET_MOBS = Set.of(215500, 215501, 215502, 215503);

	@Test
	void restoresClientStepAndKillCounterSeparationAndDialogContract() throws Exception {
		CompiledQuestDefinition compiled = load();
		QuestDefinition definition = compiled.definition();

		assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of("var0", 0, "var1", 0));
		assertNode(definition, "started", QuestStatus.START, Map.of());
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 1, "var1", 10));
		assertNode(definition, "complete", QuestStatus.COMPLETE, Map.of("var0", 0, "var1", 0));

		// 接取对话：从 unaccepted 状态收到 QUEST_SELECT(31)，下发 4762 (SELECT_NONE)
		QuestTransition startDialog = talk(definition, "unaccepted", "unaccepted", QuestDialogAction.QUEST_SELECT.id());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT_NONE.id())),
			startDialog.afterCommit());

		// 接受任务：QUEST_ACCEPT_SIMPLE(20000)，进入 started
		QuestTransition acceptSimple = talk(definition, "unaccepted", "started", QuestDialogAction.QUEST_ACCEPT_SIMPLE.id());
		assertTrue(acceptSimple.conditions().stream().anyMatch(QuestCondition.StartEligible.class::isInstance));
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()), acceptSimple.afterCommit());

		// 未完成杀怪时，started 状态不暴露 started -> started 的 QUEST_SELECT 对话（绝不向玩家弹 DEFAULT_SUCCESS）
		assertFalse(hasTalk(definition, "started", "started", QuestDialogAction.QUEST_SELECT.id()));

		// 满计数恢复路由：started -> reward 仅在 var1 >= 10 时允许
		QuestTransition recoveredSelect = talk(definition, "started", "reward", QuestDialogAction.QUEST_SELECT.id());
		assertEquals(List.of(new QuestCondition.VariableAtLeast("var1", 10)), recoveredSelect.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 1)), recoveredSelect.actions());

		// 领奖阶段：在 reward 节点收到 QUEST_SELECT(31)，下发 DEFAULT_SUCCESS(10002)
		QuestTransition reportSuccess = talk(definition, "reward", "reward", QuestDialogAction.QUEST_SELECT.id());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			reportSuccess.afterCommit());

		// 领奖预览：在 reward 节点收到 SELECT_QUEST_REWARD(1009)，下发 SHOW_SELECT_QUEST_REWARD_WINDOW1(5)
		QuestTransition previewReward = talk(definition, "reward", "reward", QuestDialogAction.SELECT_QUEST_REWARD.id());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), previewReward.afterCommit());

		// 奖励选择：SELECTED_QUEST_REWARD1..6 均指向 complete 节点
		for (int rewardIndex = 1; rewardIndex <= 6; rewardIndex++) {
			int actionId = QuestDialogAction.SELECTED_QUEST_REWARD1.id() + rewardIndex - 1;
			QuestTransition choice = talk(definition, "reward", "complete", actionId);
			assertTrue(choice.actions().stream().anyMatch(QuestAction.CompleteQuest.class::isInstance));
		}
	}

	@Test
	void killChainAdvancesVar1UpToTenAndEntersRewardOnTenthKill() throws Exception {
		CompiledQuestDefinition compiled = load();
		QuestDefinition definition = compiled.definition();
		ProgressLayout layout = definition.progressLayout();

		QuestEvent killSetEvent = new QuestEvent.KillNpcSet(TARGET_MOBS);
		QuestEvent killSingleEvent = new QuestEvent.KillNpc(215502);

		// 1. 正常递增：从 var0=0, var1=0 开始杀怪，产生 nextStatus=START, var0=0, var1=1
		QuestSnapshot initialSnapshot = new QuestSnapshot(1, QUEST_ID, QuestStatus.START,
			layout.pack(Map.of("var0", 0, "var1", 0)), Map.of());
		QuestTransition continuing = transition(definition, "started", "started", killSetEvent, 1);
		QuestMutationPlan plan1 = QuestMutationPlanner.plan(compiled, initialSnapshot, killSingleEvent, continuing).orElseThrow();
		assertEquals(QuestStatus.START, plan1.nextStatus());
		assertEquals(Map.of("var0", 0, "var1", 1), layout.unpack(plan1.nextPackedVariables()));
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)), continuing.afterCommit());

		// 2. 老脏数据自愈：玩家之前在 var0=1, var1=0，击杀一只怪后，var0 被修正为 0，var1 为 1
		QuestSnapshot dirtySnapshot = new QuestSnapshot(1, QUEST_ID, QuestStatus.START,
			layout.pack(Map.of("var0", 1, "var1", 0)), Map.of());
		QuestMutationPlan dirtyHealPlan = QuestMutationPlanner.plan(compiled, dirtySnapshot, killSingleEvent, continuing).orElseThrow();
		assertEquals(QuestStatus.START, dirtyHealPlan.nextStatus());
		assertEquals(Map.of("var0", 0, "var1", 1), layout.unpack(dirtyHealPlan.nextPackedVariables()));

		// 3. 第 10 次击杀：在 var0=0, var1=9 时击杀第 10 只，命中 priority 0 路线直接进入 REWARD
		QuestSnapshot ninthSnapshot = new QuestSnapshot(1, QUEST_ID, QuestStatus.START,
			layout.pack(Map.of("var0", 0, "var1", 9)), Map.of());
		QuestTransition completing = transition(definition, "started", "reward", killSetEvent, 0);
		QuestMutationPlan finalPlan = QuestMutationPlanner.plan(compiled, ninthSnapshot, killSingleEvent, completing).orElseThrow();
		assertEquals(QuestStatus.REWARD, finalPlan.nextStatus());
		assertEquals(Map.of("var0", 1, "var1", 10), layout.unpack(finalPlan.nextPackedVariables()));
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)),
			completing.afterCommit());

		// 4. 满计数恢复路由：若处于 started 且 var1>=10，允许通过与 798926 对话转移至 REWARD
		QuestTransition recoveredReport = talk(definition, "started", "reward", QuestDialogAction.QUEST_SELECT.id());
		assertEquals(List.of(new QuestCondition.VariableAtLeast("var1", 10)), recoveredReport.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 1)), recoveredReport.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())), recoveredReport.afterCommit());
	}

	private static boolean hasTalk(QuestDefinition definition, String source, String target, int action) {
		return definition.transitions().stream()
			.anyMatch(candidate -> candidate.sourceNode().equals(source)
				&& candidate.targetNode().equals(target)
				&& candidate.event().equals(new QuestEvent.TalkToNpc(CAINUS_NPC_ID, action)));
	}

	private static QuestTransition talk(QuestDefinition definition, String source, String target, int action) {
		return definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode().equals(source)
				&& candidate.targetNode().equals(target)
				&& candidate.event().equals(new QuestEvent.TalkToNpc(CAINUS_NPC_ID, action)))
			.findFirst().orElseThrow();
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event, Integer priority) {
		return definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode().equals(source)
				&& candidate.targetNode().equals(target)
				&& candidate.event().equals(event)
				&& Objects.equals(candidate.priority(), priority))
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

	private static CompiledQuestDefinition load() throws Exception {
		try (InputStream input = Quest19637ClientDialogAlignmentTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + QUEST_ID + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + QUEST_ID + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
