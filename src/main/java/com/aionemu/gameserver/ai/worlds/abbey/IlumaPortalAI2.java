package com.aionemu.gameserver.ai.worlds.abbey;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Abbey 区域 NPC AI：Iluma Portal（@AIName "iluma_portal"），继承 NpcAI2。
 * Abbey zone NPC AI: Iluma Portal (@AIName "iluma_portal"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("iluma_portal")
public class IlumaPortalAI2 extends NpcAI2
{
	@Override
    protected void handleDialogStart(Player player) {
        if (player.getRace() == Race.ELYOS) {
		    QuestState qs = player.getQuestStateList().getQuestState(10521);
			if (qs == null || qs.getStatus() != QuestStatus.COMPLETE) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_CANT_OWN_NOT_COMPLETE_QUEST(10521));
				PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
			} else {
				PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getOwner().getObjectId(), 10));
			}
		}
    }
}
