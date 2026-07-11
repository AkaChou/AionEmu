package com.aionemu.gameserver.ai.portals;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.TeleportAnimation;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 传送门/传送点 AI：Kaisinel Academy To Sanctum（@AIName "kats"），继承 NpcAI2。
 * Portal/teleporter AI: Kaisinel Academy To Sanctum (@AIName "kats"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("kats")
public class Kaisinel_Academy_To_SanctumAI2 extends NpcAI2
{
	@Override
	protected void handleDialogStart(Player player) {
		if (player.getLevel() >= 10) {
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
		}
	}
	
	@Override
    public boolean onDialogSelect(final Player player, int dialogId, int questId, int extendedRewardIndex) {
		if (dialogId == 10000) {
			TeleportService2.teleportTo(player, 110010000, 1366.7853f, 1539.8356f, 569.0382f, (byte) 91, TeleportAnimation.BEAM_ANIMATION);
		} else if (dialogId == 10001) {
			QuestState qs = player.getQuestStateList().getQuestState(10521); // ? .
			if (qs == null || qs.getStatus() != QuestStatus.COMPLETE) {
                PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_CANT_OWN_NOT_COMPLETE_QUEST(10521)); // ? .
				return true;
			} else {
                TeleportService2.teleportTo(player, 210100000, 1451.8922f, 1297.8304f, 335.6076f, (byte) 65, TeleportAnimation.BEAM_ANIMATION);
            }
		}
		return true;
	}
}
