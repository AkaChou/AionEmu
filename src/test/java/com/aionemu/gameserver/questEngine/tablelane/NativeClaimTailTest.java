package com.aionemu.gameserver.questEngine.tablelane;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.ai.QuestItemNpcAI2;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 领奖结算收尾分型（2026-10-17 实机 1141：酒桶结算后弹 load fail LF1a_Barrel.html）：
 * 物件型任务交互物（quest_use_item）结算收尾 = 关窗（SM_DIALOG_WINDOW(0,0)）——物件无
 * NPC 问候 html，页 10 在客户端必 load fail；普通 NPC 保持原版回选择对话页（页 10）。
 * <p>
 * Claim-tail typing (live 1141, 2026-10-17: the barrel load-failed LF1a_Barrel.html after
 * settlement): a quest-use-item object closes the window (SM_DIALOG_WINDOW(0,0)) because an
 * object has no npc hello html and the selection page load-fails it, while a plain npc keeps
 * the retail selection-dialog tail (page 10).
 */
class NativeClaimTailTest {

	@Test
	void questUseItemObjectsCloseTheWindowInsteadOfTheLoadFailingSelectionPage() throws Exception {
		Player player = NativeTalkFixture.player();
		Npc barrel = NativeTalkFixture.npc(700122);
		setAi2(barrel, new QuestItemNpcAI2());

		NativeClaimTail.send(player, barrel, 24463);
		assertEquals(List.of(0), NativeTalkFixture.dialogPages(player), "物件收尾 = 关窗页 0");
	}

	@Test
	void plainNpcsKeepTheRetailSelectionDialogTail() {
		Player player = NativeTalkFixture.player();
		Npc npc = NativeTalkFixture.npc(730001);

		NativeClaimTail.send(player, npc, 24458);
		assertEquals(List.of(QuestDialogPage.SELECT_QUEST.id()), NativeTalkFixture.dialogPages(player),
			"普通 NPC 收尾 = 页 10");
	}

	/** 反射写入 Creature.ai2（测试桩无框架初始化）。 / Reflectively sets Creature.ai2 (unframed stub). */
	private static void setAi2(Npc npc, Object value) throws Exception {
		for (Class<?> type = npc.getClass(); type != null; type = type.getSuperclass()) {
			try {
				Field field = type.getDeclaredField("ai2");
				field.setAccessible(true);
				field.set(npc, value);
				return;
			} catch (NoSuchFieldException ignored) {
				// 继续向父类找。 / Keep walking up.
			}
		}
		throw new AssertionError("ai2 field not found");
	}
}
