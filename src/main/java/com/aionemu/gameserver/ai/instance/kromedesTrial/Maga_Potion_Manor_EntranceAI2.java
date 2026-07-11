package com.aionemu.gameserver.ai.instance.kromedesTrial;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Kromedes Trial 副本 NPC AI：Maga Potion Manor Entrance（@AIName "maga_potion_2"），继承 NpcAI2。
 * Kromedes Trial instance NPC AI: Maga Potion Manor Entrance (@AIName "maga_potion_2"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("maga_potion_2")
public class Maga_Potion_Manor_EntranceAI2 extends NpcAI2
{
    @Override
    protected void handleDialogStart(Player player) {
        PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
    }
	
	@Override
    public boolean onDialogSelect(final Player player, int dialogId, int questId, int extendedRewardIndex) {
        if (dialogId == 1012) {
            switch (getNpcId()) {
                case 730341: //Maga's Potion.
                if (player.getInventory().getItemCountByItemId(164000143) < 1) {
                    PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1012));
                    PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400701));
                    ItemService.addItem(player, 164000143, 1);
                } else {
                    PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 27));
                }
                break;
            }
        }
        return true;
    }
}
