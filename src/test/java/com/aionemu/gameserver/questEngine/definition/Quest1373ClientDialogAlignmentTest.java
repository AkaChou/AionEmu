package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 锁定任务 1373 获取温泉水后的交付分支与奖励页面合同。
 * Locks quest 1373's hot-spring-water turn-in branches and reward-page contract.
 */
class Quest1373ClientDialogAlignmentTest {
	private static final int NPC_ID = 203949;
	private static final int HOT_SPRING_WATER_ID = 182201373;

	@Test
	void reportsHotSpringWaterWithRewardAndClientFailurePages() throws Exception {
		QuestDefinition definition = definition().definition();

		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
		assertNode(definition, "v2", QuestStatus.START, Map.of("var0", 2));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 3));
		assertNode(definition, "complete", QuestStatus.COMPLETE, Map.of("var0", 3));

		QuestTransition entry = transition(definition, "v2", "v2",
			new QuestEvent.TalkToNpc(NPC_ID, QuestDialogAction.QUEST_SELECT.id()));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT5.id())),
			entry.afterCommit());

		// 交付成功必须在提交后的状态同步之后打开奖励窗口。
		// A successful turn-in must open the reward window after the committed state sync.
		QuestTransition success = transition(definition, "v2", "reward",
			new QuestEvent.TalkToNpc(NPC_ID, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()));
		assertEquals(Integer.valueOf(0), success.priority());
		assertEquals(List.of(new QuestCondition.HasItem(HOT_SPRING_WATER_ID, 1)), success.conditions());
		assertEquals(List.of(new QuestAction.RemoveItem(HOT_SPRING_WATER_ID, 1)), success.actions());
		assertEquals(List.of(
			new AfterCommitAction.CancelQuestTimer(
				new QuestTimerPolicy.Identity("countdown", QuestTimerPolicy.Scope.PLAYER_QUEST)),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			success.afterCommit());

		// 未满足物品条件时回落到客户端实际存在的 SELECT6(2716) 页面。
		// When the item condition is not met, fall back to the client-owned SELECT6(2716) page.
		QuestTransition failure = transition(definition, "v2", "v2",
			new QuestEvent.TalkToNpc(NPC_ID, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()), 1);
		assertEquals(List.of(), failure.conditions());
		assertEquals(List.of(), failure.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT6.id())),
			failure.afterCommit());

		// 奖励态的普通对话和客户端奖励动作都必须打开 page 5，而不是不存在的 1352。
		// Both the normal reward-state talk and the client reward action must open page 5, not missing page 1352.
		assertRewardPage(definition, QuestDialogAction.USE_OBJECT);
		assertRewardPage(definition, QuestDialogAction.SELECT_QUEST_REWARD);
	}

	private static void assertRewardPage(QuestDefinition definition, QuestDialogAction action) {
		QuestTransition preview = transition(definition, "reward", "reward",
			new QuestEvent.TalkToNpc(NPC_ID, action.id()));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), preview.afterCommit());
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
		QuestEvent event) {
		return transition(definition, source, target, event, null);
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
		QuestEvent event, Integer priority) {
		List<QuestTransition> routes = definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> target.equals(candidate.targetNode()))
			.filter(candidate -> event.equals(candidate.event()))
			.filter(candidate -> priority == null || priority.equals(candidate.priority()))
			.toList();
		assertEquals(1, routes.size(), "quest 1373 " + source + " -> " + target + " " + event);
		return routes.getFirst();
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
		Map<String, Integer> variables) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	private static CompiledQuestDefinition definition() throws Exception {
		try (InputStream input = Quest1373ClientDialogAlignmentTest.class.getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/1373.xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition 1373.xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
