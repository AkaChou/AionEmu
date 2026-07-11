package com.aionemu.gameserver.ai.instance.kromedesTrial;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Kromedes Trial 副本 NPC AI：Grave Robber Corpse（@AIName "Grave_Robber_Corpse"），继承 NpcAI2。
 * Kromedes Trial instance NPC AI: Grave Robber Corpse (@AIName "Grave_Robber_Corpse"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("Grave_Robber_Corpse")
public class Grave_Robber_CorpseAI2 extends NpcAI2
{
    @Override
    protected void handleDialogStart(Player player) {
        if (player.getInventory().getItemCountByItemId(164000141) > 0) { //Silver Blade Rotan.
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1097));
        } else {
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
        }
    }
	
	@Override
    public boolean onDialogSelect(final Player player, int dialogId, int questId, int extendedRewardIndex) {
		if (dialogId == 1012) {
			ItemService.addItem(player, 164000141, 1); //Silver Blade Rotan.
            // 你获得了强大物品。可从背包拖到快捷栏以便使用。 / You have obtained an object with great power. For quick access, drag the item from your Cube to your Quickbar.
            PacketSendUtility.npcSendPacketTime(getOwner(), SM_SYSTEM_MESSAGE.STR_MSG_IDCROMEDE_SKILL_01, 0);
			// 可用银刃罗坦摧毁通往神殿宝库的石门。 / You can use a Silver Blade Rotan to destroy the rock door leading to the Temple Vault.
			PacketSendUtility.npcSendPacketTime(getOwner(), SM_SYSTEM_MESSAGE.STR_MSG_IDCROMEDE_DOOR, 10000);
        }
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
        return true;
    }
}
