package com.aionemu.gameserver.ai.event.pumpkinKingHaunt;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai.GeneralNpcAI2;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Pumpkin King Haunt 活动 NPC AI：Buff Coffin（@AIName "Buff_Coffin"），继承 GeneralNpcAI2。
 * Pumpkin King Haunt event NPC AI: Buff Coffin (@AIName "Buff_Coffin"), extends GeneralNpcAI2.
 *
 * @author Encom
 */
@AIName("Buff_Coffin")
public class Buff_CoffinAI2 extends GeneralNpcAI2
{
  	@Override
    protected void handleDialogStart(Player player) {
        PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
    }
	
	@Override
    public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		if (dialogId == 10000) {
			switch (getNpcId()) {
			    case 835989: //Buff Coffin.
					GameEngineServices.skillEngine().applyEffectDirectly(11385, player, player, 3600000 * 1);
				break;
			}
		}
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
		AI2Actions.deleteOwner(this);
		AI2Actions.scheduleRespawn(this);
        return true;
    }
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
