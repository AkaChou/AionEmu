package com.aionemu.gameserver.ai.instance.theEternalBastion;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * The Eternal Bastion 副本 NPC AI：Beritran Chariot（@AIName "beritran_chariot"），继承 NpcAI2。
 * The Eternal Bastion instance NPC AI: Beritran Chariot (@AIName "beritran_chariot"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("beritran_chariot")
public class Beritran_ChariotAI2 extends NpcAI2
{
	@Override
    protected void handleDialogStart(Player player) {
        if (player.getInventory().getFirstItemByItemId(185000137) != null) { //Mobile Turret Key.
        // 移动炮塔钥匙。 / Mobile Turret Key.
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 10));
        } else {
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 27));
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_IDLDF5B_TD_Tank);
        }
    }
	
	@Override
    public boolean onDialogSelect(final Player player, int dialogId, int questId, int extendedRewardIndex) {
		if (dialogId == 10000 && player.getInventory().decreaseByItemId(185000137, 1)) { //Mobile Turret Key.
		// 移动炮塔钥匙。 / Mobile Turret Key.
		    switch (getNpcId()) {
			    case 701624: //Beritran Chariot.
			    // 贝里特兰战车。 / Beritran Chariot.
				case 702689: //Beritran Chariot.
				// 贝里特兰战车。 / Beritran Chariot.
				    GameEngineServices.skillEngine().getSkill(player, 21141, 1, player).useNoAnimationSkill();
				break;
			}
		}
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
		AI2Actions.deleteOwner(this);
		return true;
	}
}
