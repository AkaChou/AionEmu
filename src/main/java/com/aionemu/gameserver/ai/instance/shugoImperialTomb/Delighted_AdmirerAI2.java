package com.aionemu.gameserver.ai.instance.shugoImperialTomb;

import com.aionemu.gameserver.ai.GeneralNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Shugo Imperial Tomb 副本 NPC AI：Delighted Admirer（@AIName "Delighted_Admirer"），继承 GeneralNpcAI2。
 * Shugo Imperial Tomb instance NPC AI: Delighted Admirer (@AIName "Delighted_Admirer"), extends GeneralNpcAI2.
 *
 * @author Encom
 */
@AIName("Delighted_Admirer")
public class Delighted_AdmirerAI2 extends GeneralNpcAI2
{
	@Override
    protected void handleDialogStart(Player player) {
        PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
    }
	
	@Override
    public boolean onDialogSelect(final Player player, int dialogId, int questId, int extendedRewardIndex) {
        int instanceId = getPosition().getInstanceId();
		if (dialogId == 10001) {
            switch (getNpcId()) {
				case 831114: //Crown Prince's Delighted Admirer.
				    TeleportService2.teleportTo(player, 300560000, instanceId, 347.85843f, 424.8407f, 294.75983f, (byte) 57);
				break;
				case 831115: //Empress's Delighted Admirer.
				    TeleportService2.teleportTo(player, 300560000, instanceId, 461.9911f, 109.995865f, 214.7108f, (byte) 68);
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
