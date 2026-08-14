package com.aionemu.gameserver.ai.instance.kromedesTrial;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Kromedes Trial 副本 NPC AI：Maga Potion Temple Vault（@AIName "maga_potion_1"），继承 NpcAI2。
 * Kromedes Trial instance NPC AI: Maga Potion Temple Vault (@AIName "maga_potion_1"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("maga_potion_1")
public class Maga_Potion_Temple_VaultAI2 extends NpcAI2
{
	@Override
    protected void handleDialogStart(Player player) {
        if (player.getInventory().getFirstItemByItemId(185000109) != null) { // 遗物钥匙 / Relic Key.
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
        } else {
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 27));
        }
    }
	
	@Override
	public boolean onDialogSelect(final Player player, int dialogId, int questId, int extendedRewardIndex) {
		int instanceId = getPosition().getInstanceId();
		if (dialogId == 10000) { // 遗物钥匙 / Relic Key.
		    switch (getNpcId()) {
			    case 730308: // 玛加药剂 / Maga's Potion.
					TeleportService2.teleportTo(player, 300230000, instanceId, 687.56116f, 681.68225f, 200.28648f, (byte) 30);
				break;
			}
		} else if (dialogId == 1012) {
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1012));
		}
		return true;
	}
}
