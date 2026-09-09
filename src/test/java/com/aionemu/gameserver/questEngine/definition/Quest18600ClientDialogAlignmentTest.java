package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证任务 18600 的客户端对话页链、步骤 NPC 所有权和奖励交付合同。
 * Verifies quest 18600 client dialog pages, step-NPC ownership, and reward delivery contracts.
 */
class Quest18600ClientDialogAlignmentTest {
	private static final int START_NPC = 204500;
	private static final int KUROCHINERK = 804601;
	private static final int HERTHIA = 205228;
	private static final int PAPER_BILL = 182213000;
	private static final int FAKE_STIGMA = 182213001;

	@Test
	void acceptDialogUsesTheClientConfirmationWindow() throws Exception {
		QuestDefinition definition = load().definition();

		assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of("var0", 0));
		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT1.id())),
			route(definition, "unaccepted", START_NPC, QuestDialogAction.QUEST_SELECT).afterCommit());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id())),
			route(definition, "unaccepted", START_NPC, QuestDialogAction.ASK_QUEST_ACCEPT).afterCommit());

		QuestTransition accept = route(definition, "unaccepted", START_NPC, QuestDialogAction.QUEST_ACCEPT_1);
		assertEquals("started", accept.targetNode());
		assertEquals(List.of(new QuestCondition.StartEligible()), accept.conditions());
		assertEquals(List.of(new QuestAction.GiveItem(PAPER_BILL, 1)), accept.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.QUEST_ACCEPT_1.id())),
			accept.afterCommit());
	}

	@Test
	void intermediateNpcPagesAndFinalRewardPagesFollowTheLegacyChain() throws Exception {
		QuestDefinition definition = load().definition();
		assertNode(definition, "s1", QuestStatus.START, Map.of("var0", 1));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 3));

		assertPage(definition, "started", KUROCHINERK, QuestDialogAction.QUEST_SELECT,
			QuestDialogPage.SELECT2);
		assertPage(definition, "started", KUROCHINERK, QuestDialogAction.SELECT2_1,
			QuestDialogPage.SELECT2_1);
		QuestTransition billDelivery = route(definition, "started", KUROCHINERK, QuestDialogAction.SETPRO1);
		assertEquals("s1", billDelivery.targetNode());
		assertEquals(List.of(new QuestAction.RemoveItem(PAPER_BILL, 1)), billDelivery.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			billDelivery.afterCommit());

		assertPage(definition, "s1", HERTHIA, QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT3);
		assertPage(definition, "s1", HERTHIA, QuestDialogAction.SELECT3_1, QuestDialogPage.SELECT3_1);
		QuestTransition report = route(definition, "s1", HERTHIA, QuestDialogAction.SETPRO2);
		assertEquals("reward", report.targetNode());
		assertEquals(List.of(), report.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			report.afterCommit());

		assertPage(definition, "reward", START_NPC, QuestDialogAction.USE_OBJECT, QuestDialogPage.SELECT5);
		assertPage(definition, "reward", START_NPC, QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT5);
		QuestTransition rewardPreview = route(definition, "reward", START_NPC,
			QuestDialogAction.SELECT_QUEST_REWARD);
		assertEquals(List.of(new QuestAction.RemoveItem(FAKE_STIGMA, QuestAction.RemoveItem.ALL)),
			rewardPreview.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), rewardPreview.afterCommit());
	}

	private static void assertPage(QuestDefinition definition, String source, int npcId,
		QuestDialogAction action, QuestDialogPage page) {
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())),
			route(definition, source, npcId, action).afterCommit());
	}

	private static QuestTransition route(QuestDefinition definition, String source, int npcId,
		QuestDialogAction action) {
		return definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode())
				&& transition.event().equals(new QuestEvent.TalkToNpc(npcId, action.id())))
			.findFirst()
			.orElseThrow(() -> new AssertionError("missing quest 18600 route: "
				+ source + " " + npcId + " " + action));
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
		Map<String, Integer> variables) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> label.equals(candidate.label()))
			.findFirst()
			.orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	private CompiledQuestDefinition load() throws Exception {
		try (InputStream input = getClass().getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/18600.xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition 18600.xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
