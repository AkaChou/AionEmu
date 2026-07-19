package com.aionemu.gameserver.ai.portals;

import com.aionemu.gameserver.ai.ActionItemNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.autogroup.MatchDefinition;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_AUTO_GROUP;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;

@AIName("dredgion_teleporter")
public class Dredgion_TeleporterAI2 extends ActionItemNpcAI2 {
	@Override
	protected void handleUseItemFinish(Player player) {
		MatchDefinition definition = MatchDefinition.forNpc(player.getLevel(), getNpcId());
		if (definition == null || !definition.isOpen()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_Telepoter_Under_User);
			return;
		}
		PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(definition.getInstanceMaskId()));
	}
}
