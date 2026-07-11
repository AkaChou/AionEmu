package com.aionemu.gameserver.ai.npcSupport;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.ChatType;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MESSAGE;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * NPC 支援/增益 AI：Blessed Relics（@AIName "blessed_relic"），继承 NpcAI2。
 * NPC support/buff AI: Blessed Relics (@AIName "blessed_relic"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("blessed_relic")
public class Blessed_RelicsAI2 extends NpcAI2
{
    @Override
	protected void handleDialogStart(Player player) {
        if (player.getInventory().getFirstItemByItemId(186000344) != null) { //Prestige Crystal.
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 10));
        } else {
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
			PacketSendUtility.broadcastPacket(player, new SM_MESSAGE(player,
			"You must have 1 <Prestige Crystal>", ChatType.BRIGHT_YELLOW_CENTER), true);
        }
    }
	
	@Override
    public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		if (dialogId == 10000 && player.getInventory().decreaseByItemId(186000344, 1)) { //Prestige Crystal.
			switch (getNpcId()) {
			    case 831987: //Lesser Blessed Relics. 
				case 831988: //Minor Blessed Relics.
				case 831989: //Major Blessed Relics.
				case 831990: //Greater Blessed Relics.
					GameEngineServices.skillEngine().applyEffectDirectly(21650, player, player, 1800000 * 1); //Prestigious Blessing.
				break;
			}
		}
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
        return true;
    }
}
