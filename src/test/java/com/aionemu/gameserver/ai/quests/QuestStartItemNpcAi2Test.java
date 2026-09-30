package com.aionemu.gameserver.ai.quests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;

class QuestStartItemNpcAi2Test {

	@Test
	void triesUseObjectBeforeFallingBackToTheStartDialog() {
		assertEquals(List.of(QuestDialogAction.USE_OBJECT.id()), QuestStartItemNpcAi2.dialogIdsFor(false));
		assertEquals(List.of(QuestDialogAction.USE_OBJECT.id(), QuestDialogAction.QUEST_SELECT.id()),
			QuestStartItemNpcAi2.dialogIdsFor(true));
	}

	@Test
	void startItemNpcRewardsAlignWithRetailContracts() {
		// 700009 (Scroll_Q41) -> 任务 1198 (壁に刺さった手紙), 道具 182200559 (The Bandit's Letter)
		QuestStartItemNpcAi2.StartItemReward r700009 = QuestStartItemNpcAi2.rewardForNpc(700009);
		assertNotNull(r700009);
		assertEquals(1198, r700009.questId());
		assertEquals(182200559, r700009.itemId());

		// 700004 (Scroll_Q49) -> 任务 1197 (克拉尔的书), 道具 182200558
		QuestStartItemNpcAi2.StartItemReward r700004 = QuestStartItemNpcAi2.rewardForNpc(700004);
		assertNotNull(r700004);
		assertEquals(1197, r700004.questId());
		assertEquals(182200558, r700004.itemId());

		// 700302 (Scroll_Q2498) -> 任务 2498, 道具 182204232
		QuestStartItemNpcAi2.StartItemReward r700302 = QuestStartItemNpcAi2.rewardForNpc(700302);
		assertNotNull(r700302);
		assertEquals(2498, r700302.questId());
		assertEquals(182204232, r700302.itemId());

		// 700513 (LF3_Box_Q1559) -> 任务 1559 (打开箱子), 道具 182201823
		QuestStartItemNpcAi2.StartItemReward r700513 = QuestStartItemNpcAi2.rewardForNpc(700513);
		assertNotNull(r700513);
		assertEquals(1559, r700513.questId());
		assertEquals(182201823, r700513.itemId());

		// 未知 NPC 返回 null
		assertNull(QuestStartItemNpcAi2.rewardForNpc(999999));
	}

	@Test
	void canReceiveRewardRejectsNullPlayer() {
		assertFalse(QuestStartItemNpcAi2.canReceiveReward(null, 1198, 182200559));
	}
}
