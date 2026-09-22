package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证任务 2392 的分支交付、奖励预览与最终交任务合同：
 * 选定分支羽毛在 started->rN 阶段已交出并扣除，进入 REWARD 后三类羽毛整叠移除保持可选，
 * 缺物时绝不能因严格正数扣除阻断奖励窗口预览或最终交任务。
 * Verifies quest 2392's branch turn-in, reward preview, and final turn-in contract:
 * the selected branch feather is already turned in and removed during started->rN;
 * in REWARD status, removing the three feather types as whole stacks remains optional,
 * so absence of items must never block reward preview or final turn-in.
 */
class Quest2392BeautifulFeatherTest {
	private static final int PLAYER_ID = 7;
	private static final int QUEST_ID = 2392;
	private static final int NPC_ID = 798085;
	private static final int FEATHER_VIRAGO = 182204159;
	private static final int FEATHER_CRESTLICH = 182204160;
	private static final int FEATHER_PECKU = 182204161;
	private static final int REWARD_ACTION_FIRST = QuestDialogAction.SELECTED_QUEST_REWARD1.id();
	private static final int REWARD_ACTION_LAST = QuestDialogAction.SELECTED_QUEST_NOREWARD.id();

	private record BranchInfo(
		String nodeName,
		int varValue,
		int featherId,
		QuestDialogAction setproAction,
		int rewardIndex,
		int rewardMedicineCount,
		QuestDialogPage rewardPage
	) {}

	private static final List<BranchInfo> BRANCHES = List.of(
		new BranchInfo("r1", 1, FEATHER_VIRAGO, QuestDialogAction.SETPRO1, 0, 8,
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1),
		new BranchInfo("r2", 2, FEATHER_CRESTLICH, QuestDialogAction.SETPRO2, 1, 4,
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW2),
		new BranchInfo("r3", 3, FEATHER_PECKU, QuestDialogAction.SETPRO3, 2, 4,
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW3));

	@Test
	void branchSelectionConsumesChosenFeatherAndAdvancesToReward() throws Exception {
		CompiledQuestDefinition definition = load();
		for (BranchInfo branch : BRANCHES) {
			QuestTransition branchRoute = branchSelectionRoute(definition, branch.setproAction());
			assertEquals("started", branchRoute.sourceNode());
			assertEquals(branch.nodeName(), branchRoute.targetNode());
			assertEquals(0, branchRoute.priority());
			assertEquals(List.of(new QuestCondition.HasItem(branch.featherId(), 1)), branchRoute.conditions());
			assertEquals(List.of(new QuestAction.RemoveItem(branch.featherId(), 1)), branchRoute.actions());
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(branch.rewardPage().id())), branchRoute.afterCommit());

			int packedStarted = definition.definition().progressLayout().pack(Map.of("var0", 0));
			QuestSnapshot snapshot = new QuestSnapshot(PLAYER_ID, QUEST_ID, QuestStatus.START, packedStarted,
				Map.of(branch.featherId(), 1));
			QuestEvent.TalkToNpc event = (QuestEvent.TalkToNpc) branchRoute.event();
			QuestMutationPlan plan = QuestMutationPlanner.plan(definition, snapshot, event, branchRoute)
				.orElseThrow(() -> new AssertionError("missing branch plan for " + branch.nodeName()));

