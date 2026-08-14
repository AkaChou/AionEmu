package com.aionemu.gameserver.ai.instance.seizedDanuarSanctuary;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Seized Danuar Sanctuary 副本 NPC AI：The Charnel Entrance（@AIName "charnel"），继承 NpcAI2。
 * Seized Danuar Sanctuary instance NPC AI: The Charnel Entrance (@AIName "charnel"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("charnel")
public class TheCharnelEntranceAI2 extends NpcAI2
{
	@Override
    protected void handleDialogStart(Player player) {
        if (player.getInventory().getFirstItemByItemId(185000183) != null) { // 藏骸所钥匙。 / The Charnels Key.
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
        } else {
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 27));
        }
    }
	
	@Override
    public boolean onDialogSelect(final Player player, int dialogId, int questId, int extendedRewardIndex) {
		int instanceId = getPosition().getInstanceId();
		switch (getNpcId()) {
		    case 701871: // 藏骸所入口。 / The Charnel Entrance.
		        switch (player.getWorldId()) {
		            case 301140000: // 被占领的达努阿尔圣所 4.8 / Seized Danuar Sanctuary 4.8
				        if (dialogId == 10000 && player.getInventory().decreaseByItemId(185000183, 1)) { // 藏骸所钥匙。 / The Charnels Key.
			                TeleportService2.teleportTo(player, 301140000, instanceId, 1005.93787f, 1366.8308f, 337.25f, (byte) 113);
					    }
				    break;
			    } switch (player.getWorldId()) {
				    case 301380000: // 达努阿尔圣所 4.8 / Danuar Sanctuary 4.8
					    if (dialogId == 10000 && player.getInventory().decreaseByItemId(185000183, 1)) { // 藏骸所钥匙。 / The Charnels Key.
					        TeleportService2.teleportTo(player, 301380000, instanceId, 1005.93787f, 1366.8308f, 337.25f, (byte) 113);
						}
					break;
				}
			break;
		}
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
		return true;
	}
}
