package com.aionemu.gameserver.questEngine.tablelane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * SimpleSerialHunt 原生表驱动家族门禁测试（计划 §6.2 / §7 / P2 切换批）。
 * <p>
 * 验证：
 * 1. 真端表行全量 16 个 SimpleSerialHunt 任务 100% 装载并纳入 SimpleSerialHuntHandler 管理；
 * 2. CameraRegistry 完整包含全部 16 行相机参数，且 fullValue 与阶段需求一致；
 * 3. 严格串行阶梯推进：首个未满阶段计数推进（+0xf0），乱序或超前击杀零动作（NO_ACTION）；
 * 4. 简报 NPC 守卫门控：声明 talk_npc 的任务在 1<<30 标志位未清前击杀零动作，与简报 NPC 对话清位后方可开启击杀；
 * 5. 全阶段饱和收口：全部阶段打满后触发推进写入（+0x100）并翻转 REWARD 状态；
 * 6. 原生对话页流闭环：4（接取）、10（进行中/未完成）、5（领奖窗口）、10（领奖收尾回选择对话页）。
 */
class SimpleSerialHuntNativeFamilyGateTest {

	private static SimpleSerialHuntHandler handler;
	private static NativeQuestTableLoader loader;
	private static CameraRegistry cameraRegistry;

	@BeforeAll
	static void setUp() {
		loader = NativeQuestTableLoader.instance();
		cameraRegistry = CameraRegistry.instance();
		handler = SimpleSerialHuntHandler.instance();
	}

	@Test
	void allSixteenSerialHuntRowsCoveredByNativeHandler() {
		assertEquals(16, loader.serialHuntRows().size());
		assertEquals(16, handler.ownedQuestCount());
		for (NativeQuestTableLoader.SimpleSerialHuntRow row : loader.serialHuntRows()) {
			int questId = row.questId();
			assertTrue(handler.owns(questId), "handler must own serial quest " + questId);
			assertNotNull(handler.acquireNpc(questId), "acquire npc must resolve for quest " + questId);
			assertNotNull(handler.rewardNpc(questId), "reward npc must resolve for quest " + questId);

			CameraRegistry.CameraRow cam = cameraRegistry.require(questId);
			assertEquals(RawQuestVarsCodec.Width.SIX, cam.width());
			assertFalse(row.stages().isEmpty(), "stages must not be empty for quest " + questId);

			int expectedFullValue = 0;
			for (NativeQuestTableLoader.SerialStage stage : row.stages()) {
				expectedFullValue |= stage.count() << cam.width().shift(stage.stage());
				assertEquals(stage.count(), cam.required(stage.stage()));
			}
			assertEquals(expectedFullValue, cam.fullValue());
		}
	}

	@Test
	void serialHuntStepLadderAndOutOfOrderKills() {
		// 任务 13025: Stage 1 需要 9 杀 (LDF5B_c3_Vri_* 801101 等), Stage 2 需要 1 杀 (LDF5B_c3_Vri_Confuse_El_SN_NmdQ_65_Ae -> 283151 等)
		int questId = 13025;
		Player player = createTestPlayer();
		QuestState qs = new QuestState(questId, QuestStatus.START, 0, 0, null, 0, null);
		player.getQuestStateList().addQuest(questId, qs);

		// 阶段 1 怪物: 801101
		Npc stage1Mob = createMockNpc(231327);
		// 阶段 2 怪物: 283151 (LDF5B_c3_Vri_Confuse_El_SN_NmdQ_65_Ae)
		Npc stage2Mob = createMockNpc(231333);

		// 1. 在 Stage 1 未满时击杀 Stage 2 怪物：乱序阻断，零动作
		QuestEnv envStage2KillPremature = new QuestEnv(stage2Mob, player, questId, 0);
		assertFalse(handler.onKill(envStage2KillPremature));
		assertEquals(0, qs.getQuestVars().getQuestVars());
		assertEquals(QuestStatus.START, qs.getStatus());

		// 2. 正常击杀 Stage 1 怪物：单次推进
		QuestEnv envStage1Kill = new QuestEnv(stage1Mob, player, questId, 0);
		assertTrue(handler.onKill(envStage1Kill));
		assertEquals(1, qs.getQuestVars().getQuestVars());

		// 3. 模拟打满 Stage 1 (9 杀, 即 vars = 9)
		qs.setQuestVar(9);

		// 4. 再次击杀 Stage 1 怪物：已满槽零动作
		assertFalse(handler.onKill(envStage1Kill));
		assertEquals(9, qs.getQuestVars().getQuestVars());

		// 5. 现在击杀 Stage 2 怪物：合法推进，且 Stage 2 打满 (1 杀) 触发全行满值翻转 REWARD (fullValue = 9 | (1 << 6) = 73)
		QuestEnv envStage2KillValid = new QuestEnv(stage2Mob, player, questId, 0);
		assertTrue(handler.onKill(envStage2KillValid));
		assertEquals(73, qs.getQuestVars().getQuestVars());
		assertEquals(QuestStatus.REWARD, qs.getStatus());

		// 6. 已是 REWARD 状态再次击杀：零动作
		assertFalse(handler.onKill(envStage2KillValid));
		assertEquals(73, qs.getQuestVars().getQuestVars());
		assertEquals(QuestStatus.REWARD, qs.getStatus());
	}

