package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证任务 14023 收集石板碎片交付、NPC 对话流与完成奖励的 Aion 5.8 客户端动作合同。
 * Verifies quest 14023 tablet fragment collection turn-in, NPC dialog flow, and completion reward against the Aion 5.8 client contract.
 */
class Quest14023ClientDialogAlignmentTest {
	private static final int TELEMACHUS_NPC_ID = 203965;
	private static final int CASTOR_NPC_ID = 203967;

	private static final int ITEM_LIGHTNING = 182215318;
	private static final int ITEM_WAVE = 182215319;
	private static final int ITEM_WIND = 182215320;
	private static final int ITEM_FIRE = 182215321;

	@Test
	void verifiesFullDialogAndItemTurnInContract() throws Exception {
		QuestDefinition definition = definition().definition();
		assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of("var0", 0));
		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
		assertNode(definition, "s1", QuestStatus.START, Map.of("var0", 1));
		assertNode(definition, "s2", QuestStatus.START, Map.of("var0", 2));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 3));
		assertNode(definition, "complete", QuestStatus.COMPLETE, Map.of("var0", 3));

		// 1. 自动接取：等级提升或区域使命结束触发
		// 1. Auto acquisition on level up or zone mission end
		QuestTransition levelUp = transition(definition, "unaccepted", "started", new QuestEvent.LevelUp());
		assertEquals(List.of(new QuestCondition.StartEligible()), levelUp.conditions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH)),
			levelUp.afterCommit());

		QuestTransition zoneMissionEnd = transition(definition, "unaccepted", "started",
			new QuestEvent.ZoneMissionEnd());
		assertEquals(List.of(new QuestCondition.StartEligible()), zoneMissionEnd.conditions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH)),
			zoneMissionEnd.afterCommit());

		// 2. started 阶段：NPC 203965 (Telemachus) 对话流推进至 s1 (var0=1)
		// 2. started stage: NPC 203965 (Telemachus) dialog flow advances to s1 (var0=1)
		QuestTransition select1 = talk(definition, "started", "started", TELEMACHUS_NPC_ID,
			QuestDialogAction.QUEST_SELECT.id());
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 0)), select1.conditions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT1.id())),
			select1.afterCommit());

		QuestTransition select11 = talk(definition, "started", "started", TELEMACHUS_NPC_ID,
			QuestDialogAction.SELECT1_1.id());
		assertEquals(List.of(), select11.conditions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT1_1.id())),
			select11.afterCommit());

		QuestTransition setpro1 = talk(definition, "started", "s1", TELEMACHUS_NPC_ID,
			QuestDialogAction.SETPRO1.id());
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 0)), setpro1.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 1)), setpro1.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.CloseDialog()), setpro1.afterCommit());

		// 3. s1 阶段：NPC 203967 (Castor) 对话流推进至 s2 (var0=2)
		// 3. s1 stage: NPC 203967 (Castor) dialog flow advances to s2 (var0=2)
		QuestTransition select2 = talk(definition, "s1", "s1", CASTOR_NPC_ID,
			QuestDialogAction.QUEST_SELECT.id());
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 1)), select2.conditions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2.id())),
			select2.afterCommit());

		QuestTransition select21 = talk(definition, "s1", "s1", CASTOR_NPC_ID,
			QuestDialogAction.SELECT2_1.id());
		assertEquals(List.of(), select21.conditions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2_1.id())),
			select21.afterCommit());

		QuestTransition select211 = talk(definition, "s1", "s1", CASTOR_NPC_ID,
			QuestDialogAction.SELECT2_1_1.id());
		assertEquals(List.of(), select211.conditions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2_1_1.id())),
			select211.afterCommit());

		QuestTransition setpro2 = talk(definition, "s1", "s2", CASTOR_NPC_ID,
			QuestDialogAction.SETPRO2.id());
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 1)), setpro2.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 2)), setpro2.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.CloseDialog()), setpro2.afterCommit());

		// 4. s2 阶段：NPC 203967 (Castor) 交付 4 个石板碎片
		// 4. s2 stage: NPC 203967 (Castor) turn in 4 tablet fragments
		QuestTransition select3 = talk(definition, "s2", "s2", CASTOR_NPC_ID,
			QuestDialogAction.QUEST_SELECT.id());
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 2)), select3.conditions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT3.id())),
			select3.afterCommit());

		QuestTransition checkSuccess = talk(definition, "s2", "reward", CASTOR_NPC_ID,
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id());
		assertEquals(Integer.valueOf(0), checkSuccess.priority());
		assertEquals(List.of(
			new QuestCondition.QuestVariableIs("var0", 2),
			new QuestCondition.HasItem(ITEM_LIGHTNING, 1, true),
			new QuestCondition.HasItem(ITEM_WAVE, 1, true),
			new QuestCondition.HasItem(ITEM_WIND, 1, true),
			new QuestCondition.HasItem(ITEM_FIRE, 1, true)), checkSuccess.conditions());
		assertEquals(List.of(
			new QuestAction.RemoveItem(ITEM_LIGHTNING, 1),
			new QuestAction.RemoveItem(ITEM_WAVE, 1),
			new QuestAction.RemoveItem(ITEM_WIND, 1),
			new QuestAction.RemoveItem(ITEM_FIRE, 1)), checkSuccess.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.CHECK_USER_ITEM_OK.id())),
			checkSuccess.afterCommit());

		QuestTransition checkFail = transition(definition, "s2", "s2",
			new QuestEvent.TalkToNpc(CASTOR_NPC_ID, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()), 1);
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 2)), checkFail.conditions());
		assertEquals(List.of(), checkFail.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.CHECK_USER_ITEM_FAIL.id())),
			checkFail.afterCommit());

		QuestTransition finishDialogS2 = talk(definition, "s2", "s2", CASTOR_NPC_ID,
			QuestDialogAction.FINISH_DIALOG.id());
		assertEquals(List.of(new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			finishDialogS2.afterCommit());

		QuestTransition finishDialogReward = talk(definition, "reward", "reward", CASTOR_NPC_ID,
			QuestDialogAction.FINISH_DIALOG.id());
		assertEquals(List.of(new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			finishDialogReward.afterCommit());

		// 5. reward 阶段：NPC 203965 (Telemachus) 报告结果并领奖完成
		// 5. reward stage: NPC 203965 (Telemachus) report result and complete with reward
		QuestTransition useObject = talk(definition, "reward", "reward", TELEMACHUS_NPC_ID,
			QuestDialogAction.USE_OBJECT.id());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT4.id())),
			useObject.afterCommit());

		List<QuestTransition> completions = definition.transitions().stream()
			.filter(t -> "reward".equals(t.sourceNode()) && "complete".equals(t.targetNode()))
			.toList();
		assertEquals(13, completions.size());
		for (QuestTransition completion : completions) {
			assertEquals(List.of(
				new QuestAction.GrantReward("EXP", 0, 1037982L, QuestRewardAmountMode.QUEST_BASE),
				new QuestAction.GrantReward("ITEM", 162000048, 36L, QuestRewardAmountMode.EXACT),
				completion.actions().get(2),
				new QuestAction.CompleteQuest(0)), completion.actions());
		}
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

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event, int priority) {
		return definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode().equals(source)
				&& candidate.targetNode().equals(target) && candidate.event().equals(event)
				&& candidate.priority() != null && candidate.priority() == priority)
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
				"/aion/data/static_data/quest_definition/quests/14023.xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition 14023.xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
