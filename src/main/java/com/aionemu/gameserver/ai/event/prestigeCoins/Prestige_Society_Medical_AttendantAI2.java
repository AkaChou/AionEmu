package com.aionemu.gameserver.ai.event.prestigeCoins;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.ChatType;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MESSAGE;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Prestige Coins 活动 NPC AI：Prestige Society Medical Attendant（@AIName "prestige_society_medical_attendant"），继承 NpcAI2。
 * Prestige Coins event NPC AI: Prestige Society Medical Attendant (@AIName "prestige_society_medical_attendant"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("prestige_society_medical_attendant")
public class Prestige_Society_Medical_AttendantAI2 extends NpcAI2
{
	@Override
	protected void handleDialogStart(Player player) {
        if (player.getInventory().getFirstItemByItemId(186000344) != null) { //Prestige Crystal.
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 10));
        } else {
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
			PacketSendUtility.broadcastPacket(player, new SM_MESSAGE(player,
			"You do not have <Prestige Crystal> to exchange", ChatType.BRIGHT_YELLOW_CENTER), true);
        }
    }
	
	@Override
    public boolean onDialogSelect(final Player player, int dialogId, int questId, int extendedRewardIndex) {
		if (dialogId == 10000 && player.getInventory().decreaseByItemId(186000344, 1)) { //Prestige Coin.
		    switch (getNpcId()) {
		        case 833764: //Prestige Society Medical Attendant.
				    GameEngineServices.skillEngine().applyEffectDirectly(21650, player, player, 1800000 * 1); //Prestigious Blessing.
				break;
			}
		}
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
		return true;
	}
}
