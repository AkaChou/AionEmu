package com.aionemu.gameserver.ai.instance.darkPoeta;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Dark Poeta 副本 NPC AI：Wounded Scar（@AIName "wounded_scar"），继承 NpcAI2。
 * Dark Poeta instance NPC AI: Wounded Scar (@AIName "wounded_scar"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("wounded_scar")
public class Wounded_ScarAI2 extends NpcAI2
{
	@Override
	protected void handleDialogStart(Player player) {
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getOwner().getObjectId(), 1011));
	}
	
	@Override
    public boolean onDialogSelect(final Player player, int dialogId, int questId, int extendedRewardIndex) {
		if (dialogId == 10000) {
		    switch (getNpcId()) {
				case 214871: //Wounded Scar.
				break;
			}
		}
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
		return true;
	}
}
