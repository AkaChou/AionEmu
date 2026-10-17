package com.aionemu.gameserver.questEngine.tablelane;

import com.aionemu.gameserver.ai.QuestItemNpcAI2;
import com.aionemu.gameserver.ai.quests.QuestStartItemNpcAi2;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.services.DialogService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 领奖结算收尾（native 八族共用）：普通 NPC 回选择对话页（页 10，questId=0；原版
 * npc-complete finish=SELECTION_DIALOG，9/28 基线）；物件型任务交互物
 * （{@code quest_use_item} / 起始物件）没有 NPC 问候 html，页 10 在客户端必 load fail
 * （2026-10-17 实机 1141 右键 LF1a_Barrel 700122 结算后弹 load fail）——改关窗收尾。
 * <p>
 * Claim settlement tail shared by the native families: a plain npc returns to the selection
 * dialog (page 10, questId=0; retail npc-complete finish=SELECTION_DIALOG, 9/28 baseline),
 * while a quest-use-item object has no npc hello html and load-fails that page (live 1141 on
 * the LF1a_Barrel 700122, 2026-10-17), so it closes the dialog instead.
 */
final class NativeClaimTail {

	private NativeClaimTail() {
	}

	/**
	 * 按交互目标对象 ID 解析 NPC 后下发收尾（解析不到退化为普通 NPC 分支）。
	 * Resolves the npc from the interaction target object id, then delegates; an unresolvable
	 * id degrades to the plain-npc branch.
	 */
	static void send(Player player, int targetObjectId) {
		VisibleObject target = targetObjectId > 0 && player.getKnownList() != null
			? player.getKnownList().getObject(targetObjectId) : null;
		send(player, target instanceof Npc npc ? npc : null, targetObjectId);
	}

	/**
	 * 按交互目标下发结算收尾：物件关窗，普通 NPC 回任务列表页。
	 * Sends the settlement tail for the interaction target: objects get a window close,
	 * plain npcs return to the quest-list page.
	 */
	static void send(Player player, Npc npc, int targetObjectId) {
		// getAi2IfPresent 而非 getAi2：桩/未初始化对象不得触发 AI2Engine 惰性解析（DialogService 同款）。
		// getAi2IfPresent over getAi2: stubs must not trigger lazy AI2Engine resolution (as DialogService).
		if (npc != null && (npc.getAi2IfPresent() instanceof QuestItemNpcAI2
				|| npc.getAi2IfPresent() instanceof QuestStartItemNpcAi2)) {
			DialogService.closeDialog(npc, player);
			return;
		}
		PacketSendUtility.sendPacket(player,
			new SM_DIALOG_WINDOW(targetObjectId, QuestDialogPage.SELECT_QUEST.id()));
	}
}
