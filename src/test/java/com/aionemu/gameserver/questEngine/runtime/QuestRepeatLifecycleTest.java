package com.aionemu.gameserver.questEngine.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.ProductionQuestDefinitions;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.tablelane.NativeTalkFixture;
import com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler;

/**
 * 重复任务生命周期：typed 行（15476）与原版 native 行（1963，可重复谈资任务）都必须能从 COMPLETE 态再次接取。
 * <p>
 * P3 重锚（计划 §8.9）：1963 的旧 IR 形状断言（repeat 别名边 / 1007 中转）随 SimpleTalk 切换批退场，
 * 改为原版表行 + native 状态端口断言；接取窗页由族级页阶梯（4/1352/1693/2034/5/1008）承担。
 * <p>
 * Repeat lifecycle: the typed row 15476 and the native retail row 1963 must both restart from COMPLETE.
 */
class QuestRepeatLifecycleTest {

	/** 原版行 1963：acquired/reward = Polyidus(203726)，中继 Phokas(203851)，接取发放并第 1 步回收 182206032。 */
	private static final int NATIVE_QUEST = 1963;
	private static final int NATIVE_NPC = 203726;
	private static final int NATIVE_ITEM = 182206032;

	@Test
	void repeatable1963ReopensItsAcceptWindowAndRestartsFromCompletedState() {
		// 用记录式假背包端口：1002 接取按原版 cab520 0x3ea 分支发放 give_item（182206032），
		// 静态 handler 的真物品端口在单测无 ItemData 服务栈。
		// The recording fake port: the 1002 accept grants the row's give_item per retail cab520
		// 0x3ea, while the static handler's real port needs the absent ItemData service stack.
		SimpleTalkHandler handler = NativeTalkFixture.handler(new NativeTalkFixture.RecordingInventory());
		Player player = createTestPlayer();
		Npc polyidus = createMockNpc(NATIVE_NPC);
		player.getQuestStateList().addQuest(NATIVE_QUEST,
				new QuestState(NATIVE_QUEST, QuestStatus.COMPLETE, 0x1f, 3, null, 0, null));

		// 原版表行事实：接取与交付同 NPC、一个中继步、接取发放且第 1 步回收同一工作物品。
		assertEquals(NATIVE_NPC, handler.acquireNpc(NATIVE_QUEST));
		assertEquals(NATIVE_NPC, handler.rewardNpc(NATIVE_QUEST));
		assertEquals(1, handler.relayCount(NATIVE_QUEST));
		assertEquals(new SimpleTalkHandler.ItemStack(NATIVE_ITEM, 1), handler.acceptGiveItem(NATIVE_QUEST));
		assertEquals(new SimpleTalkHandler.ItemStack(NATIVE_ITEM, 1), handler.stepRemoveItem(NATIVE_QUEST, 1));

		// COMPLETE 态（可重复）→ 接取窗照开；确认接取复位到 START 并清空 raw vars。
		assertTrue(handler.onDialog(new QuestEnv(polyidus, player, NATIVE_QUEST, 31)),
				"可重复行的 COMPLETE 态必须重开接取窗");
		assertTrue(handler.onDialog(new QuestEnv(polyidus, player, NATIVE_QUEST, 1002)),
				"重复接取必须落库到 START");
		QuestState state = player.getQuestStateList().getQuestState(NATIVE_QUEST);
		assertEquals(QuestStatus.START, state.getStatus());
		assertEquals(0, state.getQuestVars().getQuestVars(), "重复接取必须复位 raw vars");
		assertEquals(3, state.getCompleteCount(), "接取不改写重复计数（由完成流维护）");

		// 重复上限用尽（max_repeat_count=100）→ 接取被拒，状态不被改写。
		state.setStatus(QuestStatus.COMPLETE);
		state.setCompleteCount(100);
		assertFalse(handler.onDialog(new QuestEnv(polyidus, player, NATIVE_QUEST, 1002)),
				"达到 max_repeat_count 后必须拒绝接取");
		assertEquals(QuestStatus.COMPLETE, state.getStatus(), "被拒时不得改写状态");
	}

	private static CompiledQuestDefinition definition(int questId) {
		return ProductionQuestDefinitions.definitionInOverlay(questId);
	}

	private static Player createTestPlayer() {
		Player player = new ObjenesisStd().newInstance(Player.class);
		PlayerCommonData pcd = new PlayerCommonData(10001);
		pcd.setRace(Race.ELYOS);
		pcd.setGender(Gender.MALE);
		setField(pcd, PlayerCommonData.class, "playerClass", PlayerClass.WARRIOR);
		setField(pcd, PlayerCommonData.class, "level", 20);
		setField(player, Player.class, "playerCommonData", pcd);
		player.setQuestStateList(new QuestStateList());
		return player;
	}

	private static Npc createMockNpc(int npcId) {
		Npc npc = new ObjenesisStd().newInstance(Npc.class);
		setField(npc, com.aionemu.gameserver.model.gameobjects.AionObject.class, "objectId", 9999000 + npcId);
		NpcTemplate template = new NpcTemplate();
		setField(template, NpcTemplate.class, "npcId", npcId);
		setField(npc, com.aionemu.gameserver.model.gameobjects.VisibleObject.class, "objectTemplate", template);
		return npc;
	}

	private static void setField(Object target, Class<?> type, String name, Object value) {
		try {
			java.lang.reflect.Field field = type.getDeclaredField(name);
			field.setAccessible(true);
			field.set(target, value);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}
}
