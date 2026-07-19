package com.aionemu.gameserver.ai.portals;

import com.aionemu.gameserver.ai.GeneralNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.autogroup.MatchDefinition;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_FIND_GROUP;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
/** Helper themoose (Encom)

/**
 * 传送门/传送点 AI：Walk Of Honor（@AIName "walk_of_honor"），继承 GeneralNpcAI2。
 * Portal/teleporter AI: Walk Of Honor (@AIName "walk_of_honor"), extends GeneralNpcAI2.
 *
 * @author Encom
 */
@AIName("walk_of_honor")
public class Walk_Of_HonorAI2 extends GeneralNpcAI2
{
	@Override
	protected void handleDialogStart(Player player) {
        switch (getNpcId()) {
			case 731193: //Walk Of Honor
			case 731194: { //Walk Of Glory
				super.handleDialogStart(player);
				break;
			} default: {
				if (player.getLevel() >= 65) {
				    PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getOwner().getObjectId(), 10));
				} else {
				    PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 27));
					// 仅一星及以上军官可进入要塞。 / Only officers 1-star or higher can enter the fortress.
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_Telepoter_GAb1_User07);
				}
				break;
			}
		}
	}
	
	@Override
	public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		if (dialogId == 105) {
			switch (getNpcId()) {
			    case 731193: //Walk Of Honor
				case 731194: //Walk Of Glory
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