			assertEquals(QuestStatus.REWARD, plan.nextStatus());
			assertEquals(definition.definition().progressLayout().pack(Map.of("var0", branch.varValue())),
				plan.nextPackedVariables());
			assertEquals(List.of(new QuestAction.RemoveItem(branch.featherId(), 1)), plan.requiredActions());
		}
	}

	@Test
	void rewardTurnInCompletesWhenBranchFeathersAreAlreadyConsumed() throws Exception {
		CompiledQuestDefinition definition = load();
		for (BranchInfo branch : BRANCHES) {
			QuestNode sourceNode = node(definition, branch.nodeName());
			assertEquals(QuestStatus.REWARD, sourceNode.projection().status());
			assertEquals(Map.of("var0", branch.varValue()), sourceNode.projection().variables());

			for (int action = REWARD_ACTION_FIRST; action <= REWARD_ACTION_LAST; action++) {
				int dialogAction = action;
				QuestTransition turnInRoute = turnInRoute(definition, branch.nodeName(), action);
				assertEquals("complete", turnInRoute.targetNode());
				assertEquals(expectedTurnInActions(branch.rewardIndex(), branch.rewardMedicineCount()),
					turnInRoute.actions());
				assertEquals(expectedTurnInAfterCommit(), turnInRoute.afterCommit());

				// 核心回归点：空背包（羽毛在进入 REWARD 时已消耗）必须顺利规划完成
				int packedReward = definition.definition().progressLayout().pack(Map.of("var0", branch.varValue()));
				QuestSnapshot snapshot = new QuestSnapshot(PLAYER_ID, QUEST_ID, QuestStatus.REWARD, packedReward,
					Map.of());
				QuestEvent.TalkToNpc event = (QuestEvent.TalkToNpc) turnInRoute.event();
				QuestMutationPlan plan = QuestMutationPlanner.plan(definition, snapshot, event, turnInRoute)
					.orElseThrow(() -> new AssertionError("final turn-in blocked for " + branch.nodeName()
						+ " action " + dialogAction));

				assertEquals(QuestStatus.COMPLETE, plan.nextStatus());
				assertEquals(expectedTurnInActions(branch.rewardIndex(), branch.rewardMedicineCount()),
					plan.requiredActions());
				assertEquals(expectedTurnInAfterCommit(), plan.afterCommit());
			}
		}
	}

	@Test
	void rewardTurnInCleansUpExtraFeathersWhenPresent() throws Exception {
		CompiledQuestDefinition definition = load();
		BranchInfo branch = BRANCHES.getFirst();
		QuestTransition turnInRoute = turnInRoute(definition, branch.nodeName(), REWARD_ACTION_LAST);

		int packedReward = definition.definition().progressLayout().pack(Map.of("var0", branch.varValue()));
		Map<Integer, Integer> carryingMultiple = Map.of(
			FEATHER_VIRAGO, 3,
			FEATHER_CRESTLICH, 2,
			FEATHER_PECKU, 1);
		QuestSnapshot snapshot = new QuestSnapshot(PLAYER_ID, QUEST_ID, QuestStatus.REWARD, packedReward,
			carryingMultiple);
		QuestEvent.TalkToNpc event = (QuestEvent.TalkToNpc) turnInRoute.event();
		QuestMutationPlan plan = QuestMutationPlanner.plan(definition, snapshot, event, turnInRoute)
			.orElseThrow(() -> new AssertionError("final turn-in with items blocked for " + branch.nodeName()));

		assertEquals(QuestStatus.COMPLETE, plan.nextStatus());
		assertEquals(expectedTurnInActions(branch.rewardIndex(), branch.rewardMedicineCount()),
			plan.requiredActions());
	}

	@Test
	void rewardPreviewOpensWindowWithoutBlockingForEmptyOrCarriedInventory() throws Exception {
		CompiledQuestDefinition definition = load();
		for (BranchInfo branch : BRANCHES) {
			for (QuestDialogAction previewAction : List.of(QuestDialogAction.USE_OBJECT,
					QuestDialogAction.SELECT_QUEST_REWARD)) {
				QuestTransition previewRoute = previewRoute(definition, branch.nodeName(), previewAction.id());
				assertEquals(branch.nodeName(), previewRoute.targetNode());
				assertEquals(expectedPreviewActions(), previewRoute.actions());
				assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(branch.rewardPage().id())),
					previewRoute.afterCommit());

				int packedReward = definition.definition().progressLayout().pack(Map.of("var0", branch.varValue()));
				QuestEvent.TalkToNpc event = (QuestEvent.TalkToNpc) previewRoute.event();

				// 1. 空背包预览不被阻断
				QuestSnapshot emptySnapshot = new QuestSnapshot(PLAYER_ID, QUEST_ID, QuestStatus.REWARD, packedReward,
					Map.of());
				assertTrue(QuestMutationPlanner.plan(definition, emptySnapshot, event, previewRoute).isPresent(),
					"preview blocked on empty inventory for " + branch.nodeName() + " " + previewAction);

				// 2. 持有残留物时清理整叠
				QuestSnapshot carrySnapshot = new QuestSnapshot(PLAYER_ID, QUEST_ID, QuestStatus.REWARD, packedReward,
					Map.of(FEATHER_VIRAGO, 2, FEATHER_CRESTLICH, 1));
				QuestMutationPlan carryPlan = QuestMutationPlanner.plan(definition, carrySnapshot, event, previewRoute)
					.orElseThrow(() -> new AssertionError("preview blocked with items for " + branch.nodeName()));
				assertEquals(expectedPreviewActions(), carryPlan.requiredActions());
			}
		}
	}

	private static List<QuestAction> expectedPreviewActions() {
		return List.of(
			new QuestAction.RemoveItem(FEATHER_VIRAGO, QuestAction.RemoveItem.ALL),
			new QuestAction.RemoveItem(FEATHER_CRESTLICH, QuestAction.RemoveItem.ALL),
			new QuestAction.RemoveItem(FEATHER_PECKU, QuestAction.RemoveItem.ALL));
	}

	private static List<QuestAction> expectedTurnInActions(int rewardIndex, int medicineCount) {
		return List.of(
			new QuestAction.RemoveItem(FEATHER_VIRAGO, QuestAction.RemoveItem.ALL),
			new QuestAction.RemoveItem(FEATHER_CRESTLICH, QuestAction.RemoveItem.ALL),
			new QuestAction.RemoveItem(FEATHER_PECKU, QuestAction.RemoveItem.ALL),
			new QuestAction.GrantReward("EXP", 0, 1244918, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("ITEM", 162000048, medicineCount),
			new QuestAction.GrantReward("ITEM", 186000008, 2),
			new QuestAction.CompleteQuest(rewardIndex));
	}

	private static List<AfterCommitAction> expectedTurnInAfterCommit() {
		return List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id()));
	}

	private static QuestTransition branchSelectionRoute(CompiledQuestDefinition definition, QuestDialogAction action) {
		return definition.definition().transitions().stream()
			.filter(t -> "started".equals(t.sourceNode()) && t.event().equals(new QuestEvent.TalkToNpc(NPC_ID, action.id())) && t.priority() == 0)
			.findFirst().orElseThrow(() -> new AssertionError("missing branch selection route for " + action));
	}

	private static QuestTransition turnInRoute(CompiledQuestDefinition definition, String source, int action) {
		return definition.definition().transitions().stream()
			.filter(t -> source.equals(t.sourceNode()) && "complete".equals(t.targetNode()) && QuestEvent.matches(t.event(), new QuestEvent.TalkToNpc(NPC_ID, action)))
			.findFirst().orElseThrow(() -> new AssertionError("missing turn-in route for " + source + " " + action));
	}

	private static QuestTransition previewRoute(CompiledQuestDefinition definition, String source, int action) {
		return definition.definition().transitions().stream()
			.filter(t -> source.equals(t.sourceNode()) && source.equals(t.targetNode()) && QuestEvent.matches(t.event(), new QuestEvent.TalkToNpc(NPC_ID, action)))
			.findFirst().orElseThrow(() -> new AssertionError("missing preview route for " + source + " " + action));
	}

	private static QuestNode node(CompiledQuestDefinition definition, String label) {
		return definition.definition().nodes().stream()
			.filter(n -> label.equals(n.label())).findFirst().orElseThrow();
	}

	private static CompiledQuestDefinition load() throws Exception {
		Path path = Path.of("src/main/resources/aion/data/static_data/quest_definition/quests/2392.xml");
		try (InputStream input = Files.newInputStream(path)) {
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}