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
 * 锁定「击杀打满后仍停在 START，直到报告动作」的两条原版狩猎线：11110 与 1548。
 * Locks the two retail hunt rows whose START step saturates until the report action: 11110 and 1548.
 * <p>
 * 任务已退役（保留清单 owner=RETAIL_TABLE）：旧 XML 的阶梯节点金标随迁移退场，
 * 按计划 §8.9（P3 重锚口径）改锚原版表行 + 相机满值 + quest.xml 前置/奖励事实 + native 对话面。
 * <p>
 * The retired IR step ladders are re-anchored (plan §8.9) to the retail rows, the camera gates, the
 * quest.xml prerequisite/reward facts and the native faces.
 */
class Quest11110And1548PostKillReportDialogTest {

	@Test
	void retailRowsKeepTheKillGoalAndTheReportOwner() throws Exception {
		assertRow(11110, "Suleion", 10, List.of("LF4_B8_Nepilim_55_An", "LF4_B8_Nepilim_Tech_55_An"));
		assertRow(1548, "Senemonea", 5, List.of("LF3_Neutspawner_Q1053"));
		assertEquals(Integer.valueOf(1549), SimpleHuntHandler.instance().conQuest(1548), "原版 con_quest");

		for (int questId : List.of(11110, 1548)) {
			assertTrue(RetiredQuestIds.contains(questId), questId + " 必须在保留清单 owner=RETAIL_TABLE 内");
			assertTrue(SimpleHuntHandler.instance().routes(questId), questId + " 必须由 native 车道执行");
			assertFalse(ProductionQuestDefinitions.catalog().findExecutable(questId).isPresent(),
				questId + " 退役后 typed 目录不得再持有");
		}
	}

	@Test
	void prerequisitesAndRewardsComeFromTheRetailRow() throws Exception {
		assertPrerequisite(11110, 11109);
		assertPrerequisite(1548, 1547);

		List<QuestReward> ambitious = metadata(11110).rewards();
		assertTrue(ambitious.contains(new QuestReward("GOLD", 0, 6900)), () -> ambitious.toString());
		assertTrue(ambitious.contains(new QuestReward("EXP", 0, 2814541)), () -> ambitious.toString());
		assertEquals(10, metadata(11110).repeatPolicy().maxRepeatCount(), "原版 max_repeat_count=10");

		List<QuestReward> research = metadata(1548).rewards();
		assertTrue(research.contains(new QuestReward("EXP", 0, 1496758)), () -> research.toString());
		assertTrue(research.contains(new QuestReward("ITEM", 186000004, 3)), () -> research.toString());
	}

	@Test
	void inProgressAndRewardFacesFollowTheNativeDialogPages() {
		SimpleHuntHandler handler = SimpleHuntHandler.instance();
		Player player = NativeTalkFixture.player(Race.ELYOS, PlayerClass.WARRIOR, 53);
		for (int questId : List.of(11110, 1548)) {
			Integer reportNpc = handler.rewardNpc(questId);
			assertNotNull(reportNpc, "原版行必须有可解析的交付 NPC: " + questId);

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

	private static void assertRow(int questId, String npcName, int count, List<String> monsters) {
		NativeQuestTableLoader.SimpleHuntRow row = NativeQuestTableLoader.instance().require(questId);
		assertEquals(npcName, row.acquiredNpcName(), questId + " 接取 NPC");
		assertEquals(npcName, row.rewardNpcName(), questId + " 交付 NPC");
		assertEquals(count, row.killSlots().get(1).count(), questId + " 原版 count1");
		assertEquals(monsters, row.killSlots().get(1).monsters(), questId + " 原版 monster1");

		CameraRegistry.CameraRow camera = CameraRegistry.instance().require(questId);
		assertNotNull(camera, questId + " 必须有相机行");
		assertEquals(count, camera.required(1), questId + " 相机槽 1 需求");
	}

	private static void assertPrerequisite(int questId, int prerequisite) throws Exception {
		QuestMetadata metadata = metadata(questId);
		assertTrue(metadata.prerequisites().contains(prerequisite)
				|| metadata.startConditions().contains(new QuestStartCondition("finished", prerequisite, 0)),
			() -> questId + " 必须要求完成 " + prerequisite + "（实际前置 " + metadata.prerequisites() + "）");
	}

	private static QuestMetadata metadata(int questId) throws Exception {
		return RetailQuestDriver.ensureLoaded().retailMetadataOf(questId).orElseThrow().metadata();
	}
}
