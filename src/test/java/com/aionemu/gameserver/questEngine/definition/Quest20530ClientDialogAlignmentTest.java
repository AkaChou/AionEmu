package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 锁定任务 20530「打开深渊之门 / The Aether Field」步骤 7 收集交付与步骤 8 对话续接的客户端对齐合同。
 * Locks the step 7 collection handover and step 8 dialog continuation client contract for quest 20530.
 * <p>Aion 5.8 客户端在 collect_progress=7 时以 progress==7 (var0==7) 校验背包持有魔法阵染料 (182216166) 与咒语书碎片 (182216168)，
 * 并在巴尔德尔 (204075) 处交互 select8 -> check_user_has_quest_item。
 * 严禁在祭坛 (703389) 交互时抢跑推进 var0=8（这会导致客户端判定不在收集步而不发任务对话）。
 * 步骤 7 交付成功后扣除两件道具并推进 var0=8 进入 s8，展示 check_user_item_ok(10000)；
 * 随后在步骤 8 (s8) 通过 SELECT9 衔接对白，最终以 SELECT_QUEST_REWARD 播放影片 148 并进入 reward 结算。</p>
 */
class Quest20530ClientDialogAlignmentTest {
	private static final int BALDER_NPC_ID = 204075;
	private static final int DYE_ITEM_ID = 182216166;
	private static final int FRAGMENT_ITEM_ID = 182216168;
	private static final int ALTAR_OBJECT_ID = 703389;

	@Test
	void step7CollectionHandoverAndStep8DialogContinuationContract() throws Exception {
		QuestDefinition definition = definition();

		// 1. 祭坛交互不抢跑推进 var0=8，仍停留在 s7
		// Altar interaction must not advance var0 to 8 prematurely, remaining in s7
		QuestTransition altarUse = transition(definition, "s7", "s7",
			new QuestEvent.TalkToNpc(ALTAR_OBJECT_ID, QuestDialogAction.USE_OBJECT.id()));
		assertEquals(List.of(new QuestAction.GiveItem(FRAGMENT_ITEM_ID, 1)), altarUse.actions());

		// 2. 步骤 7: 巴尔德尔 QUEST_SELECT 必须在 s7 展示 SELECT8
		// Step 7: Balder QUEST_SELECT in s7 must show SELECT8
		QuestTransition select8 = talk(definition, "s7", "s7", BALDER_NPC_ID, QuestDialogAction.QUEST_SELECT.id());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT8.id())),
			select8.afterCommit());

		// 3. 步骤 7 交付成功 (priority 0): 在 s7 校验持有两件道具，扣除后推进 var0=8 进入 s8
		// Step 7 handover success: verifies items in s7, consumes them and sets var0=8 to s8
		QuestTransition handOverSuccess = transition(definition, "s7", "s8",
			new QuestEvent.TalkToNpc(BALDER_NPC_ID, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()), 0);
		assertEquals(List.of(
			new QuestCondition.QuestVariableIs("var0", 7),
			new QuestCondition.HasItem(DYE_ITEM_ID, 1, true),
			new QuestCondition.HasItem(FRAGMENT_ITEM_ID, 1, true)), handOverSuccess.conditions());
		assertEquals(List.of(
			new QuestAction.RemoveItem(DYE_ITEM_ID, 1),
			new QuestAction.RemoveItem(FRAGMENT_ITEM_ID, 1),
			new QuestAction.SetVariable("var0", 8)), handOverSuccess.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.CHECK_USER_ITEM_OK.id())),
			handOverSuccess.afterCommit());

		// 4. 步骤 8 续绝对话: s8 支持 QUEST_SELECT 与 SELECT9 展示 SELECT9
		// Step 8 dialog continuation: s8 supports QUEST_SELECT and SELECT9 displaying SELECT9
		QuestTransition questSelectInS8 = talk(definition, "s8", "s8", BALDER_NPC_ID, QuestDialogAction.QUEST_SELECT.id());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT9.id())),
			questSelectInS8.afterCommit());

		// 5. 步骤 8 完成奖励选择: SELECT_QUEST_REWARD 播放影片 148 并进入 reward
		// Step 8 reward selection: SELECT_QUEST_REWARD plays movie 148 and enters reward
		QuestTransition rewardTransition = talk(definition, "s8", "reward", BALDER_NPC_ID,
			QuestDialogAction.SELECT_QUEST_REWARD.id());
		assertEquals(List.of(
			new AfterCommitAction.PlayMovie(148),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			rewardTransition.afterCommit());

		// 6. 旧存档自愈: enter-world 将 var0=8 且仍持有碎片的玩家回退至 s7
		// Self-healing migration: enter-world rolls back var0=8 with fragment to s7
		QuestTransition enterWorldHeal = transition(definition, "s8", "s7", new QuestEvent.EnterWorld());
		assertEquals(List.of(
			new QuestCondition.QuestVariableIs("var0", 8),
			new QuestCondition.HasItem(FRAGMENT_ITEM_ID, 1, true)), enterWorldHeal.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 7)), enterWorldHeal.actions());
	}

	private static QuestTransition talk(QuestDefinition definition, String source, String target, int npcId,
			int action) {
		return transition(definition, source, target, new QuestEvent.TalkToNpc(npcId, action));
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event) {
		return definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()) && target.equals(candidate.targetNode())
				&& event.equals(candidate.event()))
			.findFirst().orElseThrow();
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event, int priority) {
		return definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()) && target.equals(candidate.targetNode())
				&& event.equals(candidate.event()) && Integer.valueOf(priority).equals(candidate.priority()))
			.findFirst().orElseThrow();
	}

	private static QuestDefinition definition() throws Exception {
		try (InputStream input = Quest20530ClientDialogAlignmentTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/20530.xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition 20530.xml");
			}
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}
}
