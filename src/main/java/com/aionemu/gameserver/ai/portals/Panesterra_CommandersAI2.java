package com.aionemu.gameserver.ai.portals;

import com.aionemu.gameserver.ai.GeneralNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.autogroup.AutoGroupType;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_FIND_GROUP;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
/** Helper themoose (Encom)

/**
 * 传送门/传送点 AI：Panesterra Commanders（@AIName "panesterra_commanders"），继承 GeneralNpcAI2。
 * Portal/teleporter AI: Panesterra Commanders (@AIName "panesterra_commanders"), extends GeneralNpcAI2.
 *
 * @author Encom
 */
@AIName("panesterra_commanders")
public class Panesterra_CommandersAI2 extends GeneralNpcAI2
{
	@Override
	protected void handleDialogStart(Player player) {
        switch (getNpcId()) {
			case 730940: //Advance Corridor For Commanders
			case 730941: { //Advance Corridor For Commanders
				super.handleDialogStart(player);
				break;
			} default: {
				if (player.getLevel() >= 65) {
				    PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getOwner().getObjectId(), 10));
				} else {
				    PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 27));
					// 仅总指挥官可进入。 / Only the commander-in-chief can enter.
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_Telepoter_GAb1_User05);
				}
				break;
			}
		}
	}
	
	@Override
	public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		if (dialogId == 105) {
			switch (getNpcId()) {
			    case 730940: //Advance Corridor For Commanders
			    case 730941: //Advance Corridor For Commanders
				    AutoGroupType agt = AutoGroupType.getAutoGroup(player.getLevel(), getNpcId());
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
