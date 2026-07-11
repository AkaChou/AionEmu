package com.aionemu.gameserver.ai.event;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai.GeneralNpcAI2;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 活动事件 NPC AI：Lief（@AIName "lief"），继承 GeneralNpcAI2。
 * Event NPC AI: Lief (@AIName "lief"), extends GeneralNpcAI2.
 *
 * @author Encom
 */
@AIName("lief")
public class LiefAI2 extends GeneralNpcAI2
{
  	@Override
    protected void handleDialogStart(Player player) {
        PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 10));
    }
	
	@Override
    public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		if (dialogId == 10000) {
			switch (getNpcId()) {
			    case 835271: //Lief.
					GameEngineServices.skillEngine().applyEffectDirectly(11253, player, player, 7200000 * 1);
				break;
			}
		}
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
        return true;
    }
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