	@Test
	void briefingGateEnforcesTalkBeforeKills() {
		// 任务 30600: Hejitor (800325) 接取/交付, Linocus (800324) 简报
		int questId = 30600;
		// 需要包捕获（简报页/关窗断言）：用 fixture 玩家（Elyos，含 clientConnection 捕获）。
		// Packet capture is required here: use the fixture player (Elyos, recording connection).
		Player player = NativeTalkFixture.player();
		Npc acqNpc = createMockNpc(800325);
		Npc briefingNpc = createMockNpc(800324);
		Npc stage1Mob = createMockNpc(219256); // IDDreadgion_03_DrakanFiNamedAA_60_Ae

		// 1. 接取前向简报 NPC 对话：无效
		QuestEnv envBriefingBeforeAccept = new QuestEnv(briefingNpc, player, questId, 26);
		assertFalse(handler.onDialog(envBriefingBeforeAccept));

		// 2. 模拟任务接取并带有简报标志位 (0x40000000)
		QuestState qs = new QuestState(questId, QuestStatus.START, 0x40000000, 0, null, 0, null);
		player.getQuestStateList().addQuest(questId, qs);

		// 3. 简报未完成时击杀怪物：被 0x40000000 守卫阻断，零动作
		QuestEnv envKillWithGuard = new QuestEnv(stage1Mob, player, questId, 0);
		assertFalse(handler.onKill(envKillWithGuard));
		assertEquals(0x40000000, qs.getQuestVars().getQuestVars());

		// 4. 向简报 NPC 对话：任务行（QUEST_SELECT=31）→ 简报页 select2（1352，带 questId），
		//    状态不变（真端行 0a）；实机 2026-10-08：「点击任务无下一步」= 曾发通用页 10 循环。
		NativeTalkFixture.clearPackets(player);
		QuestEnv envBriefingDialog = new QuestEnv(briefingNpc, player, questId, 31);
		assertTrue(handler.onDialog(envBriefingDialog));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 1352, questId);
		assertEquals(0x40000000, qs.getQuestVars().getQuestVars(), "打开简报页不得清位");

		// 5. 简报页「结束对话」（SETPRO1=10000）→ 清 var5 + 关窗（真端行 0b close-dialog）。
		NativeTalkFixture.clearPackets(player);
		QuestEnv envBriefingConfirm = new QuestEnv(briefingNpc, player, questId, 10000);
		assertTrue(handler.onDialog(envBriefingConfirm));
		NativeTalkFixture.assertCloseDialog(player);
		assertEquals(0, qs.getQuestVars().getQuestVars());

