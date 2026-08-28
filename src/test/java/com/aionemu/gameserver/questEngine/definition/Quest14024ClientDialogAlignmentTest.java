package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证任务 14024 交书、翻页与传送链使用 Aion 5.8 客户端实际动作。
 * Verifies quest 14024 item turn-in, page turns, and teleport against the actual Aion 5.8 client actions.
 */
class Quest14024ClientDialogAlignmentTest {
	private static final int NPC_ID = 204004;
	private static final int BOOK_ITEM_ID = 182215322;

	@Test
	void keepsItemTurnInAndTeleportDialogChainClientRoutable() throws Exception {
		QuestDefinition definition = definition().definition();
		assertNode(definition, "s2", QuestStatus.START, Map.of("var0", 2));
		assertNode(definition, "s3", QuestStatus.START, Map.of("var0", 3));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 3));

		// NPC 任务入口显示客户端 select4 页面。
		// The NPC entry shows the client-owned select4 page.
		QuestTransition select = talk(definition, "s2", "s2", QuestDialogAction.QUEST_SELECT.id());
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 2)), select.conditions());
		assertEquals(List.of(), select.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT4.id())),
			select.afterCommit());

		// 有调查书时同一次动作进入 s3，并显示 select4_2。
		// With the investigation book, the same action enters s3 and shows select4_2.
		QuestTransition bookCheck = transition(definition, "s2", "s3",
			new QuestEvent.TalkToNpc(NPC_ID, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()));
		assertEquals(Integer.valueOf(0), bookCheck.priority());
		assertEquals(List.of(
			new QuestCondition.QuestVariableIs("var0", 2),
			new QuestCondition.HasItem(BOOK_ITEM_ID, 1, true)), bookCheck.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 3)), bookCheck.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT4_2.id())), bookCheck.afterCommit());

		// 玩家在传送页取消后再次点击 NPC，s3 入口必须直接恢复客户端 select4_2 页面。
		// Re-opening the NPC after canceling on the teleport page must restore the client select4_2 page for s3.
		QuestTransition resume = talk(definition, "s3", "s3", QuestDialogAction.QUEST_SELECT.id());
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 3)), resume.conditions());
		assertEquals(List.of(), resume.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT4_2.id())),
			resume.afterCommit());

		// 客户端 select4_2 页面的“继续听”按钮发送 SELECT4_2_1(2121)，服务器必须显示 select4_2_1。
		// The client select4_2 "continue" button sends SELECT4_2_1(2121); the server must show select4_2_1.
		QuestTransition pageTurn = talk(definition, "s3", "s3", QuestDialogAction.SELECT4_2_1.id());
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 3)), pageTurn.conditions());
		assertEquals(List.of(), pageTurn.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT4_2_1.id())),
			pageTurn.afterCommit());

		// 玩家在传送页取消后仍可重新点击 NPC；确认传送时状态先提交，再同步、传送并关闭窗口。
		// The player can re-open the NPC after canceling on the teleport page; confirming commits state first,
		// then syncs, teleports, and closes in order.
		QuestTransition teleport = transition(definition, "s3", "reward",
			new QuestEvent.TalkToNpc(NPC_ID, QuestDialogAction.SETPRO4.id()));
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 3)), teleport.conditions());
		assertEquals(List.of(), teleport.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.TeleportPlayer(210020000, 1610f, 1528f, 318f, (byte) 2),
			new AfterCommitAction.CloseDialog()), teleport.afterCommit());
	}

	private static QuestTransition talk(QuestDefinition definition, String source, String target, int action) {
		return transition(definition, source, target, new QuestEvent.TalkToNpc(NPC_ID, action));
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
				"/aion/data/static_data/quest_definition/quests/14024.xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition 14024.xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
