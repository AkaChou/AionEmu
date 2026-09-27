package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 锁定 10520/20520 完成时的经验奖励、高阶守护者晋升、任务完成及协议响应顺序。
 * Locks EXP reward, ArchDaeva promotion, quest completion, and protocol response ordering for quests 10520/20520.
 */
class QuestArchDaevaPromotionDefinitionTest {
	@Test
	void elyosCompletionPromotesArchDaevaAfterTheExpReward() throws Exception {
		assertPromotionContract(10520, 806076);
	}

	@Test
	void asmodianCompletionPromotesArchDaevaAfterTheExpReward() throws Exception {
		assertPromotionContract(20520, 806080);
	}

	private void assertPromotionContract(int questId, int npcId) throws Exception {
		QuestDefinition definition = load(questId).definition();
		List<QuestTransition> completions = definition.transitions().stream()
			.filter(transition -> "reward".equals(transition.sourceNode()))
			.filter(transition -> "complete".equals(transition.targetNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == npcId)
			.toList();

		NodeProjection reward = definition.nodes().stream()
			.filter(node -> "reward".equals(node.label())).findFirst().orElseThrow().projection();
		NodeProjection complete = definition.nodes().stream()
			.filter(node -> "complete".equals(node.label())).findFirst().orElseThrow().projection();
		assertEquals(QuestStatus.REWARD, reward.status());
		// QE-051：reward 投影 = 客户端任务书领奖行（quest_q10520/quest_q20520 六行，末行是“和代理人对话”行 5）。
		// QE-051: the reward projection is the client journal reward row (six rows, reward row index 5).
		assertEquals(Map.of("var0", 5), reward.variables());
		assertEquals(QuestStatus.COMPLETE, complete.status());
		assertEquals(Map.of(), complete.variables());
		assertEquals(List.of(8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23),
			completions.stream().map(transition -> ((QuestEvent.TalkToNpc) transition.event()).dialogId()).toList());
		for (QuestTransition completion : completions) {
			assertEquals("reward", completion.sourceNode());
			assertEquals("complete", completion.targetNode());
			assertEquals(List.of(), completion.conditions());
			assertNull(completion.priority());
			assertEquals(List.of(
				new QuestAction.GrantReward("EXP", 0, 25849149, QuestRewardAmountMode.QUEST_BASE),
				new QuestAction.PromoteArchDaeva(),
				new QuestAction.CompleteQuest(0)), completion.actions());
			assertEquals(List.of(
				new AfterCommitAction.RefreshPlayerStats(),
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
				new AfterCommitAction.ShowQuestSelectionDialog(10)), completion.afterCommit());
		}
	}

	private CompiledQuestDefinition load(int questId) throws Exception {
		// 退役任务的生产 XML 只在 git 历史里：统一取生产视图（XML 目录 + 真端 overlay）。
		return ProductionQuestDefinitions.definition(questId);
	}
}
