package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证任务 14025 阶段对话流、NPC 归属与最终泰雷马科斯领奖页面的 Aion 5.8 客户端动作合同。
 * Verifies quest 14025 phased dialog flows, NPC ownership, and final Telemachus reward page against the Aion 5.8 client contract.
 */
class Quest14025ClientDialogAlignmentTest {
	private static final int AEGIS_NPC_ID = 203989;
	private static final int MABANTA_NPC_ID = 204020;
	private static final int TELEMACHUS_NPC_ID = 203901;

	private static final int ITEM_KAIDAN_FLAG = 182215323;

	@Test
	void verifiesFullDialogAndRewardAlignment() throws Exception {
		QuestDefinition definition = definition().definition();
		assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of("var0", 0, "var1", 0, "var2", 0));
		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
		assertNode(definition, "s1", QuestStatus.START, Map.of("var0", 1));
		assertNode(definition, "s2", QuestStatus.START, Map.of("var0", 2));
		assertNode(definition, "s3", QuestStatus.START, Map.of("var0", 3));
		assertNode(definition, "s4", QuestStatus.START, Map.of("var0", 4));
		assertNode(definition, "s5", QuestStatus.START, Map.of("var0", 5));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 6));
		assertNode(definition, "complete", QuestStatus.COMPLETE, Map.of());

		// 1. started 阶段：NPC 203989 (埃吉斯) 对话翻页与推进至 s1 (var0=1)
		// 1. started stage: NPC 203989 (Aegis) dialog page turns and advances to s1 (var0=1)
		QuestTransition select1 = talk(definition, "started", "started", AEGIS_NPC_ID, QuestDialogAction.QUEST_SELECT.id());
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 0)), select1.conditions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT1.id())), select1.afterCommit());

		QuestTransition select11 = talk(definition, "started", "started", AEGIS_NPC_ID, QuestDialogAction.SELECT1_1.id());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT1_1.id())), select11.afterCommit());

		QuestTransition select111 = talk(definition, "started", "started", AEGIS_NPC_ID, QuestDialogAction.SELECT1_1_1.id());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT1_1_1.id())), select111.afterCommit());

		QuestTransition setpro1 = talk(definition, "started", "s1", AEGIS_NPC_ID, QuestDialogAction.SETPRO1.id());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 1)), setpro1.actions());

		// 2. s3 阶段：NPC 204020 (马邦塔) 翻页对话归属正确并推进至 s4 (var0=4)
		// 2. s3 stage: NPC 204020 (Mabanta) page turns owned correctly and advances to s4 (var0=4)
		QuestTransition select3 = talk(definition, "s3", "s3", MABANTA_NPC_ID, QuestDialogAction.QUEST_SELECT.id());
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 3)), select3.conditions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT3.id())), select3.afterCommit());

		QuestTransition select31 = talk(definition, "s3", "s3", MABANTA_NPC_ID, QuestDialogAction.SELECT3_1.id());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT3_1.id())), select31.afterCommit());

		QuestTransition select311 = talk(definition, "s3", "s3", MABANTA_NPC_ID, QuestDialogAction.SELECT3_1_1.id());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT3_1_1.id())), select311.afterCommit());

		QuestTransition setpro3 = talk(definition, "s3", "s4", MABANTA_NPC_ID, QuestDialogAction.SETPRO3.id());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 4)), setpro3.actions());

		// 3. s4 阶段：NPC 203989 (埃吉斯) 翻页对话推进至 s5 (var0=5)
		// 3. s4 stage: NPC 203989 (Aegis) page turns advance to s5 (var0=5)
		QuestTransition select4 = talk(definition, "s4", "s4", AEGIS_NPC_ID, QuestDialogAction.QUEST_SELECT.id());
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 4)), select4.conditions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT4.id())), select4.afterCommit());

		QuestTransition select41 = talk(definition, "s4", "s4", AEGIS_NPC_ID, QuestDialogAction.SELECT4_1.id());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT4_1.id())), select41.afterCommit());

		QuestTransition select411 = talk(definition, "s4", "s4", AEGIS_NPC_ID, QuestDialogAction.SELECT4_1_1.id());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT4_1_1.id())), select411.afterCommit());

		QuestTransition select4111 = talk(definition, "s4", "s4", AEGIS_NPC_ID, QuestDialogAction.SELECT4_1_1_1.id());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT4_1_1_1.id())), select4111.afterCommit());

		QuestTransition setpro4 = talk(definition, "s4", "s5", AEGIS_NPC_ID, QuestDialogAction.SETPRO4.id());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 5)), setpro4.actions());

		// 4. reward 阶段：NPC 203901 (泰雷马科斯) 必须响应 SELECT7 (3057) 页面而非 DEFAULT_SUCCESS (10002)
		// 4. reward stage: NPC 203901 (Telemachus) must respond with SELECT7 (3057) instead of DEFAULT_SUCCESS (10002)
		QuestTransition rewardQuestSelect = talk(definition, "reward", "reward", TELEMACHUS_NPC_ID, QuestDialogAction.QUEST_SELECT.id());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT7.id())), rewardQuestSelect.afterCommit());

		QuestTransition rewardUseObject = talk(definition, "reward", "reward", TELEMACHUS_NPC_ID, QuestDialogAction.USE_OBJECT.id());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT7.id())), rewardUseObject.afterCommit());

		// 5. npc-complete 预览与奖励选择路由
		// 5. npc-complete preview and reward selection routes
		QuestTransition previewReward = talk(definition, "reward", "reward", TELEMACHUS_NPC_ID, QuestDialogAction.SELECT_QUEST_REWARD.id());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), previewReward.afterCommit());

		QuestTransition rewardChoice1 = talk(definition, "reward", "complete", TELEMACHUS_NPC_ID, QuestDialogAction.SELECTED_QUEST_REWARD1.id());
		assertEquals(List.of(
			new QuestAction.GrantReward("EXP", 0, 3504765L, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("ITEM", 169000008, 400L, QuestRewardAmountMode.EXACT),
			new QuestAction.GrantReward("ITEM", 186000003, 40L, QuestRewardAmountMode.EXACT),
			new QuestAction.GrantReward("SELECTABLE_ITEM", 120001538, 1L, QuestRewardAmountMode.EXACT),
			new QuestAction.CompleteQuest(0)), rewardChoice1.actions());

		QuestTransition rewardChoice2 = talk(definition, "reward", "complete", TELEMACHUS_NPC_ID, QuestDialogAction.SELECTED_QUEST_REWARD2.id());
		assertEquals(List.of(
			new QuestAction.GrantReward("EXP", 0, 3504765L, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("ITEM", 169000008, 400L, QuestRewardAmountMode.EXACT),
			new QuestAction.GrantReward("ITEM", 186000003, 40L, QuestRewardAmountMode.EXACT),
			new QuestAction.GrantReward("SELECTABLE_ITEM", 120001539, 1L, QuestRewardAmountMode.EXACT),
			new QuestAction.CompleteQuest(0)), rewardChoice2.actions());
	}

	private static QuestTransition talk(QuestDefinition definition, String source, String target, int npcId,
			int action) {
		return transition(definition, source, target, new QuestEvent.TalkToNpc(npcId, action));
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
				"/aion/data/static_data/quest_definition/quests/14025.xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition 14025.xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
