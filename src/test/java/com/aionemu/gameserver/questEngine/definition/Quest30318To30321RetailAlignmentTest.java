package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.retail.RetailQuestDriver;
import com.aionemu.gameserver.questEngine.tablelane.CameraRegistry;
import com.aionemu.gameserver.questEngine.tablelane.NativeQuestTableLoader;
import com.aionemu.gameserver.questEngine.tablelane.NativeTalkFixture;
import com.aionemu.gameserver.questEngine.tablelane.SimpleHuntHandler;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定 30318-30321（魔族鲁德拉/提亚马特亡灵每日与后置任务）的真端狩猎合同：
 * 前置链、击杀门（25 小怪 / 1 精英）、接取与报告 NPC、奖励面。
 * Locks 30318-30321's retail hunt contract: the prerequisite chain, the kill gates (25 trash / 1 elite),
 * the accept and report NPCs and the reward face.
 * <p>
 * 任务已退役（保留清单 owner=RETAIL_TABLE）：旧 typed XML 的转换金标随迁移退场，
 * 按计划 §8.9（P3 重锚口径）改锚真端表行 + 相机满值 + quest.xml 前置/奖励事实 + native 对话面。
 * <p>
 * Retired rows (owner=RETAIL_TABLE) are re-anchored (plan §8.9) to the retail rows, the camera gates,
 * the quest.xml prerequisite/reward facts and the native faces.
 */
class Quest30318To30321RetailAlignmentTest {

	/** 真端行：接取 = 报告 NPC（Aeshma 799504 / Bhemah 799505），单槽计数。 */
	@Test
	void retailRowsKeepThePrerequisiteChainAndTheKillGates() throws Exception {
		assertRow(30318, "Aeshma", 25, "IDCatacombsN_UnDrakanBoneFi_55_Ae");
		assertRow(30319, "Aeshma", 1, "IDCatacombsN_UnDrakanScNmd_55_Ah");
		assertRow(30320, "Bhemah", 25, "IDCatacombsN_UnDrakanGhostFi_55_Ae");
		assertRow(30321, "Bhemah", 1, "IDCatacombsN_DrakanFiDayNmd_55_Ah");

		assertEquals(799504, SimpleHuntHandler.instance().rewardNpc(30318), "Aeshma 解析");
		assertEquals(799505, SimpleHuntHandler.instance().rewardNpc(30320), "Bhemah 解析");
		for (int questId : List.of(30318, 30319, 30320, 30321)) {
			assertTrue(RetiredQuestIds.contains(questId), questId + " 必须在保留清单 owner=RETAIL_TABLE 内");
			assertTrue(SimpleHuntHandler.instance().routes(questId), questId + " 必须由 native 车道执行");
			assertFalse(ProductionQuestDefinitions.catalog().findExecutable(questId).isPresent(),
				questId + " 退役后 typed 目录不得再持有");
		}
	}

	@Test
	void prerequisitesAndRewardsComeFromTheRetailRow() throws Exception {
		assertPrerequisite(30318, 30316);
		assertPrerequisite(30319, 30318);
		assertPrerequisite(30320, 30316);
		assertPrerequisite(30321, 30320);

		assertRewards(30318, new QuestReward("GOLD", 0, 28900), new QuestReward("EXP", 0, 6517414));
		assertRewards(30319, new QuestReward("GOLD", 0, 197550), new QuestReward("EXP", 0, 6517414),
			new QuestReward("ITEM", 182209718, 1));
		assertRewards(30320, new QuestReward("GOLD", 0, 28900), new QuestReward("EXP", 0, 6517414));
		assertRewards(30321, new QuestReward("GOLD", 0, 197550), new QuestReward("EXP", 0, 6517414),
			new QuestReward("ITEM", 182209719, 1));
	}

	@Test
	void inProgressAndRewardFacesFollowTheNativeDialogPages() {
		SimpleHuntHandler handler = SimpleHuntHandler.instance();
		Player player = NativeTalkFixture.player(Race.ASMODIANS, PlayerClass.WARRIOR, 55);
		for (int questId : List.of(30318, 30319)) {
			Integer reportNpc = handler.rewardNpc(questId);
			assertNotNull(reportNpc, "真端行必须有可解析的交付 NPC: " + questId);

			NativeTalkFixture.clearPackets(player);
			NativeTalkFixture.add(player, questId, QuestStatus.START, 0);
			assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, reportNpc, questId, 31)));
			NativeTalkFixture.assertOnlyDialogPage(player, 10);

			NativeTalkFixture.clearPackets(player);
			player.getQuestStateList().getQuestState(questId).setStatus(QuestStatus.REWARD);
			assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, reportNpc, questId, 31)));
			NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 5, questId);
		}
	}

	private static void assertRow(int questId, String npcName, int count, String monsterGroup) {
		NativeQuestTableLoader.SimpleHuntRow row = NativeQuestTableLoader.instance().require(questId);
		assertEquals(npcName, row.acquiredNpcName(), questId + " 接取 NPC");
		assertEquals(npcName, row.rewardNpcName(), questId + " 交付 NPC");
		assertEquals(count, row.killSlots().get(1).count(), questId + " 真端 count1");
		assertTrue(row.killSlots().get(1).monsters().contains(monsterGroup),
			() -> questId + " 真端 monster1: " + row.killSlots().get(1).monsters());

		CameraRegistry.CameraRow camera = CameraRegistry.instance().require(questId);
		assertNotNull(camera, questId + " 必须有相机行");
		assertEquals(count, camera.required(1), questId + " 相机槽 1 需求");
	}

	private static void assertPrerequisite(int questId, int prerequisite) throws Exception {
		QuestMetadata metadata = metadata(questId);
		// 前置两种表达等价：真端 finished_quest_cond 无其它条件族时编译为 prerequisites。
		assertTrue(metadata.prerequisites().contains(prerequisite)
				|| metadata.startConditions().contains(new QuestStartCondition("finished", prerequisite, 0)),
			() -> questId + " 必须要求完成 " + prerequisite + "（实际前置 " + metadata.prerequisites() + "）");
	}

	private static void assertRewards(int questId, QuestReward... expected) throws Exception {
		List<QuestReward> rewards = metadata(questId).rewards();
		for (QuestReward reward : expected) {
			assertTrue(rewards.contains(reward), () -> questId + " 奖励缺失 " + reward + "，实际 " + rewards);
		}
	}

	private static QuestMetadata metadata(int questId) throws Exception {
		return RetailQuestDriver.ensureLoaded().retailMetadataOf(questId).orElseThrow().metadata();
	}
}
