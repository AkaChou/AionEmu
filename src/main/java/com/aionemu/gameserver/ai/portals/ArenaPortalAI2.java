package com.aionemu.gameserver.ai.portals;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.autogroup.MatchDefinition;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_AUTO_GROUP;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 传送门/传送点 AI：Arena Portal（@AIName "arena_portal"），继承 PortalDialogAI2。
 * Portal/teleporter AI: Arena Portal (@AIName "arena_portal"), extends PortalDialogAI2.
 *
 * @author Encom
 */
@AIName("arena_portal")
public class ArenaPortalAI2 extends PortalDialogAI2
{
	@Override
	public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		if (questId != 0) {
			super.onDialogSelect(player, dialogId, questId, extendedRewardIndex);
			return true;
		}
		int worldId = 0;
		switch (dialogId) {
			case 10000:
				worldId = 300430000;
			break;
			case 10001:
				worldId = 300420000;
			break;
			case 10002:
				worldId = 300570000;
			break;
		}
		MatchDefinition agt = MatchDefinition.forWorld(player.getLevel(), worldId);
		if (agt != null) {
			PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(agt.getInstanceMaskId()));
		}
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
		return true;
	}
}
