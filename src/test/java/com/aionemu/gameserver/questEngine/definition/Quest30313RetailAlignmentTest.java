package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Quest30313RetailAlignmentTest {
	@Test
	void workAndCollectedItemsAreTransactionalAcrossEveryNpcPath() throws Exception {
		Path path = Path.of("src/main/resources/aion/data/static_data/quest_definition/quests/30313.xml");
		try (InputStream input = Files.newInputStream(path)) {
			QuestDefinition definition = QuestDefinitionXmlCompiler.compile(input).definition();
			assertEquals(List.of(new QuestItemRequirement(182209716, 1)),
				definition.metadata().questWorkItems());

			// 旧 handler 只在 799322 接取：唯一的 NPC_START 提供 accept/simple 两条接取路由。
			// The legacy handler only starts at 799322: the single NPC_START provides the two accept routes.
			List<QuestTransition> starts = transitions(definition, "unaccepted", "started");
			assertEquals(2, starts.size());
			assertTrue(starts.stream().allMatch(transition ->
				transition.actions().contains(new QuestAction.GiveItem(182209716, 1))));

			List<QuestTransition> turnIns = transitions(definition, "started", "reward");
			assertEquals(2, turnIns.size());
			assertTrue(turnIns.stream().allMatch(transition ->
				transition.conditions().contains(new QuestCondition.HasItem(182209717, 1))
					&& transition.actions().contains(new QuestAction.RemoveItem(182209717, 1))));

			List<QuestTransition> completions = transitions(definition, "reward", "complete");
			assertEquals(4, completions.size());
			assertTrue(completions.stream().allMatch(transition -> transition.actions()
				.contains(new QuestAction.RemoveItem(182209716, QuestAction.RemoveItem.ALL))));
		}
	}

	@Test
	void disabledPrismInteractionStaysUnroutedAndRewardClaimBelongsTo799225() throws Exception {
		Path path = Path.of("src/main/resources/aion/data/static_data/quest_definition/quests/30313.xml");
		try (InputStream input = Files.newInputStream(path)) {
			QuestDefinition definition = QuestDefinitionXmlCompiler.compile(input).definition();
			// 旧 handler 将 730275 的交互整体注释禁用：不得存在任何 730275 路由或接取入口。
			// The legacy handler comments out the whole 730275 interaction: no 730275 route or
			// start entry may exist.
			assertTrue(definition.transitions().stream().noneMatch(transition ->
				transition.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == 730275),
				"730275 interaction is disabled without retail evidence");

			// 799225 只承担领奖：不接取任务（无 unaccepted 入口），REWARD 态打开本任务入口页或领奖窗口。
			// 799225 only claims the reward: it never starts the quest; in REWARD it opens either this
			// quest's own entry page or the reward window.
			assertTrue(definition.transitions().stream().noneMatch(transition ->
				transition.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == 799225
					&& "unaccepted".equals(transition.sourceNode())),
				"799225 must not start the quest");
			List<QuestTransition> rewardStateRoutes = transitions(definition, "reward", "reward");
			assertTrue(rewardStateRoutes.stream().allMatch(transition ->
				transition.event() instanceof QuestEvent.TalkToNpc talk
					&& (talk.npcId() == 799225 || talk.npcId() == 799322)),
				"reward-state routes belong to 799322/799225");
			// 领奖窗口预览仍必须下发第 1 档奖励窗口 5；新增的 QUEST_SELECT(31) 入口页只负责下发
			// select_success(10002)，不得改写奖励窗口合同。
			// Reward-window previews must still emit tier-1 window 5; the added QUEST_SELECT(31) entry page
			// only emits select_success(10002) and must not rewrite the reward window contract.
			assertTrue(rewardStateRoutes.stream().anyMatch(transition ->
				transition.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == 799225
					&& transition.afterCommit().equals(
						List.of(new AfterCommitAction.ShowQuestDialog(5)))),
				"799225 keeps its reward window preview");
			assertTrue(rewardStateRoutes.stream()
				.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.dialogId() != null && talk.dialogId() == QuestDialogAction.QUEST_SELECT.id())
				.allMatch(transition -> transition.afterCommit().equals(
					List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())))),
				"reward-state entry pages emit the quest's own select_success page");
		}
	}

	private static List<QuestTransition> transitions(QuestDefinition definition, String source, String target) {
		return definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()))
			.filter(transition -> target.equals(transition.targetNode()))
			.toList();
	}
}
