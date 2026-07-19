package com.aionemu.gameserver.ai.portals;

import com.aionemu.gameserver.ai.GeneralNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.autogroup.MatchDefinition;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_FIND_GROUP;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 传送门/传送点 AI：Advance Corridor For Distinguished Service（@AIName "advance_corridor_distinguished"），继承 GeneralNpcAI2。
 * Portal/teleporter AI: Advance Corridor For Distinguished Service (@AIName "advance_corridor_distinguished"), extends GeneralNpcAI2.
 *
 * @author Encom
 */
@AIName("advance_corridor_distinguished")
public class Advance_Corridor_For_Distinguished_ServiceAI2 extends GeneralNpcAI2
{
	@Override
	protected void handleDialogStart(Player player) {
        switch (getNpcId()) {
			case 730967: //Advance Corridor For Distinguished Service.
			case 730968: { //Advance Corridor For Distinguished Service.
				super.handleDialogStart(player);
				break;
			} default: {
				if (player.getLevel() >= 65) {
				    PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getOwner().getObjectId(), 10));
				}
				break;
			}
		}
	}
	
	@Override
	public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		if (dialogId == 105) {
			switch (getNpcId()) {
			    case 730967: //Advance Corridor For Distinguished Service.
				case 730968: //Advance Corridor For Distinguished Service.
				    MatchDefinition agt = MatchDefinition.forNpc(player.getLevel(), getNpcId());
					if (agt != null) {
					    PacketSendUtility.sendPacket(player, new SM_FIND_GROUP(0x1A, agt.getInstanceMapId()));
					}
				break;
			}
		} else if (dialogId == 10) {
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 10));
        }
        return true;
    }
}
