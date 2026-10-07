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
 * 锁定任务 2841（上层面阿斯特里亚全歼）的真端狩猎合同：44 杀门、报告 NPC 与奖励面。
 * Locks quest 2841's retail hunt contract: the 44-kill gate, the report NPC and the reward face.
 * <p>
 * 任务已退役（保留清单 owner=RETAIL_TABLE）：旧 XML 的节点/转换金标（hunting / hunting-complete /
 * reward 标签）随迁移退场，按计划 §8.9（P3 重锚口径）改锚真端表行 + 相机（CameraRegistry）满值 +
 * quest.xml 奖励事实 + native 对话面。「第 44 杀仍保持 START、报告动作才进 REWARD」在原生车道
 * 由相机满值语义承担（{@code SimpleHuntNativeFamilyGateTest} 全族不变量）。
 * <p>
 * The retired IR gold standard (hunting / hunting-complete / reward labels) is re-anchored (plan §8.9)
 * to the retail row, the camera registry's saturated value, the quest.xml reward facts and the native
 * faces; the "44th kill stays START" semantics live in the camera full-value invariant.
 */
class Quest2841RetailAlignmentTest {
	private static final int QUEST_ID = 2841;
	/** 真端行 reward_npc_name=Herz。 / Retail row report NPC. */
	private static final int REPORT_NPC = 278003;

	@Test
	void retiredRowIsOwnedByTheNativeLaneWithoutATypedDefinition() {
		assertTrue(RetiredQuestIds.contains(QUEST_ID), "2841 必须在保留清单 owner=RETAIL_TABLE 内");
		assertTrue(SimpleHuntHandler.instance().routes(QUEST_ID), "SimpleHunt native 车道必须路由 2841");
		assertFalse(ProductionQuestDefinitions.catalog().findExecutable(QUEST_ID).isPresent(),
			"退役后 typed 目录不得再持有 2841");
	}

	@Test
	void cameraRegistryCarriesTheFortyFourKillGateOnTheRetailMonsterGroup() {
		NativeQuestTableLoader.SimpleHuntRow row = NativeQuestTableLoader.instance().require(QUEST_ID);
		assertEquals(44, row.killSlots().get(1).count(), "真端 count1=44");
		assertEquals(List.of("IDAbRe_Up_Asteria"), row.killSlots().get(1).monsters(), "真端 monster1 组名");
		assertEquals("Herz", row.rewardNpcName());

		// 满值语义：槽 1 需求 = 44，fullValue = 44 << shift(1)。
		// Saturated value: slot 1 requires 44 and fullValue is 44 shifted into slot 1.
		CameraRegistry.CameraRow camera = CameraRegistry.instance().require(QUEST_ID);
		assertNotNull(camera, "2841 必须有相机行");
		assertEquals(44, camera.required(1), "相机槽 1 需求");
		assertEquals(44 << camera.width().shift(1), camera.fullValue(), "相机满值");

		SimpleHuntHandler handler = SimpleHuntHandler.instance();
		assertNotNull(handler.acquireNpc(QUEST_ID), "真端行必须有可解析的接取 NPC");
		assertEquals(REPORT_NPC, handler.rewardNpc(QUEST_ID));
		assertTrue(handler.questsForNpc(REPORT_NPC).contains(QUEST_ID), "Herz 必须服务 2841");
	}

	@Test
	void rewardFactsComeFromTheRetailRow() throws Exception {
		QuestMetadata metadata = RetailQuestDriver.ensureLoaded()
			.retailMetadataOf(QUEST_ID).orElseThrow().metadata();

		assertEquals(45, metadata.minLevel(), "真端 minlevel_permitted=45");
		assertEquals(java.util.Set.of("ASMODIANS"), metadata.permittedRaces(), "真端 pc_dark");
		assertEquals(255, metadata.repeatPolicy().maxRepeatCount(), "真端 max_repeat_count=255（可重复）");
		List<QuestReward> rewards = metadata.rewards();
		// 真端 quest.xml：reward_exp1=2068277、reward_abyss_point1=700。
		assertTrue(rewards.contains(new QuestReward("EXP", 0, 2068277)), () -> rewards.toString());
		assertTrue(rewards.contains(new QuestReward("AP", 0, 700)), () -> rewards.toString());
	}

	@Test
	void inProgressAndRewardFacesFollowTheNativeDialogPages() {
		SimpleHuntHandler handler = SimpleHuntHandler.instance();
		Player player = NativeTalkFixture.player(Race.ASMODIANS, PlayerClass.WARRIOR, 45);
		Integer reportNpc = handler.rewardNpc(QUEST_ID);
		assertNotNull(reportNpc);

		NativeTalkFixture.add(player, QUEST_ID, QuestStatus.START, 0);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, reportNpc, QUEST_ID, 31)));
		NativeTalkFixture.assertOnlyDialogPage(player, 10);

		NativeTalkFixture.clearPackets(player);
		player.getQuestStateList().getQuestState(QUEST_ID).setStatus(QuestStatus.REWARD);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, reportNpc, QUEST_ID, 31)));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 5, QUEST_ID);
	}
}
