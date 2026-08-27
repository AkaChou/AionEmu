package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证任务 14015 的升级登记、收集对话链与 Aion 5.8 客户端页面合同。
 * Verifies quest 14015 level-up acquisition, collect dialog chain, and Aion 5.8 client page contracts.
 */
class Quest14015ClientDialogAlignmentTest {
	private static final int TURN_IN_NPC_ID = 203098;
	private static final int COLLECTED_ITEM_ID = 182215316;

	@Test
	void keepsLevelUpAcquisitionFreeOfDialogPages() throws Exception {
		QuestDefinition definition = definition().definition();
		assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of("var0", 0));
		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 1));
		assertNode(definition, "complete", QuestStatus.COMPLETE, Map.of("var0", 0));

		QuestTransition levelUp = transition(definition, "unaccepted", "started", new QuestEvent.LevelUp());
		assertEquals(List.of(
			new QuestCondition.StartEligible(),
			new QuestCondition.QuestsFinished(Set.of(14010))), levelUp.conditions());
		assertEquals(List.of(), levelUp.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH)), levelUp.afterCommit());

		QuestTransition zoneMissionEnd = transition(definition, "unaccepted", "started",
			new QuestEvent.ZoneMissionEnd());
		assertEquals(List.of(new QuestCondition.StartEligible()), zoneMissionEnd.conditions());
		assertEquals(List.of(), zoneMissionEnd.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH)),
			zoneMissionEnd.afterCommit());
	}

	@Test
	void collectDialogChainUsesOnlyClientOwnedPages() throws Exception {
		QuestDefinition definition = definition().definition();

		// 选择任务后显示客户端 quest_q14015.html 实际存在的入口页 1011。
		// Show the client-owned entry page 1011 of quest_q14015.html after quest selection.
		QuestTransition select = talk(definition, "started", "started", QuestDialogAction.QUEST_SELECT.id());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT1.id())),
			select.afterCommit());

		// 客户端 select1 -> select1_1 -> select1_1_1 的"继续听"按钮链。
		// The client "keep listening" chain select1 -> select1_1 -> select1_1_1.
		QuestTransition firstPage = talk(definition, "started", "started", QuestDialogAction.SELECT1_1.id());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT1_1.id())),
			firstPage.afterCommit());
		QuestTransition secondPage = talk(definition, "started", "started", QuestDialogAction.SELECT1_1_1.id());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT1_1_1.id())),
			secondPage.afterCommit());

		// 物品检查成功：集齐 10 个证物时同一次交互进入奖励并打开奖励窗口。
		// Successful item check: with all 10 proofs, the same interaction enters reward and opens the reward window.
		QuestTransition success = transition(definition, "started", "reward",
			new QuestEvent.TalkToNpc(TURN_IN_NPC_ID, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()));
		assertEquals(Integer.valueOf(0), success.priority());
		assertEquals(List.of(new QuestCondition.HasItem(COLLECTED_ITEM_ID, 10)), success.conditions());
		assertEquals(List.of(
			new QuestAction.SetVariable("var0", 1),
			new QuestAction.RemoveItem(COLLECTED_ITEM_ID, 10)), success.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			success.afterCommit());

		// 物品检查失败：回落到客户端存在的 1097 提示页，不移除物品、不推进状态。
		// Failed item check: fall back to the client-owned page 1097 without removing items or advancing.
		QuestTransition failure = transition(definition, "started", "started",
			new QuestEvent.TalkToNpc(TURN_IN_NPC_ID, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()));
		assertEquals(Integer.valueOf(1), failure.priority());
		assertEquals(List.of(), failure.conditions());
		assertEquals(List.of(), failure.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT1_2.id())),
			failure.afterCommit());
	}

	private static QuestTransition talk(QuestDefinition definition, String source, String target, int action) {
		return transition(definition, source, target, new QuestEvent.TalkToNpc(TURN_IN_NPC_ID, action));
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event) {
		return definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode().equals(source)
				&& candidate.targetNode().equals(target) && candidate.event().equals(event))
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
				"/aion/data/static_data/quest_definition/quests/14015.xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition 14015.xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