		// 6. 简报完成后击杀怪物：合法推进
		assertTrue(handler.onKill(envKillWithGuard));
		assertEquals(1, qs.getQuestVars().getQuestVars());
	}

	@Test
	void nativeDialogFlowEndToEnd() {
		// 任务 41189: Ganofus (205565)
		int questId = 41189;
		Player player = createTestPlayer();
		Npc ganofus = createMockNpc(205565);

		// 1. 未接取状态：请求任务描述页 (4)
		QuestEnv envDesc = new QuestEnv(ganofus, player, questId, 26);
		assertTrue(handler.onDialog(envDesc));

		// 1b. 打开（-1，无任务上下文）不被接取面认领——归引擎开门规则（进行中重放，否则通用页 10
		// 列表；2026-10-05 同 DD 面修复，实机 NPC 834166 缺陷类别）。
		QuestEnv envOpen = new QuestEnv(ganofus, player, questId, -1);
		assertFalse(handler.onDialog(envOpen), "打开(-1) 不得被接取面认领");

		// 2. 拒绝接取 (1003)
		QuestEnv envRefuse = new QuestEnv(ganofus, player, questId, 1003);
		assertTrue(handler.onDialog(envRefuse));

		// 3. 进行中状态：向交付 NPC 说话弹出未完成提示 (10)
		QuestState qs = new QuestState(questId, QuestStatus.START, 0, 0, null, 0, null);
		player.getQuestStateList().addQuest(questId, qs);
		QuestEnv envInProgress = new QuestEnv(ganofus, player, questId, 31);
		assertTrue(handler.onDialog(envInProgress));

		// 4. 待交付状态 (REWARD)：展示奖励选择窗口 (5)
		qs.setStatus(QuestStatus.REWARD);
		QuestEnv envReward = new QuestEnv(ganofus, player, questId, 31);
		assertTrue(handler.onDialog(envReward));
	}

	/**
	 * 可重复行 COMPLETE 重开局（真端 {@code finishedcount < max_repeat_count}）：9622 是本族唯一
	 * max_repeat_count>1 的行（255）；完成后点任务行必须重新开放接取面并复位档案（含简报守卫位）。
	 * <p>
	 * Repeatable COMPLETE re-open: 9622 is the family's only row with max_repeat_count>1 (255); the
	 * accept face must reopen and the row must reset (briefing guard bit included) on re-accept.
	 */
	@Test
	void repeatableCompletedRowReopensTheAcceptFace() {
		Player player = NativeTalkFixture.player();
		QuestState state = NativeTalkFixture.add(player, 9622, QuestStatus.COMPLETE, 0);
		state.setCompleteCount(1);
		Integer acquireNpc = handler.acquireNpc(9622);
		assertNotNull(acquireNpc, "真端行必须有可解析的接取 NPC");

		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, acquireNpc, 9622, 31)),
			"可重复行 COMPLETE 态点任务行必须开放接取面");
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, NativeTalkFixture.clientEntryPage(9622), 9622);

		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, acquireNpc, 9622, 1002)),
			"重复接取收尾（0x3ea）必须由 native 接取口服务");
		assertEquals(QuestStatus.START, state.getStatus(), "重复接取必须复位为 START");
		assertEquals(0x40000000, state.getQuestVars().getQuestVars(),
			"该行带简报 NPC ⇒ 接取后置简报守卫位（既有语义不变）");
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 1003, 9622);

		// 非可重复行（30600：max_repeat_count=1）不受影响：COMPLETE 态仍不进接取面。
		QuestState single = NativeTalkFixture.add(player, 30600, QuestStatus.COMPLETE, 0);
		single.setCompleteCount(1);
		NativeTalkFixture.clearPackets(player);
		assertFalse(handler.onDialog(NativeTalkFixture.dialog(player, handler.acquireNpc(30600), 30600, 31)),
			"max_repeat_count=1 的行 COMPLETE 态不得开放接取面");
		assertTrue(NativeTalkFixture.dialogPages(player).isEmpty(), "非可重复行不得下发接取页");
	}

	private static Player createTestPlayer() {
		Player player = new ObjenesisStd().newInstance(Player.class);
		PlayerCommonData pcd = new PlayerCommonData(10001);
		pcd.setRace(Race.ELYOS);
		pcd.setGender(Gender.MALE);
		try {
			java.lang.reflect.Field f = Player.class.getDeclaredField("playerCommonData");
			f.setAccessible(true);
			f.set(player, pcd);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		QuestStateList qsl = new QuestStateList();
		player.setQuestStateList(qsl);
		return player;
	}

	private static Npc createMockNpc(int npcId) {
		Npc npc = new ObjenesisStd().newInstance(Npc.class);
		try {
			java.lang.reflect.Field fId = com.aionemu.gameserver.model.gameobjects.AionObject.class.getDeclaredField("objectId");
			fId.setAccessible(true);
			fId.set(npc, 9999000 + npcId);

			NpcTemplate template = new NpcTemplate();
			java.lang.reflect.Field fTemplateId = NpcTemplate.class.getDeclaredField("npcId");
			fTemplateId.setAccessible(true);
			fTemplateId.set(template, npcId);

			java.lang.reflect.Field fObjectTemplate = com.aionemu.gameserver.model.gameobjects.VisibleObject.class.getDeclaredField("objectTemplate");
			fObjectTemplate.setAccessible(true);
			fObjectTemplate.set(npc, template);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return npc;
	}
}
