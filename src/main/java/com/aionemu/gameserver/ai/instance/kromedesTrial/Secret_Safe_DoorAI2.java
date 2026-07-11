package com.aionemu.gameserver.ai.instance.kromedesTrial;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Kromedes Trial 副本 NPC AI：Secret Safe Door（@AIName "secret_safe_door"），继承 NpcAI2。
 * Kromedes Trial instance NPC AI: Secret Safe Door (@AIName "secret_safe_door"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("secret_safe_door")
public class Secret_Safe_DoorAI2 extends NpcAI2
{
	@Override
    protected void handleDialogStart(Player player) {
        if (player.getInventory().getFirstItemByItemId(185000101) != null) { //Secret Safe Key.
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1352));
        } else {
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
        }
    }
	
	@Override
	public boolean onDialogSelect(final Player player, int dialogId, int questId, int extendedRewardIndex) {
		int instanceId = getPosition().getInstanceId();
		if (dialogId == 10001) { //Secret Safe Key.
		    switch (getNpcId()) {
			    case 700924: //Secret Safe Door.
				    TeleportService2.teleportTo(player, 300230000, instanceId, 593.46f, 774.000f, 215.58f, (byte) 0);
				break;
			}
		}
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
		return true;
	}
}
