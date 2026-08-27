package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证任务 1311 的接取页链、苗木交付和奖励报告合同。
 * Verifies the quest 1311 accept page chain, soil delivery, and reward report contracts.
 */
class Quest1311ClientDialogAlignmentTest {
	private static final int START_NPC_ID = 203997;
	private static final int SOIL_OBJECT_ID = 700164;
	private static final int SOIL_ITEM_ID = 182201305;

	@Test
	void acceptDialogChainReachesTheLegacyAcceptWindowAndStartsTheQuest() throws Exception {
		QuestDefinition definition = definition().definition();
		assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of("var0", 0));
		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));

		// Aion 5.8 客户端沿 1011 -> 1012 继续阅读，1013 必须返回旧脚本的确认页 4。
		// The Aion 5.8 client reads 1011 -> 1012; action 1013 must return the legacy confirmation page 4.
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT1.id())),
			talk(definition, "unaccepted", "unaccepted", QuestDialogAction.QUEST_SELECT.id()).afterCommit());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT1_1.id())),
			talk(definition, "unaccepted", "unaccepted", QuestDialogAction.SELECT1_1.id()).afterCommit());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id())),
			talk(definition, "unaccepted", "unaccepted", QuestDialogAction.SELECT1_1_1.id()).afterCommit());

		// 接受任务时发放苗木，提交后同步可见性并打开客户端确认页 1003。
		// Accepting grants the germ, then refreshes visibility and opens client page 1003 after commit.
		QuestTransition accept = talk(definition, "unaccepted", "started",
			QuestDialogAction.QUEST_ACCEPT_1.id());
		assertEquals(List.of(new QuestCondition.StartEligible()), accept.conditions());
		assertEquals(List.of(new QuestAction.GiveItem(SOIL_ITEM_ID, 1)), accept.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.QUEST_ACCEPT_1.id())),
			accept.afterCommit());
	}

	@Test
	void soilDeliveryEntersRewardAndTheReportShowsTheRetailPage() throws Exception {
		QuestDefinition definition = definition().definition();
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 3));

		// 使用可交互苗木扣除任务物品并把状态推进到 REWARD。
		// Using the interactive soil removes the quest item and advances to REWARD.
		QuestTransition deliver = transition(definition, "started", "reward",
			new QuestEvent.TalkToNpc(SOIL_OBJECT_ID, QuestDialogAction.USE_OBJECT.id()));
		assertEquals(List.of(new QuestCondition.HasItem(SOIL_ITEM_ID, 1)), deliver.conditions());
		assertEquals(List.of(new QuestAction.RemoveItem(SOIL_ITEM_ID, 1)), deliver.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
				QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), deliver.afterCommit());

		// 旧脚本要求报告页 2375，选择奖励后打开奖励窗口 5。
		// The legacy handler requires report page 2375, then reward selection opens page 5.
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT5.id())),
			talk(definition, "reward", "reward", QuestDialogAction.USE_OBJECT.id()).afterCommit());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			talk(definition, "reward", "reward", QuestDialogAction.SELECT_QUEST_REWARD.id()).afterCommit());
	}

	private static QuestTransition talk(QuestDefinition definition, String source, String target, int action) {
		return transition(definition, source, target, new QuestEvent.TalkToNpc(START_NPC_ID, action));
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event) {
		return definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode())
				&& target.equals(candidate.targetNode()) && event.equals(candidate.event()))
			.findFirst().orElseThrow();
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
			Map<String, Integer> variables) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	private CompiledQuestDefinition definition() throws Exception {
		try (InputStream input = getClass().getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/1311.xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition 1311.xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
