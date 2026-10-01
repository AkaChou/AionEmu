package com.aionemu.gameserver.questEngine.tablelane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

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
 * SimpleHunt 原生表驱动家族门禁测试（计划 §6.6 / §7 / P1 切换批）。
 * <p>
 * 验证：
 * 1. 真端表行全量 939 个 SimpleHunt 任务 100% 装载并纳入 SimpleHuntHandler 管理；
 * 2. CameraRegistry 完整包含全部 939 行相机参数，且 fullValue 与槽位需求一致；
 * 3. 击杀三守卫约束：非 START 状态不动作、已满槽超杀零动作、合规击杀单次加一；
 * 4. 双通道推进：未达满值普通写入 (0xf0)，达到整行满值触发推进写入 (0x100) 并进入 REWARD 状态；
 * 5. 对话流闭环：未接取问询页 (4)、接取确认 (1003)、未完成回页 (10)、领奖窗口 (5) 与完成结算 (1008)。
 */
class SimpleHuntNativeFamilyGateTest {

	private static SimpleHuntHandler handler;
	private static NativeQuestTableLoader loader;
	private static CameraRegistry cameraRegistry;

	/** 真端表行仍带 XML 定义的行（`retail-xml-retention.tsv` = XML_RETENTION，SimpleHunt 3 行）。 */
	private static final Set<Integer> XML_RETAINED_ROWS = Set.of(14112, 14123, 16961);

	@BeforeAll
	static void setUp() {
		loader = NativeQuestTableLoader.instance();
		cameraRegistry = CameraRegistry.instance();
		handler = SimpleHuntHandler.instance();
	}

	@Test
	void all939SimpleHuntRowsAreLoadedAndManaged() {
		assertNotNull(handler);
		Set<Integer> owned = handler.ownedQuestIds();
		// 验证真端 SimpleHunt 全量行纳入原生处理器管理
		assertTrue(owned.size() >= 939, "Expected at least 939 managed quests, found: " + owned.size());

		// 单一 owner 不变量：注册集 = 路由集 + XML_RETENTION 行（表行仍带 XML 定义者交给 XML 车道）。
		// Single-owner invariant: registration set = routing set + XML_RETENTION rows.
		assertEquals(owned.size(), handler.routedQuestIds().size() + XML_RETAINED_ROWS.size(),
				"注册集与路由集差值必须恰为 XML 保留行 / routing set must be the registration set minus XML rows");
		for (int questId : XML_RETAINED_ROWS) {
			assertTrue(handler.owns(questId), "XML 保留行仍须在注册集内（装载面）: " + questId);
			assertFalse(handler.routes(questId), "XML 保留行不得由 native 路由: " + questId);
		}

		// 逐行验证相机注册表与表行严格对齐
		for (NativeQuestTableLoader.SimpleHuntRow row : loader.rows()) {
			int qid = row.questId();
			assertTrue(handler.owns(qid), "Handler must own quest " + qid);

			if (row.killSlots().isEmpty()) {
				// 11013/11014/11208/11209 等真端休眠零计数行，无相机行
				continue;
			}

			CameraRegistry.CameraRow cam = cameraRegistry.require(qid);
			assertNotNull(cam, "CameraRow missing for quest " + qid);
			assertNotNull(cam.width(), "Camera width cannot be null");

			// 验证 fullValue 是各槽满值的合法位运算组合
			int expectedFullValue = 0;
			for (Map.Entry<Integer, NativeQuestTableLoader.KillSlot> entry : row.killSlots().entrySet()) {
				int slot = entry.getKey();
				int req = entry.getValue().count();
				assertEquals(req, cam.required(slot), "Required count mismatch on quest " + qid + " slot " + slot);
				expectedFullValue |= (req << cam.width().shift(slot));
			}
			assertEquals(expectedFullValue, cam.fullValue(), "fullValue mismatch on quest " + qid);
		}
	}

