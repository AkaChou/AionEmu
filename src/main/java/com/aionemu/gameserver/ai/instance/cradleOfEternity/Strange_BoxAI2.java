package com.aionemu.gameserver.ai.instance.cradleOfEternity;

import com.aionemu.gameserver.ai.GeneralNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Cradle Of Eternity 副本 NPC AI：Strange Box（@AIName "Strange_Box"），继承 GeneralNpcAI2。
 * Cradle Of Eternity instance NPC AI: Strange Box (@AIName "Strange_Box"), extends GeneralNpcAI2.
 *
 * @author Encom
 */
@AIName("Strange_Box")
public class Strange_BoxAI2 extends GeneralNpcAI2
{
	@Override
	protected void handleDialogStart(Player player) {
		if (player.isArchDaeva()) {
		    PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
		}
	}
	
	@Override
	public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		if (dialogId == 10000) {
			switch (getNpcId()) {
				case 834140: //Strange Box.
				case 834153: //Strange Box.
			    break;
			}
		}
		AI2Actions.deleteOwner(this);
		return true;
	}
}
