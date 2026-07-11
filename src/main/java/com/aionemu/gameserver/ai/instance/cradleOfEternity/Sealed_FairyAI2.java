package com.aionemu.gameserver.ai.instance.cradleOfEternity;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Cradle Of Eternity 副本 NPC AI：Sealed Fairy（@AIName "Sealed_Fairy"），继承 NpcAI2。
 * Cradle Of Eternity instance NPC AI: Sealed Fairy (@AIName "Sealed_Fairy"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("Sealed_Fairy")
public class Sealed_FairyAI2 extends NpcAI2
{
	@Override
	protected void handleDialogStart(Player player) {
		if (player.isArchDaeva()) {
		    PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
		} else {
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 27));
        }
	}
	
	@Override
	public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		if (dialogId == 10000) {
			switch (getNpcId()) {
			    case 834009: //Sealed Fairy.
				    // 离开花园后，希尔法女王的力量将消失。 / Once you leave the garden, the Sylfae Queen’s power will disappear.
					PacketSendUtility.npcSendPacketTime(getOwner(), SM_SYSTEM_MESSAGE.STR_IDEternity_02_SYSTEM_MSG_41, 5000);
					GameEngineServices.skillEngine().applyEffectDirectly(21340, player, player, 3600000 * 1); //Sealed Fairy.
					GameEngineServices.skillEngine().applyEffectDirectly(21344, player, player, 3600000 * 1); //Beguiling Visions.
			    break;
			}
		}
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
		return true;
	}
}
