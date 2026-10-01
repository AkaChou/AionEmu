package com.aionemu.gameserver.questEngine.tablelane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * {@link SimpleHuntHandler} 原生任务处理器单元测试。
 * 验证真端表装载、目标索引映射、相机击杀推进、双通道状态演进与超杀静默规范。
 */
class SimpleHuntHandlerTest {

	@Test
	void initializesManagedQuests() {
		SimpleHuntHandler handler = SimpleHuntHandler.instance();
		assertNotNull(handler);
		assertTrue(handler.ownedQuestIds().size() > 900);
		// 验证 P0a 锚点任务 2354 包含在管理集合中
		assertTrue(handler.owns(2354));
	}

	@Test
	void advancesProgressCameraOnMonsterKill() {
		SimpleHuntHandler handler = SimpleHuntHandler.instance();

		// 构造测试玩家
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

		// 任务 2354: 双槽 6 位相机 (槽1: 6, 槽2: 4, fullValue: 0x106)
		QuestState qs = new QuestState(2354, QuestStatus.START, 0, 0, null, 0, null);
		qs.getQuestVars().setVar(0);
		qsl.addQuest(2354, qs);

		// 获取任务 2354 槽 1 对应怪物的 NPC ID（通过 NativeQuestTableLoader 与 NativeNpcNameResolver）
		NativeQuestTableLoader.SimpleHuntRow row = NativeQuestTableLoader.instance().require(2354);
		String slot1Monster = row.killSlots().get(1).monsters().get(0);
		int slot1NpcId = NativeNpcNameResolver.instance().resolveMonsterIds(slot1Monster).get(0);

		// 1. 击杀 1 次槽 1 怪物：普通写入通道，vars 从 0 增加到 1，状态保持 START
		boolean killed1 = handler.onKill(player, slot1NpcId);
		assertTrue(killed1);
		assertEquals(1, qs.getQuestVars().getQuestVars());
		assertEquals(QuestStatus.START, qs.getStatus());

		// 2. 连续击杀 5 次，槽 1 满（达到 6 杀，vars 槽 1 = 6）
		for (int i = 0; i < 5; i++) {
			handler.onKill(player, slot1NpcId);
		}
		assertEquals(6, qs.getQuestVars().getQuestVars());
		assertEquals(QuestStatus.START, qs.getStatus());

		// 3. 槽 1 已满后超杀：守卫触发，超杀零动作
		boolean overkill = handler.onKill(player, slot1NpcId);
		assertFalse(overkill);
		assertEquals(6, qs.getQuestVars().getQuestVars());

		// 4. 击杀槽 2 怪物推进到满值
		String slot2Monster = row.killSlots().get(2).monsters().get(0);
		int slot2NpcId = NativeNpcNameResolver.instance().resolveMonsterIds(slot2Monster).get(0);

		// 击杀槽 2 前 3 次
		for (int i = 0; i < 3; i++) {
			handler.onKill(player, slot2NpcId);
		}
		// 此时槽 1 = 6, 槽 2 = 3，vars = 6 + (3 << 6) = 6 + 192 = 198 (0xc6)
		assertEquals(0xc6, qs.getQuestVars().getQuestVars());
		assertEquals(QuestStatus.START, qs.getStatus());

		// 第 4 次击杀槽 2：达到满值 (0x106)！触发推进写入通道（ADVANCE_WRITE），状态变为 REWARD！
		boolean reachedFull = handler.onKill(player, slot2NpcId);
		assertTrue(reachedFull);
		assertEquals(0x106, qs.getQuestVars().getQuestVars());
		assertEquals(QuestStatus.REWARD, qs.getStatus());

		// 5. 任务进入 REWARD 之后，再次击杀怪物：由于 status != START，相机守卫直接阻断，超杀静默
		boolean postRewardKill = handler.onKill(player, slot2NpcId);
		assertFalse(postRewardKill);
		assertEquals(0x106, qs.getQuestVars().getQuestVars());
		assertEquals(QuestStatus.REWARD, qs.getStatus());
	}
}