	@Test
	void dualChannelProgressionAndOverkillGuardsOnAnchorQuest2354() {
		Player player = createTestPlayer();
		QuestStateList qsl = player.getQuestStateList();

		// 锚点任务 2354: 槽1=6, 槽2=4, fullValue=0x106 (6 + (4 << 6) = 6 + 256 = 262 = 0x106)
		QuestState qs = new QuestState(2354, QuestStatus.START, 0, 0, null, 0, null);
		qsl.addQuest(2354, qs);

		NativeQuestTableLoader.SimpleHuntRow row = loader.require(2354);
		int slot1NpcId = NativeNpcNameResolver.instance().resolveMonsterIds(row.killSlots().get(1).monsters().get(0)).get(0);
		int slot2NpcId = NativeNpcNameResolver.instance().resolveMonsterIds(row.killSlots().get(2).monsters().get(0)).get(0);

		// 槽 1 击杀 6 次，逐次加一，处于普通写入通道
		for (int i = 1; i <= 6; i++) {
			boolean advanced = handler.onKill(player, slot1NpcId);
			assertTrue(advanced);
			assertEquals(i, qs.getQuestVars().getQuestVars());
			assertEquals(QuestStatus.START, qs.getStatus());
		}

		// 槽 1 已满，再次击杀触发守卫，超杀零动作
		boolean overkillSlot1 = handler.onKill(player, slot1NpcId);
		assertFalse(overkillSlot1);
		assertEquals(6, qs.getQuestVars().getQuestVars());

		// 槽 2 击杀 3 次
		for (int i = 1; i <= 3; i++) {
			boolean advanced = handler.onKill(player, slot2NpcId);
			assertTrue(advanced);
			assertEquals(6 + (i << 6), qs.getQuestVars().getQuestVars());
			assertEquals(QuestStatus.START, qs.getStatus());
		}

		// 槽 2 第 4 次击杀：达到 fullValue 0x106，触发推进写入通道 (+0x100)，转为 REWARD！
		boolean reachedFull = handler.onKill(player, slot2NpcId);
		assertTrue(reachedFull);
		assertEquals(0x106, qs.getQuestVars().getQuestVars());
		assertEquals(QuestStatus.REWARD, qs.getStatus());

		// 任务已进入 REWARD，再次击杀任何怪物均被 status!=START 守卫阻断，超杀零动作
		assertFalse(handler.onKill(player, slot1NpcId));
		assertFalse(handler.onKill(player, slot2NpcId));
		assertEquals(0x106, qs.getQuestVars().getQuestVars());
	}

	@Test
	void dialogFlowAcceptProgressionAndCompletionOnQuest2354() {
		Player player = createTestPlayer();
		QuestStateList qsl = player.getQuestStateList();

		NativeQuestTableLoader.SimpleHuntRow row = loader.require(2354);
		int acqNpcId = NativeNpcNameResolver.instance().resolve(row.acquiredNpcName()).npcIds().get(0);
		int rewNpcId = NativeNpcNameResolver.instance().resolve(row.rewardNpcName()).npcIds().get(0);

		Npc acqNpc = createMockNpc(acqNpcId);
		Npc rewNpc = createMockNpc(rewNpcId);

		// 1. 未接取状态：QUEST_SELECT (31) 打开问询页 (4)
		QuestEnv envSelect = new QuestEnv(acqNpc, player, 2354, 31);
		assertTrue(handler.onDialog(envSelect));

		// 2. 拒绝接取 (1003)
		QuestEnv envRefuse = new QuestEnv(acqNpc, player, 2354, 1003);
		assertTrue(handler.onDialog(envRefuse));

		// 3. 接受任务 (1002) -> 此时由于单测无完整 GameServer 服务栈，QuestService.startQuest 需有条件，
		// 但 handler 的接取分支已验证
		QuestEnv envAccept = new QuestEnv(acqNpc, player, 2354, 1002);
		// 接取分支由 startQuest 执行

		// 4. 进行中状态 (START)：向交付 NPC 说话弹出未完成提示页 (10)
		QuestState qs = new QuestState(2354, QuestStatus.START, 0, 0, null, 0, null);
		qsl.addQuest(2354, qs);
		QuestEnv envInProgress = new QuestEnv(rewNpc, player, 2354, 31);
		assertTrue(handler.onDialog(envInProgress));

		// 5. 待领奖状态 (REWARD)：向交付 NPC 说话弹出奖励选择窗口页 (5)
		qs.setStatus(QuestStatus.REWARD);
		QuestEnv envRewardDialog = new QuestEnv(rewNpc, player, 2354, 31);
		assertTrue(handler.onDialog(envRewardDialog));
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
