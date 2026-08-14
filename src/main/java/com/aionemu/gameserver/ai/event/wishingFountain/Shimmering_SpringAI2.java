package com.aionemu.gameserver.ai.event.wishingFountain;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Wishing Fountain 活动 NPC AI：Shimmering Spring（@AIName "shimmering_spring"），继承 NpcAI2。
 * Wishing Fountain event NPC AI: Shimmering Spring (@AIName "shimmering_spring"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("shimmering_spring")
public class Shimmering_SpringAI2 extends NpcAI2
{
	@Override
    protected void handleDialogStart(Player player) {
        if (player.getInventory().getFirstItemByItemId(186000406) != null) { //[Event] Petal Of Magic.
            //[活动] 魔法花瓣 / [Event] Petal Of Magic.
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 10));
        } else {
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
        }
    }
	
	@Override
    public boolean onDialogSelect(final Player player, int dialogId, int questId, int extendedRewardIndex) {
		if (dialogId == 10000 && player.getInventory().decreaseByItemId(186000406, 1)) { //[Event] Petal Of Magic.
			//[活动] 魔法花瓣 / [Event] Petal Of Magic.
		    switch (getNpcId()) {
		        case 833501: //Shimmering Spring.
			        // 微光之泉。 / Shimmering Spring.
				    switch (Rnd.get(1, 3)) {
					    case 1:
						    ItemService.addItem(player, 186000407, 1);
						break;
						case 2:
						    ItemService.addItem(player, 188054028, 1);
						break;
						case 3:
						    ItemService.addItem(player, 188054029, 1);
						break;
					}
				break;
			}
		}
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
		return true;
	}
}
