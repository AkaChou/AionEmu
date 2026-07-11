package com.aionemu.gameserver.ai.siege;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 攻城战相关 NPC AI：Ab1 1011 Sim Weak Seller Li（@AIName "Ab1_1011_Sim_Weak_Seller_Li"），继承 NpcAI2。
 * Siege-related NPC AI: Ab1 1011 Sim Weak Seller Li (@AIName "Ab1_1011_Sim_Weak_Seller_Li"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("Ab1_1011_Sim_Weak_Seller_Li")
public class Ab1_1011_Sim_Weak_Seller_LiAI2 extends NpcAI2
{
	@Override
	protected void handleDialogStart(Player player) {
		if (player.getLevel() >= 66) {
		    PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getOwner().getObjectId(), 1011));
		} else {
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1182));
        }
	}
	
	@Override
    public boolean onDialogSelect(final Player player, int dialogId, int questId, int extendedRewardIndex) {
		if (dialogId == 10000 && player.getInventory().decreaseByItemId(182400001, 3000)) {
		    switch (getNpcId()) {
				case 273425:
				case 273427:
		        case 273429:
				    //to do...
				break;
			}
		} else if (dialogId == 1012) {
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1012));
        }
		return true;
	}
}
